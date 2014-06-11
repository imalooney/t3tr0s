(ns server.core
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<! timeout alts! close! chan sliding-buffer filter< put!]]
    [clojure.walk]
    [cljs.reader :refer [read-string]]
    [server.gif :refer [create-gif]]
    [server.util :as util]))

(enable-console-print!)

;;------------------------------------------------------------
;; Node libraries
;;------------------------------------------------------------

(def express (js/require "express"))
(def http    (js/require "http"))
(def socket  (js/require "socket.io"))

;;------------------------------------------------------------
;; Config
;;------------------------------------------------------------

(def config
  (-> (js/require "./config.json")
    js->clj
    clojure.walk/keywordize-keys))

;;------------------------------------------------------------
;; Player IDs
;;------------------------------------------------------------

(def players
  "Table of connected players."
  (atom {}))

(def player-count
  "Total number of players who have connected since server started."
  (atom 0))

(defn gen-player-id!
  "Create a new unique player ID."
  []
  (swap! player-count inc)
  @player-count)

(def anon-player {:user "Anon" :color 0})

;;------------------------------------------------------------
;; Game Runner
;;------------------------------------------------------------

(declare go-go-next-game-countdown!)

(def game-count
  "Number of games started since server started (used to identify games)"
  (atom 0))

(def game-mode
  "The current game mode is nil, :lines, or :time"
  (atom nil))

(def quit-game-chan
  "Channel to close in order to halt the current game."
  (atom nil))

(def start-game-chan
  "Channel to start in order to start a new game."
  (atom nil))

(def players-waiting-chan
  "Channel to signal the number of players waiting to play in lobby."
  (atom (chan (sliding-buffer 1))))

(def game-settings
  "Game timer settings."
  (atom {:duration 300 ; Length in seconds of a multiplayer round. Default is 5 minutes.
         :cooldown 0})) ; Length in seconds before automatically starting a new game. Default (0) requires manual start.

(def leaders
  "The leaders in the current game."
  (atom []))

