(ns server.core
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<! timeout alts! close! chan sliding-buffer
                             filter< put!]]
    [clojure.walk]
    [cljs.reader :refer [read-string]]
    [server.gif :refer [create-html-gif create-canvas-gif]]
    [server.html :as html]
    [server.util :as util]))

(enable-console-print!)

;;------------------------------------------------------------------------------
;; Node libraries
;;------------------------------------------------------------------------------

(def express     (js/require "express"))
(def compression (js/require "compression"))
(def http        (js/require "http"))
(def socketio    (js/require "socket.io"))

;;------------------------------------------------------------------------------
;; Config
;;------------------------------------------------------------------------------

(def config
  (-> (js/require "./config.json")
    js->clj
    clojure.walk/keywordize-keys))

;;------------------------------------------------------------------------------
;; Player IDs
;;------------------------------------------------------------------------------

(def players
  "Table of connected players."
  (atom {}))

(defn short-pid [pid]
  (subs pid 0 6))

(defn pprint-pid
  "Returns either the username or a short version of the pid."
  [pid]
  (if-let [username (:user (get @players pid))]
    username
    (short-pid pid)))

;;------------------------------------------------------------------------------
;; Game Runner
;;------------------------------------------------------------------------------

(declare go-go-next-game-countdown!)

(def current-round-id
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
         :cooldown 90})) ; Length in seconds before automatically starting a new game. Setting 0 requires manual start.

(def initial-piece-counts
  {:I 0
   :T 0
   :O 0
   :J 0
   :L 0
   :Z 0
   :S 0})

(def piece-counts
  (atom {}))

