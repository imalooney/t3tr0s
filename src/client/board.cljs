(ns client.board)

;;------------------------------------------------------------
;; Pieces.
;;------------------------------------------------------------

; The available pieces resemble letters I,L,J,S,Z,O,T.
; Each piece structure is stored in :coords as [x y a].
; The "a" component of :coords stands for adjacency,
; which is a number with bit flags UP, RIGHT, DOWN, LEFT.

; For example, the coords for the J piece:
;
;       ********
;       * X=-1 *
;       * Y=-1 *
;       *      *
;       **********************
;       * X=-1 * X=0  * X=1  *
;       * Y=0  * Y=0  * Y=0  *
;       *      *      *      *
;       **********************
;
; We also need to encode "adjacency" information so we
; can graphically connect tiles of the same piece.
; These codes require explanation:
;
;       ********
;       *      *
;       * A=4  *
;       *      *
;       **********************
;       *      *      *      *
;       * A=3  * A=10 * A=8  *
;       *      *      *      *
;       **********************
;
; Adjacency codes are 4-bit numbers (for good reason),
; with each bit indicating adjacency along its respective direction:
;
;     UP  RIGHT  DOWN  LEFT -> binary -> CODE (decimal)
;     -     -     -     -      0000      0
;     X     -     -     -      0001      1
;     -     X     -     -      0010      2
;     X     X     -     -      0011      3  <-- shown in above example
;     -     -     X     -      0100      4  <-- shown in above example
;     X     -     X     -      0101      5
;     -     X     X     -      0110      6
;     X     X     X     -      0111      7
;     -     -     -     X      1000      8  <-- shown in above example
;     X     -     -     X      1001      9
;     -     X     -     X      1010      10 <-- shown in above example
;     X     X     -     X      1011      11
;     -     -     X     X      1100      12
;     X     -     X     X      1101      13
;     -     X     X     X      1110      14
;     X     X     X     X      1111      15 (not possible in tetris)
; 
; The revelation here is that SIMPLE ROTATION of the piece
; is achieved by applying this function over each coordinate:
;
;     Rotate( [X Y A] )  -->   [ -Y X (4 bit rotate of A) ]
; 

(def pieces
  {:I {:name :I
       :coords [
        [-1  0  2] [ 0  0 10] [ 1  0 10] [ 2  0  8]
        ]}

   :L {:name :L
       :coords [
                              [ 1 -1  4]
        [-1  0  2] [ 0  0 10] [ 1  0  9]
        ]}

   :J {:name :J
       :coords [
        [-1 -1  4]
        [-1  0  3] [ 0  0 10] [ 1  0  8]
        ]}

   :S {:name :S
       :coords [
                   [ 0 -1  6] [ 1 -1  8]
        [-1  0  2] [ 0  0  9]
        ]}

   :Z {:name :Z
       :coords [
        [-1 -1  2] [ 0 -1 12]
                   [ 0  0  3] [ 1  0  8]
        ]}

   :O {:name :O
       :coords [
                   [ 0 -1  6] [ 1 -1 12]
                   [ 0  0  3] [ 1  0  9]
        ]}

   :T {:name :T
       :coords [
                   [ 0 -1  4]
        [-1  0  2] [ 0  0 11] [ 1  0  8]
        ]}})

(defn get-rand-diff-piece
  "Return a random piece different from the given one."
  [piece]
  (pieces (rand-nth (keys (dissoc pieces (:name piece))))))

(defn get-rand-piece
  "Return a random piece."
  []
  (pieces (rand-nth (keys pieces))))

(defn rotate-piece
  "Create a new piece by rotating the given piece clockwise."
  [piece]
  (if (= :O (:name piece))
    piece
    (let [br (fn [a] (+ (* 2 (mod a 8)) (/ (bit-and a 8) 8)))
          new-coords (map (fn [[x y a]] [(- y) x (br a)]) (:coords piece))]
      (assoc piece :coords new-coords))))

(defn piece-value
  "Creates a cell value from the given piece type and adjacency."
  [t a]
  (if (zero? t) 0 (str (name t) a)))

(defn piece-type-adj
  "Gets the piece type and adjacency from a cell value string."
  [value]
  (let [t (if (zero? value) 0 (keyword (first value))) ; get the value key (piece type)
        a (if (zero? value) 0 (int (subs value 1)))]   ; get the adjacency code
    [t a]))

