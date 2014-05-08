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
  [x y]
  (get-in @state [:board y x]))

(defn coord-inside?
  [x y]
  (and (<= 0 x 9) (<= 0 y 21)))

(defn write-to-board!
  "Writes a given value to the x,y position on the board."
  [ x y value ]
  (if (coord-inside? x y)
    (swap! state assoc-in [:board y x] value)))

(defn write-coord-to-board!
  [[cx cy] x y]
    (write-to-board! (+ cx x) (+ cy y) 1))

(defn write-piece-to-board!
  "Writes a given piece to the board."
  [piece x y]
  (doall (map #(write-coord-to-board! % x y) piece)))

(defn clear-board!
  "Clears the board."
  []
  (swap! state assoc :board empty-board))

(defn rotate-piece
  "Create a new piece by rotating the given piece clockwise."
  [piece]
  (doall (map (fn [[x y]] [(- y) x]) piece)))

(defn coord-occupied?
  [x y]
  (not= 0 (read-board x y)))

(defn coord-collide?
  [[cx cy] x y]
  (let [abs-x (+ x cx)
        abs-y (+ y cy)]
    (or (not (coord-inside? abs-x abs-y))
        (coord-occupied? abs-x abs-y))))

(defn piece-collide?
  "Determines if the given piece will collide with anything in the current board."
  [piece x y]
  (some #(coord-collide? % x y) piece))

; ------------------------------------------------------------
; DRAWING FUNCTIONS
; ------------------------------------------------------------

(def sq-size 20)

(defn size-canvas []
  (let [canvas (.getElementById js/document "canvas")]
    (aset canvas "width" (* sq-size 10))
    (aset canvas "height" (* sq-size 22))))

(defn draw-cell
  [ctx x y]
  (let [color (get-cell-color (read-board x y))
        left (* sq-size x)
        top  (* sq-size y)]

    (aset ctx "fillStyle" color)
    (.fillRect ctx left top sq-size sq-size)))

(defn draw-board []
  (let [canvas (.getElementById js/document "canvas")
        ctx    (.getContext canvas "2d")]
    (doall (for [x (range 10) y (range 22)]
      (draw-cell ctx x y)))))

; ------------------------------------------------------------
; TESTING FUNCTIONS
; ------------------------------------------------------------

(defn test-rotate-piece!
  "Clear the board, write the piece at 5, 9, rotate the piece, write the piece at 5, 1,
    print the board."
  [piece-key]
  (clear-board!)
  (write-piece-to-board! (piece-key pieces) 5 9)
  (write-piece-to-board! (rotate-piece (piece-key pieces)) 5 1)
  (draw-board)
  (print-board))

(defn test-piece!
  "Clear the board, write a piece at 5,9, and print it."
  [piece-key x y]
  (clear-board!)
  (write-piece-to-board! (piece-key pieces) x y)
  (draw-board)
  (print-board))

(defn test-collide-piece!
  "Test collision."
  [piece-key x y]
  (clear-board!)
  (write-piece-to-board! (:J pieces) 5 9)
  (println (piece-collide? (piece-key pieces) x y))
  (println "\n")
  (write-piece-to-board! (piece-key pieces) x y)
  (draw-board)
  (print-board))

(defn auto-refresh
  "Automatically refresh the page whenever a cljs file is compiled."
  []
  (let [url (.-href js/location)
        socket (.connect js/io url)]
    (.on socket "refresh" #(.reload js/location))))

(defn init []
  (connect-repl)
  (print-board)
  (size-canvas)
  (draw-board)
  (test-rotate-piece! :Z)

  (auto-refresh)
  )

(document-ready init)
