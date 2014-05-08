(ns client.core
  (:require
    [clojure.browser.repl :as repl]
    [clojure.string :refer [join]]
    [jayq.core :refer [$ ajax document-ready]]))

(enable-console-print!)

;;------------------------------------------------------------
;; Pieces.
;;------------------------------------------------------------

(def pieces
  {:I {:name :I :coords [[0 0] [0 -1] [0 1] [0 2]]}
   :L {:name :L :coords [[0 0] [0 1] [1 1] [0 -1]]}
   :J {:name :J :coords [[0 0] [0 -1] [0 1] [-1 1]]}
   :S {:name :S :coords [[0 0] [-1 0] [0 -1] [1 -1]]}
   :Z {:name :Z :coords [[0 0] [-1 -1] [0 -1] [1 0]]}
   :O {:name :O :coords [[0 0] [-1 0] [-1 1] [0 1]]}
   :T {:name :T :coords [[0 0] [-1 0] [1 0] [0 1]]}})

(defn get-rand-piece
  "Return a random piece."
  []
  (pieces (rand-nth (keys pieces))))

;;------------------------------------------------------------
;; Board.
;;------------------------------------------------------------

(def empty-board [[0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]
                  [0 0 0 0 0 0 0 0 0 0]])

; The starting position of all pieces.
(def start-position [4 2])

(defn coord-inside?
  "Determines if the coordinate is inside the board."
  [x y]
  (and (<= 0 x 9) (<= 0 y 21)))


; The size of a cell in pixels.
(def cell-size 20)

;;------------------------------------------------------------
;; Pure Functions operating on a board.
;;------------------------------------------------------------

(defn read-board
  "Get the current value from the given board position."
  [x y board]
  (get-in board [y x]))

(defn write-to-board
  "Returns a new board with a value written to the given position."
  [x y value board]
  (if (coord-inside? x y)
    (assoc-in board [y x] value)
    board))

(defn write-coord-to-board
  "Returns a new board with a value written to the given relative coordinate and position."
  [[cx cy] x y value board]
    (write-to-board (+ cx x) (+ cy y) value board))

(defn write-coords-to-board
  "Returns a new board with a value written to the given relative coordinates and position."
  [coords x y value board]
  (if (zero? (count coords))
    board
    (let [coord (first coords)
          rest-coords (rest coords)
          new-board (write-coord-to-board coord x y value board)]
      (recur rest-coords x y value new-board))))

(defn write-piece-to-board
  "Returns a new board with a the given piece written to the coordinate on the board."
  [piece x y board]
  (let [value (:name piece)
        coords (:coords piece)]
    (write-coords-to-board coords x y value board)))

(defn rotate-piece
  "Create a new piece by rotating the given piece clockwise."
  [piece]
  (let [new-coords (map (fn [[x y]] [(- y) x]) (:coords piece))]
    (assoc piece :coords new-coords)))

(defn coord-empty?
  "Determines if the given coordinate on the board is empty."
  [x y board]
  (zero? (read-board x y board)))

(defn coord-fits?
  "Determines if the given relative coordinate fits at the position on the board."
  [[cx cy] x y board]
  (let [abs-x (+ x cx)
        abs-y (+ y cy)]
    (and (coord-inside? abs-x abs-y)
         (coord-empty? abs-x abs-y board))))

(defn piece-fits?
  "Determines if the given piece will collide with anything in the current board."
  [piece x y board]
  (every? #(coord-fits? % x y board) (:coords piece)))

;;------------------------------------------------------------
;; PAINTING (for showing the game on a canvas)
;;------------------------------------------------------------

(defn size-canvas
  "Set the size of the canvas."
  []
  (let [canvas (.getElementById js/document "canvas")]
    (aset canvas "width" (* cell-size 10))
    (aset canvas "height" (* cell-size 22))))

(defn get-cell-color
  "Get the color for the given cell value."
  [value]
  (if (zero? value) "#EEE" "#CC0000"))

(defn draw-cell
  "Draw the given cell of the given board."
  [ctx x y board]
  (let [color (get-cell-color (read-board x y board))
        left (* cell-size x)
        top  (* cell-size y)]
    (aset ctx "fillStyle" color)
    (.fillRect ctx left top cell-size cell-size)))

(defn draw-board
  "Draw the given board to the canvas."
  [board]
  (let [canvas (.getElementById js/document "canvas")
        ctx    (.getContext canvas "2d")]
    (doseq [x (range 10) y (range 22)]
      (draw-cell ctx x y board))
    nil))

;;------------------------------------------------------------
;; STATE OF THE GAME
;;------------------------------------------------------------

(def state (atom {:piece (get-rand-piece)
                  :position start-position
                  :board empty-board}))

;;------------------------------------------------------------
;; STATE MONITOR
;;------------------------------------------------------------

(defn create-drawable-board
  "Creates a new drawable board, by combining the current piece with the current board."
  []
  (let [piece (:piece @state)
        [x y] (:position @state)
        board (:board @state)]
  (write-piece-to-board piece x y board)))

(defn draw-state
  "Draw the current state of the board."
  []
  (draw-board (create-drawable-board)))

(add-watch state :draw draw-state)

;;------------------------------------------------------------
;; Game-driven STATE CHANGES
;;------------------------------------------------------------

(defn spawn-piece!
  "Spawns a random piece at the starting position."
  []
  (swap! state assoc :piece (get-rand-piece)
                     :position start-position))

;;------------------------------------------------------------
;; Input-driven STATE CHANGES
;;------------------------------------------------------------

(def key-codes {:left 37
                :up 38
                :right 39
                :down 40
                :space 32})

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
           (= code (:up key-codes))    (do (try-rotate!)     (.preventDefault e)))))))

;;------------------------------------------------------------
;; Facilities
;;------------------------------------------------------------

(defn connect-repl []
  (ajax {:url "repl-url"
         :cache false
         :dataType "text"
         :success #(repl/connect %)}))

(defn auto-refresh
  "Automatically refresh the page whenever a cljs file is compiled."
  []
  (let [url (.-href js/location)
        socket (.connect js/io url)]
    (.on socket "refresh" #(.reload js/location))))

;;------------------------------------------------------------
;; Entry Point
;;------------------------------------------------------------

(defn init []
  (size-canvas)
  (spawn-piece!)
  (add-key-events)

  (connect-repl)
  (auto-refresh)
  )

(document-ready init)
