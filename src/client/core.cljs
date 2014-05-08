(ns client.core
  (:require
    [clojure.browser.repl :as repl]
    [clojure.string :refer [join]]
    [jayq.core :refer [$ ajax document-ready]]))

;;------------------------------------------------------------
;; Connect to the Browser REPL
;;------------------------------------------------------------

(defn connect-repl []
  (ajax {:url "repl-url"
         :cache false
         :dataType "text"
         :success #(repl/connect %)}))

(enable-console-print!)

(def pieces
  {:I [[0 0] [0 -1] [0 1] [0 2]]
   :L [[0 0] [0 1] [1 1] [0 -1]]
   :J [[0 0] [0 -1] [0 1] [-1 1]]
   :S [[0 0] [-1 0] [0 -1] [1 -1]]
   :Z [[0 0] [-1 -1] [0 -1] [1 0]]
   :O [[0 0] [-1 0] [-1 1] [0 1]]
   :T [[0 0] [-1 0] [1 0] [0 1]]})

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

(def state (atom {:board empty-board}))

(defn get-cell-color
  [cell]
  (if (= 0 cell) "#EEE" "#F0F"))

(defn read-board
  "Get the current value from the given board position."
  [x y board]
  (get-in board [y x]))

(defn coord-inside?
  [x y]
  (and (<= 0 x 9) (<= 0 y 21)))

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
  [piece-key x y board]
  (let [value piece-key
        coords (piece-key pieces)]
    (write-coords-to-board coords x y value board)))

(defn rotate-piece
  "Create a new piece by rotating the given piece clockwise."
  [piece]
  (doall (map (fn [[x y]] [(- y) x]) piece)))

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
  (every? #(coord-fits? % x y board) piece))

; ------------------------------------------------------------
; DRAWING FUNCTIONS
; ------------------------------------------------------------

(def sq-size 20)

(defn size-canvas []
  (let [canvas (.getElementById js/document "canvas")]
    (aset canvas "width" (* sq-size 10))
    (aset canvas "height" (* sq-size 22))))

(defn draw-cell
  "Draw the given cell of the given board."
  [ctx x y board]
  (let [color (get-cell-color (read-board x y board))
        left (* sq-size x)
        top  (* sq-size y)]
    (aset ctx "fillStyle" color)
    (.fillRect ctx left top sq-size sq-size)))

(defn draw-board
  "Draw the given board to the canvas."
  [board]
  (let [canvas (.getElementById js/document "canvas")
        ctx    (.getContext canvas "2d")]
    (doseq [x (range 10) y (range 22)]
      (draw-cell ctx x y board))
    nil))

(defn auto-refresh
  "Automatically refresh the page whenever a cljs file is compiled."
  []
  (let [url (.-href js/location)
        socket (.connect js/io url)]
    (.on socket "refresh" #(.reload js/location))))

(defn init []
  (connect-repl)
  (size-canvas)
  (draw-board (:board @state))

  (auto-refresh)
  )

(document-ready init)
