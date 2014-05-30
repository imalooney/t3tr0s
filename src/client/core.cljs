(ns client.core
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [cljs.reader :refer [read-string]]
    [client.board :refer [piece-fits?
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
                          collapse-rows
                          highlight-rows
                          write-to-board
                          n-rows
                          n-cols
                          rows-cutoff
                          next-piece-board]]
    [client.rules :refer [get-points
                          level-up?
                          get-level-speed]]
    [client.paint :refer [delete-opponent-canvas!
                          create-opponent-canvas!
                          size-canvas!
                          cell-size
                          draw-board!]]
    [client.multiplayer :refer [opponent-scale]]
    [client.repl :as repl]
    [client.socket :refer [socket connect-socket!]]
    [client.vcr :refer [vcr toggle-record! record-frame!]]
    [cljs.core.async :refer [put! chan <! timeout unique]]))

(enable-console-print!)

; alias the jquery variable
(def $ js/$)

;;------------------------------------------------------------
;; STATE OF THE GAME
;;------------------------------------------------------------

(def state (atom {:next-piece nil
                  :piece nil
                  :position nil
                  :board empty-board

                  :score 0
                  :level 0
                  :level-lines 0
                  :total-lines 0

                  :soft-drop false}))

; required for pausing/resuming the gravity routine
(def pause-grav (chan))
(def resume-grav (chan))

;;------------------------------------------------------------
;; STATE MONITOR
;;------------------------------------------------------------

(defn drawable-board
  "Draw the current state of the board."
  []
  (let [{piece :piece
         [x y] :position
         board :board} @state]
    (create-drawable-board piece x y board)))

