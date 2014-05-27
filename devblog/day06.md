# Day 6

<img src="http://i.imgur.com/5Ua31TR.gif" align="right" width="100px">

Today I handled a lot of boring things so we can move on with art, networking,
and layout.

Some improvements were made: fluid drop, gameboy level speeds, gravity timer
reset (thanks Trey!), gameboy collapse animation, next piece generator, level
up logic.

I noticed the modern Tetris version prevent same consecutive pieces, so this
function will prevent the same piece from being selected twice in a row.  This
helps fix the strangely common case of seeing ~4 of the same pieces in a row:

```clj
(def pieces
  {:I {:name :I :coords [[0 0] [-1 0] [1 0] [2 0]]}
   :L {:name :L :coords [[0 0] [-1 0] [1 0] [1 -1]]}
   :J {:name :J :coords [[0 0] [-1 0] [1 0] [-1 -1]]}
   :S {:name :S :coords [[0 0] [-1 0] [0 -1] [1 -1]]}
   :Z {:name :Z :coords [[0 0] [-1 -1] [0 -1] [1 0]]}
   :O {:name :O :coords [[0 0] [1 0] [1 -1] [0 -1]]}
   :T {:name :T :coords [[0 0] [-1 0] [1 0] [0 -1]]}})

(defn get-rand-diff-piece
  "Return a random piece different from the given one."
  [piece]
  (pieces (rand-nth (keys (dissoc pieces (:name piece))))))
```

I also found a better way to handle flashing row animations so it doesn't store
explicit state.  I'm very happy with the way it stores the 3 animation frames
in the go-block, as immutable instances of the board, instead of rendering them
elsewhere based on a `:flashing-rows` state:

```clj
(defn go-go-collapse!
  "Starts the collapse animation if we need to, returning nil or the animation channel."
  []
  (let [board (:board @state)
        rows (get-filled-row-indices board)
        flashed-board (highlight-rows rows board)
        cleared-board (clear-rows rows board)]

    (when-not (zero? (count rows))
      (go
        ; blink n times
        (doseq [i (range 3)]
          (swap! state assoc :board flashed-board)
          (<! (timeout 170))
          (swap! state assoc :board board)
          (<! (timeout 170)))

        ; clear rows to create a gap, and pause
        (swap! state assoc :board cleared-board)
        (<! (timeout 220))

        ; finally collapse
        (collapse-rows! rows)))))
```

The fluid drop is worth mentioning here.  I'm no longer pushing the piece down
for every key-down event fired by the Javascript event.  Instead, I'm just
altering the gravity speed when I press down.  I'm pushing a boolean flag onto
a channel everytime a key-down event is fired, but I'm filtering that to throw
away repeats, so I only get a single "down" event.  Channels are pretty
powerful.  (Abridged version):

```clj
(defn add-key-events
  "Add all the key inputs."
  []
  (let [down-chan (chan)
        key-names {40 :down}
        key-name #(-> % .-keyCode key-names)]

    (.addEventListener js/window "keydown"
      (fn [e]
        (if (:piece @state)
          (case (key-name e)
            :down  (do (put! down-chan true) (.preventDefault e))
            nil))))

    (.addEventListener js/window "keyup"
      (fn [e]
        (case (key-name e)
          :down  (put! down-chan false)
          nil)))

    ; Listen to the down key, but ignore repeats.
    (let [c (unique down-chan)]
      (go
        (while true
          (let [on-or-off (<! c)]
            (swap! state assoc :soft-drop on-or-off)

            ; force gravity to reset
            (put! pause-grav 0)
            (put! resume-grav 0)))))))
```

We want the gravity timer to reset for new pieces, so the piece doesn't drop
earlier than it should after spawning.  I'm resolving this by sending "resume"
and "pause" signals to the gravity go-block.

I'm not sure if this is a good idea, but it sort of makes sense to have to
signal the isolated event loop to influence it.  A strange side effect of this
is that I have to send a pause signal immediately when starting.  If I don't,
then the first spawned piece will send a resume signal, which will buffer,
effectively causing the next pause signal to be ignored:

```clj
(defn go-go-gravity!
  "Starts the gravity routine."
  []
  ; Make sure gravity starts in paused mode.
  ; Spawning the piece will signal the first "resume".
  (put! pause-grav 0)

  (go
    (loop []
      (let [cs [(timeout speed) pause-grav] ; channels to listen to (timeout, pause)
            [_ c] (alts! cs)]               ; get the first channel to receive a value
        (if (= pause-grav c)                ; if "pause" received, wait for "resume"
          (<! resume-grav)
          (apply-gravity!))
        (recur)))))

; using this to pause gravity after piece lock:
(put! pause-grav 0)

; using to resume gravity after piece spawn:
(put! resume-grav 0)
```
