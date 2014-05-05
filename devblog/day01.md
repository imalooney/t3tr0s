# Day 1

We created the foundation of the game today step-by-step.  We focused on doing
the simplest possible thing so we could test it immediately in the REPL.  This
resulted in many small functions and a tight feedback loop:

1. Make pieces.

    ```clj
    (def pieces
      {:I [[0,0],[0,-1],[0,1],[0,2]]
       :L [[0,0],[0,1],[1,1],[0,-1]]
       :J [[0,0],[0,-1],[0,1],[-1,1]]
       :S [[0,0],[-1,0],[0,-1],[1,-1]]
       :Z [[0,0],[-1,-1],[0,-1],[1,0]]
       :O [[0,0],[-1,0],[-1,1],[0,1]]
       :T [[0,0],[-1,0],[1,0],[0,1]]})
    ```

1. Make board.

    ```clj
    (def empty-board [[0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]
                      [0,0,0,0,0,0,0,0,0,0]])

    (def state (atom {:board empty-board}))
    ```

1. Print board as text.

    ```clj
    (defn get-cell-str
      "Return a space or asterisk for the given cell."
      [cell]
      (if (= 0 cell) "_" "*"))

    (defn row-str
      "Create a string from a board row."
      [row]
      (join "" (map get-cell-str row)))

    (defn board-str
      "Create a string from the tetris board."
      [board]
      (join "\n" (map row-str board)))

    (defn print-board
      "Prints the board to screen."
      []
      (println (board-str (:board @state))))
    ```

1. Write piece to board.

    ```clj
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
    ```

1. Clear board.

    ```clj
    (defn clear-board!
      "Clears the board."
      []
      (swap! state assoc :board empty-board))
    ```

1. Rotate piece.

    ```clj
    (defn rotate-piece
      "Create a new piece by rotating the given piece clockwise."
      [piece]
      (doall (map (fn [[x y]] [(- y) x]) piece)))
    ```

1. Determine if piece can be placed at a board position.

    ```clj
    (defn read-board
      "Get the current value from the given board position."
      [x y]
      (get-in @state [:board y x]))

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
    ```

1. Draw board on canvas.

    ```clj
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
    ```

