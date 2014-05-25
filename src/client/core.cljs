(ns client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [client.board :refer [piece-fits?
                          cell-filled?
                          rotate-piece
                          start-position
                          empty-board
                          empty-row
                          get-drop-pos
                          get-rand-piece
                          write-piece-to-board
                          create-drawable-board
                          get-filled-row-indices
                          ]]
    [client.paint :refer [size-canvas!
                          draw-board!]]
    [client.repl :as repl]
    [client.socket :refer [socket connect-socket!]]
    [client.vcr :refer [vcr toggle-record! record-frame!]]
    [cljs.core.async :refer [put! chan <! timeout]]))

(enable-console-print!)

;;------------------------------------------------------------
;; STATE OF THE GAME
;;------------------------------------------------------------

(def state (atom {:piece (get-rand-piece)
                  :position start-position
                  :board empty-board

                  :flashing-rows #{}}))

;;------------------------------------------------------------
;; STATE MONITOR
;;------------------------------------------------------------

(defn draw-state
  "Draw the current state of the board."
  []
  (let [{piece :piece
         [x y] :position
         board :board
         flashing-rows :flashing-rows} @state]
    (draw-board! (create-drawable-board piece x y board flashing-rows)))

  (if (:recording @vcr)
    (record-frame!)))

(add-watch state :draw draw-state)

;;------------------------------------------------------------
;; Game-driven STATE CHANGES
;;------------------------------------------------------------

(defn spawn-piece!
  "Spawns a random piece at the starting position."
  []
  (swap! state assoc :piece (get-rand-piece)
                     :position start-position))

(defn collapse-rows!
  "Collapse all filled rows."
  []
  (let [board (:board @state)
        cleared-board (remove #(every? cell-filled? %) board)
        n (- (count board) (count cleared-board))
        new-board (into (vec (repeat n empty-row)) cleared-board)]
    (swap! state assoc :board new-board)))

(defn go-go-collapse!
  "Starts the collapse animation if we need to, returning nil or the animation channel."
  []
  (let [board (:board @state)
        rows (get-filled-row-indices board)]

    (when (> (count rows) 0)
      (go
        ; blink n times
        (doseq [i (range 3)]

          (<! (timeout 100))                      ; resume here later
          (swap! state assoc :flashing-rows rows) ; flash rows

          (<! (timeout 100))                      ; resume here later
          (swap! state update-in
                 [:flashing-rows] empty))         ; unflash rows

        ; finally collapse
        (collapse-rows!)))))

(defn lock-piece!
  "Lock the current piece into the board."
  []
  (let [[x y] (:position @state)
        piece (:piece @state)
        board (:board @state)]
    (swap! state assoc  :board (write-piece-to-board piece x y board)
                        :piece nil)

    ; If collapse routine returns a channel...
    ; then wait for it before spawning a new piece.
    (if-let [collapse-anim (go-go-collapse!)]
      (go (<! collapse-anim) (spawn-piece!))
      (spawn-piece!))))

(defn go-go-gravity!
  "Starts the gravity routine."
  []
  (go
    (loop []
      (<! (timeout 1000))
      (when (:piece @state)
        (let [[x y] (:position @state)
              piece (:piece @state)
              board (:board @state)
              ny (inc y)]
          (if (piece-fits? piece x ny board)
            (swap! state assoc-in [:position 1] ny)
            (lock-piece!))))
      (recur))))

;;------------------------------------------------------------
;; Input-driven STATE CHANGES
;;------------------------------------------------------------

(def key-codes {:left 37
                :up 38
                :right 39
                :down 40
                :space 32
                :shift 16})

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
  (.addEventListener js/window "keydown"
     (fn [e]
       (let [code (aget e "keyCode")]
         (cond
           (= code (:down key-codes))  (do (try-move!  0  1) (.preventDefault e))
           (= code (:left key-codes))  (do (try-move! -1  0) (.preventDefault e))
           (= code (:right key-codes)) (do (try-move!  1  0) (.preventDefault e))
           (= code (:space key-codes)) (do (hard-drop!)      (.preventDefault e))
           (= code (:up key-codes))    (do (try-rotate!)     (.preventDefault e))))))
  (.addEventListener js/window "keyup"
     (fn [e]
       (let [code (aget e "keyCode")]
         (cond
           (= code (:shift key-codes)) (toggle-record!))))))

;;------------------------------------------------------------
;; Facilities
;;------------------------------------------------------------

(defn auto-refresh
  "Automatically refresh the page whenever a cljs file is compiled."
  []
  (.on @socket "refresh" #(.reload js/location)))

;;------------------------------------------------------------
;; Entry Point
;;------------------------------------------------------------

(defn init []
  (size-canvas!)
  (spawn-piece!)
  (add-key-events)
  (go-go-gravity!)

  (repl/connect)
  (connect-socket!)
  (auto-refresh)
  )

(.addEventListener js/window "load" init)
