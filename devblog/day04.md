# Day 4

<img src="http://i.imgur.com/eq19e4b.gif" align="right" width="100px">

Today we implemented the ghost piece.  We also made the rows flash before they
collapse.  The ghost piece was remarkably simple, but the flashing animation
required a lot of thinking about how we wanted to handle it, since this
marks the introduction of timed-animations into our game.

The only bugs we encountered were simple.  We use an empty vector
instead of an empty set when clearing our `:flashing-rows` state.
And we reversed condition to kick off an animation, and forgot
a few times to uncomment some code we were verifying.

1. Ghost piece

    ```clj
    (defn create-drawable-board
      "Creates a new drawable board, by combining the current piece with the current board."
      []
      (let [piece (:piece @state)
            [x y] (:position @state)
            board (:board @state)
            ghost (assoc piece :name :G)                    ; copy piece, change value to :G
            gy    (get-drop-pos piece x y board)            ; get drop position
            board1 (write-piece-to-board ghost x gy board)  ; draw ghost at drop position
            board2 (write-piece-to-board piece x y board1)]
        board2))
    ```

1. Prepare state for flashing row animation:

```clj
(def state (atom {:piece (get-rand-piece)
                  :position start-position
                  :board empty-board

                  ; ANIMATION ADDITIONS
                  :animating false
                  :flashing-rows #{}}))
```

1. Get the indices of the filled rows (pure function):

```clj
(defn get-filled-row-indices
  "Get the indices of the filled rows for the given board."
  [board]
  (->> (map-indexed vector board)                        ; indexed rows [[0 r] [1 r]]
       (filter (fn [[i row]] (every? cell-filled? row))) ; choose filled [1 r]
       (map first)                                       ; select index only
       (apply hash-set)))                                ; convert to a set     
```

1. Draw highlighted rows on the given board (pure function):

```clj
(defn highlight-rows
  "Returns a new board with the given rows highlighted."
  [active-rows board]
  (vec (map-indexed
   (fn [i row]
     (if (active-rows i) highlighted-row row)) board)))
```

1. Add row-flashing to `create-drawable-board`:

```clj
(defn create-drawable-board
  "Creates a new drawable board, by combining the current piece with the current board."
  []
  (let [piece (:piece @state)
        [x y] (:position @state)
        board (:board @state)
        ghost (assoc piece :name :G)
        gy    (get-drop-pos piece x y board)
        board1 (write-piece-to-board ghost x gy board)
        board2 (write-piece-to-board piece x y board1)
        board3 (highlight-rows (:flashing-rows @state) board2)] ; <-- DRAW HERE
    board3))
```

1. Create animation go-routine for flashing the rows:

```clj
(defn go-go-collapse!
  "Starts the collapse animation if we need to, returning nil or the animation channel."
  []
  (let [board (:board @state)
        rows (get-filled-row-indices board)]

    (when (> (count rows) 0)
      (swap! state assoc :animating true)
      (go
        ; blink n times
        (doseq [i (range 3)]

          (<! (timeout 100))                      ; resume here later
          (swap! state assoc :flashing-rows rows) ; flash rows

          (<! (timeout 100))                      ; resume here later
          (swap! state update-in
                 [:flashing-rows] empty))         ; unflash rows

        ; finally collapse
        (collapse-rows!)
        (swap! state assoc :animating false)))))
```

1. Make sure gravity is off while we are animating:

```clj
(defn go-go-gravity!
  "Starts the gravity routine."
  []
  (go
    (loop []
      (<! (timeout 1000))
      (when-not (:animating @state) ; <-- CHECK HERE
        (let [[x y] (:position @state)
              piece (:piece @state)
              board (:board @state)
              ny (inc y)]
          (if (piece-fits? piece x ny board)
            (swap! state assoc-in [:position 1] ny)
            (lock-piece!))))
      (recur))))
```

1. Wait until after the collapse animation to spawn a new piece.

```clj
(defn lock-piece!
  "Lock the current piece into the board."
  []
  (let [[x y] (:position @state)
        piece (:piece @state)
        board (:board @state)]
    (swap! state assoc :board (write-piece-to-board piece x y board))

    ; If collapse routine returns a animation channel...
    ; then wait for it before spawning a new piece.
    (if-let [collapse-anim (go-go-collapse!)]
      (go (<! collapse-anim) (spawn-piece!))
      (spawn-piece!))))
```