(defn rank-players
  "Get players for the given game sorted by rank for the given mode."
  [game-id mode]
  (->> (vals @players)
       (filter #(= game-id (:game %)))
       (sort-by :score)
       (reverse)))

(defn player-visible-on-dashboard?
  "Determines if the given player is visible on the dashboard."
  [pid]
  (some #(= pid (:pid %)) (take 3 @leaders)))

(defn go-go-countdown!
  "Start the countdown"
  [io seconds]
  (go
    (doseq [i (reverse (range (inc seconds)))]
      (<! (timeout 1000))
      (util/js-log "countdown:" i)
      (.. io (to "lobby") (emit "start-game"))
      (.. io (to "game") (emit "countdown" i))
      (.. io (to "mc") (emit "countdown" i)))
  ))

(defn go-go-game!
  "Start a game."
  [io mode]

  ; Empty the leaders.
  (swap! leaders empty)

  ; Create new quit channel for this game.
  (reset! quit-game-chan (chan))

  ; Set the game mode.
  (reset! game-mode mode)

  ; Update the game count to uniquely identify current game.
  (swap! game-count inc)

  ; Log the game start.
  (util/js-log "Starting the game:" (name mode) @game-count)

  ; Make lobby players navigate to a countdown screen.
  (.. io (to "lobby") (emit "start-game"))

  (go

    ; Kick off and wait for the countdown for game start.
    (<! (go-go-countdown! io 5))

    ; Emit a message for every second left until game over.
    (if (= mode :time)
      (loop [s (:duration @game-settings)]

        (util/js-log "time left:" s)

        (.. io (to "game") (emit "time-left" s))
        (.. io (to "mc") (emit "time-left" s))
        (.. io (to "dashboard") (emit "time-left" s))
        (.. io (to "lobby") (emit "start-game"))

        (if-not (zero? s)

          ; Wait for either the timer or the quit channel.
          (let [t (timeout 1000)
                q @quit-game-chan
                [_ c] (alts! [t q])]
            (if (= c t)
              (recur (dec s)))))))

    ; Calculate final ranks.
    (let [ranks (rank-players @game-count @game-mode)]

      ; Show ranks on server console.
      (util/js-log "RANKS")
      (doseq [r ranks]
        (util/js-log (:user r) " - "
                        (:score r) " points - "
                        (:total-lines r) " lines"))

      ; Emit a game over message with ranks to players.
      (.. io (to "game") (emit "game-over"
                               (pr-str ranks)))

      ; Emit a game over message with final ranks to MC.
      (.. io (to "mc") (emit "game-over"
                             (pr-str ranks))))

    ; Clear the game mode when done.
    (reset! game-mode nil)

    (util/js-log "Game ended.")

    (go-go-next-game-countdown! io)
    )
  )

(defn go-go-next-game-countdown!
  "Start a new game automatically after cooldown period"
  [io]

  ; Create new start channel for this game.
  (reset! start-game-chan (chan))

  (go
    ; If a cooldown time is set, then wait for it to expire
    (let [cooldown (:cooldown @game-settings)]
      (if (pos? cooldown)
        (do
          (util/js-log "Waiting for players")

          ; don't start the countdown until there are at least 2 players connected
          (<! (filter< #(>= % 2) @players-waiting-chan))

          ; Countdown
          (loop [s cooldown]
            (util/js-log "time until next game:" s)

            (.. io (to "lobby") (emit "time-left" s))

            (if-not (zero? s)
              ; Wait for either the timer or the start channel.
              (let [t (timeout 1000)
                    q @start-game-chan
                    [_ c] (alts! [t q])]
                (if (= c t)
                  (recur (dec s))))
              ; timer expired let the game automatically start
              (close! @start-game-chan))))

        ; no cooldown set, then just wait for mc to manually start the game
        (util/js-log "Waiting for manual start of next game")))

      ; wait on the start game channel
      ; note: if the cooldown timer was interrupted then this should
      ;       return immediately
      (<! @start-game-chan)

      ; It's game time, GO!
      (go-go-game! io :time)))

(defn- signal-num-players-in-lobby!
  "Publishes the number of players waiting in the lobby in case next game countdown is on hold."
  []
  (let [num-players (count (filter :in-lobby (vals @players)))]
    (util/log (str "Num players in the lobby: " num-players))
    (put! @players-waiting-chan num-players)))


;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

(defn- on-chat-message [msg pid socket]
  (let [d (assoc (get @players pid) :type "msg" :msg msg)]
    (util/js-log "Player" pid "said:" msg)
    (.. socket -broadcast (to "lobby") (emit "new-message" (pr-str d)))))

(defn- on-score-update
  "Called when a player's score is updated."
  [io]
  (let [ranks (take 10 (rank-players @game-count @game-mode))]
    (reset! leaders ranks)
    (.. io (to "dashboard") (emit "leader-update" (pr-str ranks)))))

(defn- on-update-player
  "Called when player sends an updated state."
  [data pid socket io]

  (util/js-log "Player" pid "updated.")

  ; Merge in the updated data into the player structure.
  ; Also update the game id, so we know which players are in the current game.
  (swap! players update-in [pid] merge data {:game @game-count :pid pid})

  ; Call score update events.
  (if (contains? data :score)
    (on-score-update io))

  ; If player should be visible on dashboard, then emit to dashboard.
  (if (and (contains? data :board)
           (player-visible-on-dashboard? pid))
    (.. io (to "dashboard")
           (emit "board-update"
                 (pr-str (select-keys (get @players pid) [:board :pid :theme]))))))


(defn- on-reset-times
  "Called when the MC updates the game time settings."
  [new-times socket]
  ; exclude any invalid time entries that aren't positive integers
  (let [{:keys [duration cooldown]} new-times
        new-times (if (pos? duration) (assoc new-times :duration duration))
        new-times (if (>= cooldown 0) (assoc new-times :cooldown cooldown))]
    (when-not (empty? new-times)
      (swap! game-settings merge new-times)
      (.. socket -broadcast (to "mc") (emit "settings-update" (pr-str new-times)))
      (.emit socket "settings-update" (pr-str new-times)) ; TODO - How do you emit to self AND room?
      (util/js-log (str "Updated game times: " new-times)))))

;;------------------------------------------------------------------------------
;; Socket Setup
;;------------------------------------------------------------------------------

(defn init-socket
  "Initialize the web socket."
  [io socket]

  ; Create gif whenever "create-gif" is emitted.
  (.on socket "create-gif" #(create-gif (read-string %)))


  (let [pid (gen-player-id!)]

    (util/js-log "Player" pid "connected.")

    ; Attach player id to socket.
    (aset socket "pid" pid)

    ; Add to player table as "Anon" for now.
    (swap! players assoc pid anon-player)

    ; Request that the client emit an "update-name" message back
    ; in case the server restarts and we need user info again.
    (.emit socket "request-name")

    ; Remove player from table when disconnected.
    (.on socket "disconnect"
         #(do
            (util/js-log "Player" pid "disconnected.")
            (swap! players dissoc pid)
            (signal-num-players-in-lobby!)))

    ; Update player name when requested.
    (.on socket "update-name"
         #(let [data (read-string %)]
            (util/js-log "Updating player" pid "with" (:user data) (:color data))
            (swap! players update-in [pid] merge data)))

    ; Join the lobby.
    (.on socket "join-lobby"
         #(let [data (assoc (get @players pid) :type "join")]
            (util/js-log "Player" pid "joined the lobby.")
            (.. socket -broadcast (to "lobby") (emit "new-message" (pr-str data)))
            (.join socket "lobby")
            ; socket.io provides no way get a count of players in a room
            ; so we need to mark players as in the lobby in order to know
            ; who is waiting for the next game
            (swap! players update-in [pid] assoc :in-lobby true)
            (signal-num-players-in-lobby!)))

    ; Leave the lobby.
    (.on socket "leave-lobby"
         #(let [data (assoc (get @players pid) :type "leave")]
            (util/js-log "Player" pid "left the lobby.")
            (.. socket -broadcast (to "lobby") (emit "new-message" (pr-str data)))
            (.leave socket "lobby")
            (swap! players update-in [pid] dissoc :in-lobby)
            (signal-num-players-in-lobby!)))

    ; Chat in the lobby.
    (.on socket "chat-message" #(on-chat-message % pid socket))

    ; Join/leave the game.
    (.on socket "join-game" #(.join socket "game"))
    (.on socket "leave-game" #(.leave socket "game"))

    ; Join/leave the dashboard.
    (.on socket "join-dashboard"
         #(do
            (.join socket "dashboard")
            (on-score-update io)))
    (.on socket "leave-dashboard" #(.leave socket "dashboard"))

    ; Receive the update from the player.
    (.on socket "update-player"
         #(on-update-player (read-string %) pid socket io))

    ; Request access to the MC role.
    (.on socket "request-mc"
         #(if (= % (:mc-password config))
            (do
              (util/js-log "Player" pid "granted as MC.")
              (.join socket "mc")
              (.emit socket "grant-mc" (pr-str @game-mode))
              (.emit socket "settings-update" (pr-str @game-settings)))
            (do
              (util/js-log "Player" pid "rejected as MC."))))

    ; Leave the MC role.
    (.on socket "leave-mc"
         #(.leave socket "mc"))

    ; Start the game
    #_(.on socket "start-lines" #(close! if-not @game-mode (go-go-game! io :line)))
    (.on socket "start-time" #(close! @start-game-chan))

    ; Stop the game.
    (.on socket "stop-game"
         #(if (and @game-mode @quit-game-chan)
            (close! @quit-game-chan)))

    ; Reset game times
    (.on socket "reset-times" #(on-reset-times (read-string %) socket))

    ))

;;------------------------------------------------------------
;; Main
;;------------------------------------------------------------

(defn -main [& args]

  (let [app    (express)
        server (.createServer http app)
        io     (.listen socket server)]

    ; configure express app
    (doto app
      (.get "/" (fn [req res] (.send res (html/page-shell))))
      (.use (.static express (str js/__dirname "/public"))))

    ; start server
    (.listen server (:port config))
    (println "t3tr0s server listening on port" (:port config) "\n")

    ; wait for next game to start
    (go-go-next-game-countdown! io)

    ; configure sockets
    (.sockets.on io "connection" #(init-socket io %))))

(set! *main-cli-fn* -main)