(defn make-redraw-chan
  "Create a channel that receives a value everytime a redraw is requested."
  []
  (let [redraw-chan (chan)
        request-anim #(.requestAnimationFrame js/window %)]
    (letfn [(trigger-redraw []
              (put! redraw-chan 1)
              (request-anim trigger-redraw))]
      (request-anim trigger-redraw)
      redraw-chan)))

(defn go-go-draw!
  "Kicks off the drawing routine."
  []
  (let [redraw-chan (make-redraw-chan)]
    (go
      (loop [board nil]
        (<! redraw-chan)
        (let [new-board (drawable-board)]
          (when (not= board new-board)
            (.emit @socket "board-update" (pr-str {:level (:level @state)
                                                   :board new-board}))
            (draw-board! "game-canvas" new-board cell-size (:level @state) rows-cutoff)
            (if (:recording @vcr)
              (record-frame!)))
          (recur new-board))))))

;;------------------------------------------------------------
;; Game-driven STATE CHANGES
;;------------------------------------------------------------

(defn go-go-game-over!
  "Kicks off game over routine. (and get to the chopper)"
  []
  (go
    (doseq [y (reverse (range n-rows))
            x (range n-cols)]
      (if (even? x)
        (<! (timeout 2)))
      (swap! state update-in [:board] #(write-to-board x y "H0" %)))))

(defn spawn-piece! 
  "Spawns the given piece at the starting position."
  [piece]
    (swap! state assoc :piece piece
                       :position start-position)
    (put! resume-grav 0))

(defn try-spawn-piece!
  "Checks if new piece can be written to starting position."
  []
  (let [piece (or (:next-piece @state) (get-rand-piece))
        next-piece (get-rand-diff-piece piece)
        [x y] start-position
        board (:board @state)]

    (swap! state assoc :next-piece next-piece)
    (draw-board! "next-canvas" (next-piece-board next-piece) cell-size (:level @state))

    (if (piece-fits? piece x y board)
      (spawn-piece! piece)
      (go
        ; Show piece that we attempted to spawn, drawn behind the other pieces.
        ; Then pause before kicking off gameover animation.
        (swap! state update-in [:board] #(write-piece-behind-board piece x y %))
        (<! (timeout (get-level-speed (:level @state))))
        (go-go-game-over!)))))

(defn display-points!
  []
  (.html ($ "#score") (str "Score: " (:score @state)))
  (.html ($ "#level") (str "Level: " (:level @state)))
  (.html ($ "#lines") (str "Lines: " (:total-lines @state))))

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

    (swap! state update-in [:total-lines] + n))

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
      (go
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
        board (:board @state)]
    (swap! state assoc :board (write-piece-to-board piece x y board)
                       :piece nil
                       :soft-drop false)
    (put! pause-grav 0)

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
      (swap! state assoc-in [:position 1] ny)
      (lock-piece!))))

(defn go-go-gravity!
  "Starts the gravity routine."
  []
  ; Make sure gravity starts in paused mode.
  ; Spawning the piece will signal the first "resume".
  (put! pause-grav 0)

  (go
    (loop []
      (let [soft-speed 25
            level-speed (get-level-speed (:level @state))
            speed (if (:soft-drop @state)
                    (min soft-speed level-speed)
                    level-speed)

            cs [(timeout speed) pause-grav] ; channels to listen to (timeout, pause)
            [_ c] (alts! cs)]               ; get the first channel to receive a value
        (if (= pause-grav c)                ; if "pause" received, wait for "resume"
          (<! resume-grav)
          (apply-gravity!))
        (recur)))))

;;------------------------------------------------------------
;; Input-driven STATE CHANGES
;;------------------------------------------------------------

(defn try-move!
  "Try moving the current piece to the given offset."
  [dx dy]
  (let [[x y] (:position @state)
        piece (:piece @state)
        board (:board @state)
        nx (+ dx x)
        ny (+ dy y)]
    (if (piece-fits? piece nx ny board)
      (swap! state assoc :position [nx ny]))))

(defn try-rotate!
  "Try rotating the current piece."
  []
  (let [[x y] (:position @state)
        piece (:piece @state)
        board (:board @state)
        new-piece (rotate-piece piece)]
    (if (piece-fits? new-piece x y board)
      (swap! state assoc :piece new-piece))))

(defn hard-drop!
  "Hard drop the current piece."
  []
  (let [[x y] (:position @state)
        piece (:piece @state)
        board (:board @state)
        ny (get-drop-pos piece x y board)]
    (swap! state assoc :position [x ny])
    (lock-piece!)))

(defn add-key-events
  "Add all the key inputs."
  []
  (let [down-chan (chan)
        key-names {37 :left
                   38 :up
                   39 :right
                   40 :down
                   32 :space
                   16 :shift}
        key-name #(-> % .-keyCode key-names)]

    (.addEventListener js/window "keydown"
      (fn [e]
        (if (:piece @state)
          (case (key-name e)
            :down  (do (put! down-chan true) (.preventDefault e))
            :left  (do (try-move! -1  0)     (.preventDefault e))
            :right (do (try-move!  1  0)     (.preventDefault e))
            :space (do (hard-drop!)          (.preventDefault e))
            :up    (do (try-rotate!)         (.preventDefault e))
            nil))))

    (.addEventListener js/window "keyup"
      (fn [e]
        (case (key-name e)
          :down  (put! down-chan false)
          :shift (toggle-record!)
          nil)))

    ; Listen to the down key, but ignore repeats.
    (let [c (unique down-chan)]
      (go
        (while true
          (let [on-or-off (<! c)]
            (swap! state assoc :soft-drop on-or-off)

            ; force gravity to reset
            (put! pause-grav 0)
            (put! resume-grav 0)))))))


;;------------------------------------------------------------
;; Facilities
;;------------------------------------------------------------

(defn auto-refresh
  "Automatically refresh the page whenever a cljs file is compiled."
  []
  (.on @socket "refresh" #(.reload js/location)))

;;------------------------------------------------------------
;; Opponent drawing
;;------------------------------------------------------------

(defn on-opponent-update
  [{:keys [id level board]}]

  (create-opponent-canvas! id)

  (draw-board! id board (opponent-scale cell-size) level)
  )

;;------------------------------------------------------------
;; Entry Point
;;------------------------------------------------------------

(defn init []

  (size-canvas! "game-canvas" empty-board cell-size rows-cutoff)
  (size-canvas! "next-canvas" (next-piece-board) cell-size)

  (try-spawn-piece!)
  (add-key-events)
  (go-go-draw!)
  (go-go-gravity!)

  (display-points!)

  (repl/connect)
  (connect-socket!)

  (.on @socket "board-update" #(on-opponent-update (read-string %)))
  (.on @socket "board-delete" delete-opponent-canvas!)

  (auto-refresh)
  )

(.addEventListener js/window "load" init)
