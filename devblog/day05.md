# Day 5

<img src="http://i.imgur.com/F1ehgDk.gif" align="right" width="100px">

1. We started today by reviewing a recent splitting of our files:
    * core.cljs (game state)
    * board.cljs (board constants and pure functions)
    * paint.cljs (drawing related)
    * repl.cljs (connecting the browser repl)
    * socket.cljs (connecting to server)
    * vcr.cljs (recording gif)
1. We replaced the `:animating` state flag with a `nil` condition check on `:piece` state.
1. We prevented controls when piece is `nil`.
1. We detected game over before spawning a new piece.

    ```clj
    (defn try-spawn-piece!
      "Checks if new piece can be written to starting position."
      []
      (let [piece (get-rand-piece)
            [x y] start-position
            board (:board @state)]
        (if (piece-fits? piece x y board)
          (spawn-piece! piece)
          (go-go-game-over!))))
    ```

1. We added game over animation.

    ```clj
    (defn go-go-game-over!
      "Kicks off game over routine. (and get to the chopper)"
      []
      (go
        (doseq [y (reverse (range n-rows))
                x (range n-cols)]
          (if (even? x)
            (<! (timeout 2)))
          (swap! state update-in [:board] #(write-to-board x y :I %)))))
    ```


1. We added score state, and incremented it when collapsing rows.

    ```clj
    (def state (atom {:piece (get-rand-piece)
                      :position start-position
                      :board empty-board

                      :flashing-rows #{}

                      :score 0}))

    (defn collapse-rows!
      "Collapse all filled rows."
      []
      (let [board (:board @state)
            cleared-board (remove #(every? cell-filled? %) board)
            n (- (count board) (count cleared-board))
            new-board (into (vec (repeat n empty-row)) cleared-board)
            points (get-points n 1)]
        (swap! state assoc :board new-board)
        (swap! state update-in [:score] + points)))


    (defn get-points
      "Determine how many points were scored."
      [rows-cleared level]
      (case rows-cleared
        1 (* 40 level)
        2 (* 100 level)
        3 (* 300 level)
        4 (* 1200 level)))
    ```
