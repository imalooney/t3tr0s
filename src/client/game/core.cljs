(ns client.game.core
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
    [client.dom :as dom]
    [client.util :as util]
    [cljs.reader :refer [read-string]]
    [client.game.history :as history]
    [client.game.board :refer [piece-fits?
                               rotate-piece
                               start-position
                               empty-board
                               get-drop-pos
                               get-rand-piece
                               get-rand-diff-piece
                               write-piece-to-board
                               write-piece-behind-board
                               create-drawable-board
                               get-filled-row-indices
                               clear-rows
                               game-over-row
                               collapse-rows
                               highlight-rows
                               write-to-board
                               n-rows
                               n-cols
                               rows-cutoff
                               next-piece-board
                               tower-height]]
    [client.game.rules :refer [get-points
                               level-up?
                               grav-speed
                               initial-shift-speed
                               shift-speed]]
    [client.game.paint :refer [size-canvas!
                               cell-size
                               draw-board!]]
    [client.game.vcr :refer [vcr toggle-record! record-frame!]]
    [client.socket :as socket]
    [cljs.core.async :refer [close! put! chan <! timeout unique alts!]]))

(enable-console-print!)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; Themes
;;------------------------------------------------------------------------------

(def themes
  {0  {:year "1984" :platform "Electronika 60"}
   10 {:year "1984" :platform "Electronika 60"}
   1  {:year "1986" :platform "MS DOS"}
   2  {:year "1986" :platform "Tengen/Atari Arcade"}
   3  {:year "1989" :platform "Gameboy"}
   13 {:year "1989" :platform "Gameboy"}
   4  {:year "1989" :platform "NES"}
   5  {:year "1989" :platform "Sega Genesis"}
   6  {:year "1998" :platform "Gameboy color"}
   7  {:year "2000" :platform "TI-83"}
   8  {:year "2002" :platform "Flash"}
   9  {:year "2012" :platform "Facebook"}
   19 {:year "2014" :platform "Mario"}})

;;------------------------------------------------------------
;; STATE OF THE GAME
;;------------------------------------------------------------

(def state
  "The state of the game."
  (atom nil))

(defn init-state!
  "Set the initial state of the game."
  []
  (reset! state {:next-piece nil
                 :piece nil
                 :position nil
                 :board empty-board

                 :theme (rand-nth (keys themes))

                 :score 0
                 :level 0
                 :level-lines 0
                 :total-lines 0

                 :soft-drop false

                 :quit false

                 :history []}))

; This channel is closed when we want to shutdown all go-blocks
; in order to exit the game.
; It must be initialized to a new channel on init.
(def quit-chan nil)

; required for pausing/resuming the gravity routine
(declare go-go-gravity!)

(def stop-grav (chan))
(defn stop-gravity! []
  (put! stop-grav 0))

(defn refresh-gravity! []
  (stop-gravity!)
  (go-go-gravity!))

(def game-over? (atom false))

(def battle
  "Boolean flag signaling whether we are in solo or battle mode."
  (atom false))

(def paused?
  "Boolean flag signaling if game is paused or not"
  (atom false))

(def paused-board
  "Temp state of the board when paused"
  (atom nil))

(def paused-music
  "Temp state of the music when paused"
  (atom nil))

(def music-playing?
  "Boolean flag signaling if the music is playing or not"
  (atom false))

;;------------------------------------------------------------------------------
;; Piece Control Throttling
;;------------------------------------------------------------------------------

; These channels can received boolean signals indicating on/off status.
; Duplicate signals are ignored with the (dedupe) transducer.
(def move-left-chan (chan 1 (dedupe)))
(def move-right-chan (chan 1 (dedupe)))
(def move-down-chan (chan 1 (dedupe)))

(defn go-go-control-soft-drop!
  "Monitor move-down-chan to update the gravity speed."
  []
  (go-loop []
    (let [[value c] (alts! [quit-chan move-down-chan])]
      (when (= c move-down-chan)
        (swap! state assoc :soft-drop value)
        (refresh-gravity!)
        (recur)))))