(defn rank-players
  "Get players for the given game sorted by rank."
  [game-id]
  (->> (vals @players)
       (filter #(= game-id (:game %)))
       (sort-by :score)
       reverse))

(defn go-go-countdown!
  "Start the countdown"
  [io seconds]
  (go
    (doseq [i (reverse (range (inc seconds)))]
      (<! (timeout 1000))
      (util/tlog "new game starting in " i)
      (.. io (to "lobby") (emit "start-game"))
      (.. io (to "game") (emit "countdown" i))
      (.. io (to "mc") (emit "countdown" i)))))

;; TODO: rename "game" to "round"?

(defn- log-time-left [s]
  (cond
    (= 0 s) nil
    (= 0 (mod s 15)) (util/tlog "game time remaining: " s " seconds")
    (< s 10) (util/tlog "game ending in " s)
    :else nil))

(defn go-go-game!
  "Start a game."
  [io mode]

  ; Reset the piece counts.
  (reset! piece-counts initial-piece-counts)

  ; Create new quit channel for this game.
  (reset! quit-game-chan (chan))

  ; Set the game mode.
  (reset! game-mode mode)

  ; Update the game count to uniquely identify current game.
  (swap! current-round-id inc)

  ; Log the game start.
  (util/tlog "Starting the game: " (name mode) " " @current-round-id)

  ; Make lobby players navigate to a countdown screen.
  (.. io (to "lobby") (emit "start-game"))

  (go

    ; Kick off and wait for the countdown for game start.
    (<! (go-go-countdown! io 5))

    ; Emit a message for every second left until game over.
    (if (= mode :time)
      (loop [s (:duration @game-settings)]

        (log-time-left s)

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
    (let [ranks (rank-players @current-round-id)]

      ; Show ranks on server console.
      (util/tlog "Game Results:")
      (doseq [r ranks]
        (util/tlog (:user r) " - "
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

    (util/tlog "game ended")

    (go-go-next-game-countdown! io)))

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
          (util/tlog "waiting for players")

          ; don't start the countdown until there are at least 2 players connected
          (<! (filter< #(>= % 2) @players-waiting-chan))

          ; Countdown
          (loop [s cooldown]
            (util/tlog "time until next game: " s)

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
        (util/tlog "waiting for manual start of next game")))

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
    (util/tlog "lobby count: " num-players)
    (put! @players-waiting-chan num-players)))

(defn- send-lobby-players-update!
  "Send the current lobby player information to all clients in lobby"
  [io]
  (.. io (to "lobby") (emit "players-update" (pr-str (filter :in-lobby (vals @players))))))

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

(defn- on-chat-message
  "Chat in the lobby"
  [msg pid socket]
  (let [d (assoc (get @players pid) :type "msg" :msg msg)]
    (util/tlog "player " (pprint-pid pid) " says: \"" msg "\"")
    (.. socket -broadcast (to "lobby") (emit "new-message" (pr-str d)))))

(defn- on-score-update
  "Called when a player's score is updated."
  [io]
  (let [ranks (rank-players @current-round-id)]
    (.. io (to "dashboard") (emit "leader-update" (pr-str ranks)))))

(defn- on-update-player
  "Called when player sends an updated state."
  [pid socket io data-str]
  (let [player-data (read-string data-str)]
    ;; new piece
    (when-let [new-piece (:new-piece player-data)]
      (swap! piece-counts update-in [new-piece] inc)
      (.. io (to "dashboard") (emit "piece-stats" (pr-str @piece-counts))))

    ; Merge in the updated data into the player structure.
    ; Also update the game id, so we know which players are in the current game.
    (swap! players update-in [pid] merge player-data
      {:game @current-round-id :pid pid})

    ; Call score update events.
    (if (contains? player-data :score)
      (on-score-update io))

    ; If player should be visible on dashboard, then emit to dashboard.
    (if (contains? player-data :board)
      (.. io (to "dashboard")
             (emit "board-update"
                   (pr-str (select-keys (get @players pid) [:board :pid :theme])))))

    ; If player in lobby then send updated player list
    (if (:in-lobby (get @players pid))
      (send-lobby-players-update! io))))

(defn- on-update-times
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
      (util/tlog "new game times: " new-times))))

(defn- on-join-lobby
  "Player joins the lobby"
  [pid socket io]
  (let [data (assoc (get @players pid) :type "join")]
    (util/tlog "player " (pprint-pid pid) " joined the lobby")
    (.. socket -broadcast (to "lobby") (emit "new-message" (pr-str data)))
    (.join socket "lobby")
    ; socket.io provides no way to get a count of players in a room
    ; so we need to mark players as in the lobby in order to know
    ; who is waiting for the next game
    (swap! players update-in [pid] assoc :in-lobby true)
    (send-lobby-players-update! io)
    (signal-num-players-in-lobby!)))

(defn- on-leave-lobby
  "Player leaves the lobby"
  [pid socket io]
  (let [data (assoc (get @players pid) :type "leave")]
    (util/tlog "player " (pprint-pid pid) " left the lobby")
    (.. socket -broadcast (to "lobby") (emit "new-message" (pr-str data)))
    (.leave socket "lobby")
    (swap! players update-in [pid] dissoc :in-lobby)
    (send-lobby-players-update! io)
    (signal-num-players-in-lobby!)))

(def socket-id (util/uuid))

(defn- emit-to-socket [event-name data]
  (.emit (aget js/global socket-id) event-name (pr-str data)))

(defn- on-update-name [pid data-str]
  (let [data (read-string data-str)]
    (swap! players update-in [pid] merge data)
    (util/tlog "player " (short-pid pid) " now known as \"" (:user data) "\"")))

(def chat (atom []))

(defn- on-change-chat [_ _ _ new-c]
  (emit-to-socket "chat-update2" new-c))

(add-watch chat :main on-change-chat)

(def max-num-chat 100)

;; save the chat message in the @chat atom
(defn- on-chat-msg [data-str]
  (let [data (read-string data-str)]
    (swap! chat (fn [c]
      (if (= (count c) max-num-chat)
        (into [] (rest (conj c data)))
        (conj c data))))))

(defn- on-game-update [data-str]
  (let [data (read-string data-str)]
    (util/log data)
    ))

(defn- on-join-dashboard [the-socket io]
  (.join the-socket "dashboard")
  (on-score-update io))

(defn- on-disconnect [pid]
  (util/tlog "player " (pprint-pid pid) " disconnected")
  (swap! players dissoc pid)
  (signal-num-players-in-lobby!))

(defn- on-request-mc [pid the-socket password-attempt]
  (if (= (read-string password-attempt) (:mc-password config))
    (do
      (util/tlog "player " (pprint-pid pid) " granted as MC")
      (.join the-socket "mc")
      (.emit the-socket "grant-mc" (pr-str @game-mode))
      (.emit the-socket "settings-update" (pr-str @game-settings)))
    (do
      (util/tlog "player " (pprint-pid pid) " rejected as MC"))))

(defn- on-stop-game
  "Called when we receive the 'stop-game' event from the client."
  []
  (if (and @game-mode @quit-game-chan)
    (close! @quit-game-chan)))

;;------------------------------------------------------------------------------
;; Socket Setup
;;------------------------------------------------------------------------------

(defn on-socket-connect
  "Initialize the web socket."
  [io socket]
  (let [pid (util/uuid)]

    (util/tlog "player " (pprint-pid pid) " connected")

    ;; add to player table
    (swap! players assoc pid {:color (rand-int 7) :pid pid})

    (doto socket
      ;; Create gif whenever "create-canvas-gif" is emitted.
      (.on "create-html-gif" #(create-html-gif (read-string %) socket))
      (.on "create-canvas-gif" #(create-canvas-gif (read-string %)))

      ;; TODO: this is not really finished yet
      (.on "chat-msg2" on-chat-msg)
      (.on "game-update2" on-game-update)

      ;; Request that the client emit an "update-name" message back
      ;; in case the server restarts and we need user info again.
      (.emit "request-name")

      ;; Remove player from table when disconnected.
      (.on "disconnect" #(on-disconnect pid))

      ;; Update player name when requested.
      (.on "update-name" #(on-update-name pid %))

      (.on "join-lobby" #(on-join-lobby pid socket io))
      (.on "leave-lobby" #(on-leave-lobby pid socket io))
      (.on "chat-message" #(on-chat-message (read-string %) pid socket))

      ;; Join/leave the game.
      (.on "join-game" #(.join socket "game"))
      (.on "leave-game" #(.leave socket "game"))

      ;; Join/leave the dashboard.
      (.on "join-dashboard" #(on-join-dashboard socket io))
      (.on "leave-dashboard" #(.leave socket "dashboard"))

      ;; Receive the update from the player.
      (.on "update-player" #(on-update-player pid socket io %))

      ;; Request access to the MC role.
      (.on "request-mc" #(on-request-mc pid socket %))

      ;; Leave the MC role.
      (.on "leave-mc" #(.leave socket "mc"))

      ;; Start the game
      (.on "start-time" #(close! @start-game-chan))

      ;; Stop the game.
      (.on "stop-game" on-stop-game)

      ;; Update game times
      (.on "update-times" #(on-update-times (read-string %) socket)))))

;;------------------------------------------------------------------------------
;; Main
;;------------------------------------------------------------------------------

(defn -main [& args]
  (let [app    (express)
        server (.createServer http app)
        io     (.listen socketio server)]

    (aset js/global socket-id io)

    ;; configure express app
    (doto app
      (.use (compression))
      (.get "/" (fn [req res] (.send res (html/page-shell))))
      (.use (.static express (str js/__dirname "/public"))))

    ;; start server
    (if (:host config)
      (.listen server (:port config) (:host config))
      (.listen server (:port config)))
    (util/tlog "t3tr0s server listening on port " (:port config))

    ;; wait for next game to start
    (go-go-next-game-countdown! io)

    ;; configure sockets
    (.sockets.on io "connection" #(on-socket-connect io %))))

(set! *main-cli-fn* -main)
