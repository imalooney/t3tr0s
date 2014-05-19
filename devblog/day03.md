# Day 3

<img src="http://i.imgur.com/XzonCuN.gif" align="right">

We started the day looking at the `core.async` library since I thought it would
drastically simplify the way we write time-based events.  It is similar to C#
Co-Routines in Unity.  So we built this simple app to test it out.

```clj
(ns async-test.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [put! chan <! timeout]]))

(enable-console-print!)

(println "Hello world!")

; async demo (a go-routine)
(go
  (<! (timeout 1000)) ; wait for 1 second
  (println "HI")
  (<! (timeout 1000)) ; wait for 1 second
  (println "WORLD"))

; same code without async (callback hell)
#_(js/setTimeout
  (fn []
    (println "HI")
    (js/setTimeout
      (fn []
        (println "WORLD"))
      1000))
    1000)

; another async demo (executes concurrently with first go-routine)
(go
  (loop [i 0]
    (println "hola " i)
    (<! (timeout 500))
    (if (< i 6)
      (recur (inc i)))))
```

This outputs:

```
hola 0
hola 1
HI
hola 2
hola 3
WORLD
hola 4
hola 5
hola 6
```

After that experiment, we implemented gravity, stacking, collapsing, and
hard-drop.  Most of our time was spent figuring out ways to do things the
"functional" way.  The other time was spent trying to make it readable and
succinct.

1. Prevent square from rotating.

    ```clj
    (defn rotate-piece
      "Create a new piece by rotating the given piece clockwise."
      [piece]
      (if (= :O (:name piece))
        piece
        (let [new-coords (map (fn [[x y]] [(- y) x]) (:coords piece))]
          (assoc piece :coords new-coords))))
    ```

1. Add gravity and stacking.

    ```clj
    (defn lock-piece!
      "Lock the current piece into the board."
      []
      (let [[x y] (:position @state)
            piece (:piece @state)
            board (:board @state)]
        (swap! state assoc :board (write-piece-to-board piece x y board))
        (try-collapse!)
        (spawn-piece!)))

    (defn go-go-gravity!
      "Starts the gravity routine."
      []
      (go
        (loop []
          (<! (timeout 1000))
          (let [[x y] (:position @state)
                piece (:piece @state)
                board (:board @state)
                ny (inc y)]
            (if (piece-fits? piece x ny board)
              (swap! state assoc-in [:position 1] ny)
              (lock-piece!)))
          (recur))))
    ```

1. Add coloring.

    ```clj
    (def cell-colors
      { 0 "#333"
       :I "#0FF"
       :L "#FA0"
       :J "#00F"
       :S "#0F0"
       :Z "#F00"
       :O "#FF0"
       :T "#A0F"})
    ```

1. Collapse of filled rows.

    ```clj
    (def cell-filled? (complement zero?))

    (defn try-collapse!
      "Try to collapse any full rows on the current board."
      []
      (let [board (:board @state)
            cleared-board (remove #(every? cell-filled? %) board)
            n (- (count board) (count cleared-board))
            new-board (into (vec (repeat n empty-row)) cleared-board)]
        (swap! state assoc :board new-board)))
    ```

1. Add hard-drop.

    ```clj
    (defn get-drop-pos
      "Get the future drop position of the given piece."
      [piece x y board]
      (let [collide? (fn [cy] (not (piece-fits? piece x cy board)))
            cy (first (filter collide? (iterate inc y)))]
        (dec cy)))

    (defn hard-drop!
      "Hard drop the current piece."
      []
      (let [[x y] (:position @state)
            piece (:piece @state)
            board (:board @state)
            ny (get-drop-pos piece x y board)]
        (swap! state assoc :position [x ny])
        (lock-piece!)))
    ```