(defn update-adj
  "Updates the adjacency of the given cell value."
  [value f]
  (let [[t a] (piece-type-adj value)
        new-a (f a)]
    (piece-value t new-a)))

;;------------------------------------------------------------
;; Board.
;;------------------------------------------------------------

; conventions for standard board size
(def n-rows 22)
(def n-cols 10)
(def rows-cutoff 1.5)

(def empty-row       (vec (repeat n-cols 0)))
(def highlighted-row (vec (concat ["H2"] (repeat (- n-cols 2) "H10") ["H8"])))
(def empty-board (vec (repeat n-rows empty-row)))

; The starting position of all pieces.
(def start-position [4 2])

(defn coord-inside?
  "Determines if the coordinate is inside the board."
  [x y]
  (and (<= 0 x (dec n-cols))
       (<= 0 y (dec n-rows))))

(def cell-filled? (complement zero?))

;;------------------------------------------------------------
;; Pure Functions operating on a board.
;;------------------------------------------------------------

(defn board-size
  "Get the size of the given board as [w h]."
  [board]
  (let [w (count (first board))
        h (count board)]
    [w h]))

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
  [[cx cy ca] x y value board]
    (write-to-board (+ cx x) (+ cy y) (piece-value value ca) board))

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

(defn write-piece-behind-board
  "Like write-piece-to-board, but only draws to empty cells, to make it look like it's drawing behind."
  [piece x y board]
  (let [value (:name piece)
        can-write? (fn [[cx cy]] (zero? (read-board (+ x cx) (+ y cy) board)))
        coords (filter can-write? (:coords piece))]
    (write-coords-to-board coords x y value board)))

(defn highlight-rows
  "Returns a new board with the given rows highlighted."
  [active-rows board]
  (vec (map-indexed
   (fn [i row]
     (if (active-rows i) highlighted-row row)) board)))

(defn collapse-rows
  "Returns a new board with the given row indices collapsed."
  [rows board]
  (let [cleared-board (->> board
                           (map-indexed vector)
                           (remove #(rows (first %)))
                           (map second))
        n (count rows)
        new-board (into (vec (repeat n empty-row)) cleared-board)]
    new-board))

(defn sever-row
  "Return a new row, severing its adjacency across the given boundary."
  [row dir]
  (let [adj (if (= dir :up) (+ 2 4 8) (+ 1 2 8))
        new-row (vec (map #(update-adj % (fn [a] (bit-and a adj))) row))]
    new-row))

(defn sever-row-neighbors
  "Return a new board, disconnecting the adjacency of the rows neighboring the given row index."
  [i board]
  (let [row-up (get board (dec i))
        board1 (if row-up
                 (assoc board (dec i) (sever-row row-up :down))
                 board)
        row-down (get board (inc i))
        board2 (if row-down
                 (assoc board1 (inc i) (sever-row row-down :up))
                 board1)]
    board2))
        
(defn clear-rows
  "Return a new board with the given row indices cleared."
  [rows board]
  (if (zero? (count rows))
    board
    (let [next-rows (rest rows)
          i (first rows)
          severed-board (sever-row-neighbors i board)
          next-board (assoc severed-board i empty-row)]
      (recur next-rows next-board))))

(defn get-filled-row-indices
  "Get the indices of the filled rows for the given board."
  [board]
  (->> (map-indexed vector board)                        ; indexed rows [[0 r] [1 r]]
       (filter (fn [[i row]] (every? cell-filled? row))) ; choose filled [1 r]
       (map first)                                       ; select index only
       (apply hash-set)))                                ; convert to a set     

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
  [piece x y board]
  (if piece
    (let [ghost (assoc piece :name :G)
          gy    (get-drop-pos piece x y board)
          board1 (write-piece-to-board ghost x gy board)
          board2 (write-piece-to-board piece x y board1)]
      board2)
    board))

;;------------------------------------------------------------
;; Next piece board generator.
;;------------------------------------------------------------

(defn next-piece-board
  "Returns a small board for drawing the next piece."
  ([] (next-piece-board nil))
  ([piece]
    (let [board [[0 0 0 0]
                 [0 0 0 0]
                 [0 0 0 0]
                 [0 0 0 0]]]
      (if piece
        (write-piece-to-board piece 1 2 board)
        board))))