(declare try-move!)

(defn go-go-piece-shift!
  "Shifts a piece in the given direction until given channel is closed."
  [stop-chan dx]
  (go-loop [speed initial-shift-speed]
    (try-move! dx 0)
    (let [time- (timeout speed)
          [value c] (alts! [quit-chan stop-chan time-])]
      (when (= c time-)
        (recur shift-speed)))))

(defn go-go-control-piece-shift!
  "Monitors the given shift-chan to control piece-shifting."
  [shift-chan dx]
  (go-loop [stop-chan (chan)]
    (let [[value c] (alts! [quit-chan shift-chan])]
      (when (= c shift-chan)
        (recur (if value
                 (do (go-go-piece-shift! stop-chan dx)
                     stop-chan)
                 (do (close! stop-chan)
                     (chan))))))))

;;------------------------------------------------------------------------------
;; State Monitor
;;------------------------------------------------------------------------------

(defn- on-music-playing-change [_ _ _ new-state]
  (if-let [music-el (dom/by-id "music")]
    (if new-state
      (.play music-el)
      (.pause music-el))))

(add-watch music-playing? :music on-music-playing-change)

(defn- update-theme [_ _ old-state new-state]
  (if (not= (:theme old-state)
            (:theme new-state))
    (let [theme-num (:theme new-state)
          theme (get themes theme-num)]
      (.html ($ "#themeYear") (:year theme))
      (.html ($ "#themePlatform") (:platform theme)))))

(add-watch state :theme-change update-theme)

