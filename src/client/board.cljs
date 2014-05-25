(ns client.board)

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

(def empty-row       (vec (repeat 10 0)))
(def highlighted-row (vec (repeat 10 :H)))
(def empty-board (vec (repeat 22 empty-row)))

; The starting position of all pieces.
(def start-position [4 2])

(defn coord-inside?
  "Determines if the coordinate is inside the board."
  [x y]
  (and (<= 0 x 9) (<= 0 y 21)))

(def cell-filled? (complement zero?))

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

(defn highlight-rows
  "Returns a new board with the given rows highlighted."
  [active-rows board]
  (vec (map-indexed
   (fn [i row]
     (if (active-rows i) highlighted-row row)) board)))

(defn get-filled-row-indices
  "Get the indices of the filled rows for the given board."
  [board]
  (->> (map-indexed vector board)                        ; indexed rows [[0 r] [1 r]]
       (filter (fn [[i row]] (every? cell-filled? row))) ; choose filled [1 r]
       (map first)                                       ; select index only
       (apply hash-set)))                                ; convert to a set     

(defn rotate-piece
  "Create a new piece by rotating the given piece clockwise."
  [piece]
  (if (= :O (:name piece))
    piece
    (let [new-coords (map (fn [[x y]] [(- y) x]) (:coords piece))]
      (assoc piece :coords new-coords))))

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

(defn get-drop-pos
  "Get the future drop position of the given piece."
  [piece x y board]
  (let [collide? (fn [cy] (not (piece-fits? piece x cy board)))
        cy (first (filter collide? (iterate inc y)))]
    (max y (dec cy))))

(defn create-drawable-board
  "Creates a new drawable board, by combining the current piece with the current board."
  [piece x y board flashing-rows]
  (if piece
    (let [ghost (assoc piece :name :G)
          gy    (get-drop-pos piece x y board)
          board1 (write-piece-to-board ghost x gy board)
          board2 (write-piece-to-board piece x y board1)
          board3 (highlight-rows flashing-rows board2)]
      board3)
    (let [board3 (highlight-rows flashing-rows board)]
      board3)))