(defn make-redraw-chan
  "Create a channel that receives a value everytime a redraw is requested."
  []
  (let [redraw-chan (chan)
        request-anim #(.requestAnimationFrame js/window %)]
    (letfn [(trigger-redraw []
              (when-not (:quit @state)
                (put! redraw-chan 1)
                (request-anim trigger-redraw)))]
      (request-anim trigger-redraw)
      redraw-chan)))

(defn drawable-board
  "Draw the current state of the board."
  []
  (let [{piece :piece
         [x y] :position
         board :board} @state]
    (create-drawable-board piece x y board)))

(defn go-go-draw!
  "Kicks off the drawing routine."
  []
  (let [redraw-chan (make-redraw-chan)]
    (go-loop [board nil theme nil]
      (let [[_ c] (alts! [quit-chan redraw-chan])]
        (if (= c redraw-chan)
          (let [new-board (drawable-board)
                new-theme (:theme @state)
                next-piece (:next-piece @state)]
            (when (or (not= board new-board)
                      (not= theme new-theme))

              (history/draw-history! (:history @state))
              (draw-board! "mainGameCanvas" new-board cell-size new-theme rows-cutoff)
              (draw-board! "nextPieceCanvas" (next-piece-board next-piece) cell-size new-theme)
              (if (:recording @vcr)
                (record-frame!)))
            (recur new-board new-theme)))))))

(defn go-go-emit-updates!
  []
  (go-loop [prev-data nil]

    ; emit update to the server if relevant attributes have changed
    (let [data (select-keys @state [:piece :position :board :theme])]
      (when (not= prev-data data)
        (socket/emit "update-player" {:theme (:theme @state) :board (drawable-board)}))

      ; quit or schedule another update
      (let [quit quit-chan
            [_ ch] (alts! [quit (timeout 16)])]
        (when-not (= ch quit)
          (recur data))))))

;;------------------------------------------------------------
;; Game-driven STATE CHANGES
;;------------------------------------------------------------

(defn hide-game-over-modal! []
  (when (dom/by-id "overBoardModal")
    (dom/hide-el! "overBoardModal")
    (dom/hide-el! "gameOverMsg")))

(defn show-game-over-modal! []
  (when (dom/by-id "overBoardModal")
    (dom/show-el! "overBoardModal")
    (dom/show-el! "gameOverMsg")))

(defn hide-paused-modal! []
  (when (dom/by-id "overBoardModal")
    (dom/hide-el! "overBoardModal")
    (dom/hide-el! "pausedMsg")))

(defn show-paused-modal! []
  (when (dom/by-id "overBoardModal")
    (dom/show-el! "overBoardModal")
    (dom/show-el! "pausedMsg")))

(defn show-scramble-board-instant! [next-fn]
  (go
    (doseq [y (reverse (range n-rows))]
      (swap! state assoc-in [:board y] (game-over-row)))
    (next-fn)))

(defn show-scramble-board! [next-fn]
  (go
    (doseq [y (reverse (range n-rows))]
      (<! (timeout 10))
      (swap! state assoc-in [:board y] (game-over-row)))
    (when (and next-fn (goog/isFunction next-fn))
      (<! (timeout 200))
      (next-fn))))

(defn go-go-game-over!
  "Kicks off game over routine. (and get to the chopper)"
  []
  (reset! game-over? true)
  (show-scramble-board! show-game-over-modal!))

(defn spawn-piece!
  "Spawns the given piece at the starting position."
  [piece]
  (swap! state assoc :piece piece
                     :position start-position)

  (swap! state update-in [:history]
               conj {:height (-> @state :board tower-height)
                     :drop-y (second start-position)
                     :collapsed #{}})

  (when @battle
    (socket/emit "update-player" {:new-piece (:name piece)}))

  (go-go-gravity!))

(defn try-spawn-piece!
  "Checks if new piece can be written to starting position."
  []
  (let [piece (or (:next-piece @state) (get-rand-piece))
        next-piece (get-rand-diff-piece piece)
        [x y] start-position
        board (:board @state)]

    (swap! state assoc :next-piece next-piece)

    (if (piece-fits? piece x y board)
      (spawn-piece! piece)
      (go ;exitable
        ; Show piece that we attempted to spawn, drawn behind the other pieces.
        ; Then pause before kicking off gameover animation.
        (swap! state update-in [:board] #(write-piece-behind-board piece x y %))
        (<! (timeout (grav-speed (:level @state))))
        (go-go-game-over!)))))

(defn display-points!
  []
  (.html ($ "#gameScreenScore") (util/format-number (:score @state)))
  (.html ($ "#gameScreenLines") (:total-lines @state))
  (.html ($ "#gameScreenLevel") (inc (:level @state))))

(defn try-publish-score!
  "Inform the server of our current state."
  []
  (if @battle
    (socket/emit "update-player"
      (select-keys @state [:total-lines :score]))))

(defn update-points!
  [rows-cleared]
  (let [n rows-cleared
        level (:level @state)
        points (get-points n (inc level))
        level-lines (+ n (:level-lines @state))]

    ; update the score before a possible level-up
    (swap! state update-in [:score] + points)

    (if (level-up? level-lines)
      (do
        (swap! state update-in [:level] inc)
        (swap! state assoc :level-lines 0))
      (swap! state assoc :level-lines level-lines))

    (swap! state update-in [:total-lines] + n)

    (try-publish-score!))

  (display-points!))

(defn collapse-rows!
  "Collapse the given row indices."
  [rows]
  (let [n (count rows)
        board (collapse-rows rows (:board @state))]
    (swap! state assoc :board board)
    (update-points! n)))

(defn go-go-collapse!
  "Starts the collapse animation if we need to, returning nil or the animation channel."
  []
  (let [board (:board @state)
        rows (get-filled-row-indices board)
        flashed-board (highlight-rows rows board)
        cleared-board (clear-rows rows board)]

    (when-not (zero? (count rows))
      (go ; no need to exit this (just let it finish)
        ; blink n times
        (doseq [i (range 3)]
          (swap! state assoc :board flashed-board)
          (<! (timeout 170))
          (swap! state assoc :board board)
          (<! (timeout 170)))

        ; clear rows to create a gap, and pause
        (swap! state assoc :board cleared-board)
        (<! (timeout 220))

        ; finally collapse
        (collapse-rows! rows)))))

(defn lock-piece!
  "Lock the current piece into the board."
  []
  (let [[x y] (:position @state)
        piece (:piece @state)
        board (:board @state)
        new-board (write-piece-to-board piece x y board)]
    (swap! state assoc :board new-board
                       :piece nil
                       :soft-drop false) ; reset soft drop
    (swap! state update-in [:history (-> @state :history count dec)]
                 assoc :height (tower-height new-board)
                       :collapsed (get-filled-row-indices new-board))
    (stop-gravity!)

    ; If collapse routine returns a channel...
    ; then wait for it before spawning a new piece.
    (if-let [collapse-anim (go-go-collapse!)]
      (go
        (<! collapse-anim)
        (<! (timeout 100))
        (try-spawn-piece!))
      (try-spawn-piece!))))

(defn apply-gravity!
  "Move current piece down 1 if possible, else lock the piece."
  []
  (let [piece (:piece @state)
        [x y] (:position @state)
        board (:board @state)
        ny (inc y)]
    (if (piece-fits? piece x ny board)
      (do
        (swap! state assoc-in [:position 1] ny)
        (swap! state assoc-in [:history (-> @state :history count dec) :drop-y] ny))
      (lock-piece!))))

(defn go-go-gravity!
  "Starts the gravity routine."
  []
  (go-loop []
    (let [speed (grav-speed (:level @state) (:soft-drop @state))
          time- (timeout speed)
          [_ c] (alts! [time- stop-grav quit-chan])]
      (when (= c time-)
        (apply-gravity!)
        (recur)))))

;;------------------------------------------------------------
;; Input-driven STATE CHANGES
;;------------------------------------------------------------

(defn try-move!
  "Try moving the current piece to the given offset."
  [dx dy]
  (when-let [piece (:piece @state)]
    (let [[x y] (:position @state)
          board (:board @state)
          nx (+ dx x)
          ny (+ dy y)]
      (if (piece-fits? piece nx ny board)
        (swap! state assoc :position [nx ny])))))

(defn try-rotate!
  "Try rotating the current piece."
  []
  (when-let [piece (:piece @state)]
    (let [[x y] (:position @state)
          board (:board @state)
          new-piece (rotate-piece piece)]
      (if (piece-fits? new-piece x y board)
        (swap! state assoc :piece new-piece)))))

(defn hard-drop!
  "Hard drop the current piece."
  []
  (when-let [piece (:piece @state)]
    (let [[x y] (:position @state)
          board (:board @state)
          ny (get-drop-pos piece x y board)]
      (swap! state assoc :position [x ny])
      (lock-piece!))))

(defn change-theme!
  "Changes the boards theme"
  [theme event]
  (.preventDefault event)
  (swap! state assoc :theme theme)
  ;; TODO: set this in the atom watcher?
  (aset js/localStorage "theme" theme))

(defn load-theme!
  "Loads a theme if theres one saved in localStorage"
  []
  (if-let [theme (aget js/localStorage "theme")]
    (swap! state assoc :theme (int theme))
    "")) ;; TODO: why is this empty string returning here?

(def key-names
  {37 :left
   38 :up
   39 :right
   40 :down
   32 :space
   16 :shift
   80 :p
   77 :m

   49 :one
   50 :two
   51 :three
   52 :four
   53 :five
   54 :six
   55 :seven
   56 :eight
   57 :nine
   48 :zero})

(defn resume-game!
  "Restores the state of the board pre-pausing, and resumes gravity"
  []
  (reset! state @paused-board)

  ;; NOTE: this is just a quick hack to make the modal disappear after the
  ;;   scramble board is gone
  ;; it was jarring visually otherwise
  (js/setTimeout hide-paused-modal! 20)

  (go-go-gravity!)
  (reset! paused? false)
  (reset! music-playing? @paused-music))

(defn pause-game!
  "Saves the current state of the board, loads the game-over animation and pauses gravity"
  []
  (reset! paused-board @state)
  (reset! paused-music @music-playing?)
  (show-scramble-board-instant! show-paused-modal!)
  (swap! state assoc :piece nil)
  (stop-gravity!)
  (reset! paused? true)
  (reset! music-playing? false))

(defn toggle-pause-game!
  "Toggles pause on the game board"
  []
  (if (and (not @battle) (not @game-over?))
    (if @paused?
      (resume-game!)
      (pause-game!))
    (util/js-log "Cant pause in battle mode or game over.")))

(defn- toggle-music!
  "Toggles the music on or off"
  []
  (if-not @paused? (swap! music-playing? not)))

(defn add-key-events
  "Add all the key inputs."
  []
  (let [down-chan (chan)
        key-name #(-> % .-keyCode key-names)
        key-down (fn [e]
                   (case (key-name e)
                     ;; TODO: remove this - replace with themes value
                     :one   (if (.-shiftKey e)
                              (change-theme! 10 e)
                              (change-theme! 0 e))
                     :two   (change-theme! 1 e)
                     :three (change-theme! 2 e)
                     :four  (if (.-shiftKey e)
                              (change-theme! 13 e)
                              (change-theme! 3 e))
                     :five  (change-theme! 4 e)
                     :six   (change-theme! 5 e)
                     :seven (change-theme! 6 e)
                     :eight (change-theme! 7 e)
                     :nine  (change-theme! 8 e)
                     :zero  (if (.-shiftKey e)
                              (change-theme! 19 e)
                              (change-theme! 9 e))
                     :p     (do (toggle-pause-game!) (.preventDefault e))
                     :m     (do (toggle-music!) (.preventDefault e))
                     nil)
                   (when (not @paused?)
                     (case (key-name e)
                       :down  (put! move-down-chan true)
                       :left  (put! move-left-chan true)
                       :right (put! move-right-chan true)
                       :space (hard-drop!)
                       :up    (try-rotate!)
                       nil))
                   (when (#{:down :left :right :space :up} (key-name e))
                     (.preventDefault e)))
        key-up (fn [e]
                 (when-not (:quit @state)
                   (case (key-name e)
                     :down  (put! move-down-chan false)
                     :left  (put! move-left-chan false)
                     :right (put! move-right-chan false)
                     ;:shift (toggle-record!)
                     nil)))]

    ; Add key events
    (.addEventListener js/window "keydown" key-down)
    (.addEventListener js/window "keyup" key-up)

    ; Remove key events when quitting
    (go
      (<! quit-chan)
      (.removeEventListener js/window "keydown" key-down)
      (.removeEventListener js/window "keyup" key-up))))

;;------------------------------------------------------------
;; Entry Point
;;------------------------------------------------------------

(defn on-set-state
  "Called when server is setting the state of the client for a screenshot."
  [data-str]
  (let [new-state (read-string data-str)]

    ; Freeze the game.
    (if quit-chan
      (close! quit-chan))

    ; Merge state with new state data.
    (swap! state merge new-state)

    (display-points!)

    ; Draw new state.
    (let [board (drawable-board)
          theme (:theme @state)
          next-piece (:next-piece @state)]
      (draw-board! "mainGameCanvas" board cell-size theme rows-cutoff)
      (draw-board! "nextPieceCanvas" (next-piece-board next-piece) cell-size theme))))

(defn init []

  (reset! game-over? false)
  (reset! paused? false)

  (hide-game-over-modal!)

  ; Create new quit channel that all go-blocks will monitor for closing.
  (set! quit-chan (chan))

  (init-state!)
  (load-theme!)

  (history/init-canvas! "historyCanvas")

  (size-canvas! "mainGameCanvas" empty-board cell-size rows-cutoff)
  (size-canvas! "nextPieceCanvas" (next-piece-board) cell-size)

  (go-go-control-soft-drop!)
  (go-go-control-piece-shift! move-left-chan -1)
  (go-go-control-piece-shift! move-right-chan 1)

  (try-spawn-piece!)
  (add-key-events)
  (go-go-draw!)

  (display-points!)
  (try-publish-score!)

  (when @battle
    (go-go-emit-updates!))

  (socket/on "set-state" on-set-state))

(defn cleanup []
  (swap! state assoc :quit true)
  (reset! music-playing? false)
  (if quit-chan
    (close! quit-chan))

  (socket/removeListener "set-state"))
