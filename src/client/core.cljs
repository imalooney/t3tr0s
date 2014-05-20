(ns client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [clojure.browser.repl :as repl]
    [clojure.string :refer [join]]
    [jayq.core :refer [$ ajax document-ready]]
    [cljs.core.async :refer [put! chan <! timeout]]))

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

(def empty-row (vec (repeat 10 0)))
(def empty-board (vec (repeat 22 empty-row)))

; The starting position of all pieces.
(def start-position [4 2])

(defn coord-inside?
  "Determines if the coordinate is inside the board."
  [x y]
  (and (<= 0 x 9) (<= 0 y 21)))


; The size of a cell in pixels.
(def cell-size 20)

(def cell-colors
  { 0 "#333"
   :I "#0FF"
   :L "#FA0"
   :J "#00F"
   :S "#0F0"
   :Z "#F00"
   :O "#FF0"
   :T "#A0F"
   :G "#555"})

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
    (dec cy)))

;;------------------------------------------------------------
;; PAINTING (for showing the game on a canvas)
;;------------------------------------------------------------

(defn size-canvas
  "Set the size of the canvas."
  []
  (let [canvas (.getElementById js/document "canvas")]
    (aset canvas "width" (* cell-size 10))
    (aset canvas "height" (* cell-size 22))))

(defn draw-cell
  "Draw the given cell of the given board."
  [ctx x y board]
  (let [color (cell-colors (read-board x y board))
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
;; Web Socket (for connecting to the server)
;;------------------------------------------------------------

(def socket (atom nil))

(defn connect-socket!
  "Create a web socket connection to the server."
  []
  (let [url (.-href js/location)]
    (reset! socket (.connect js/io url))))

;;------------------------------------------------------------
;; STATE OF THE GAME
;;------------------------------------------------------------

(def state (atom {:piece (get-rand-piece)
                  :position start-position
                  :board empty-board}))

;;------------------------------------------------------------
;; VCR (record game)
;;------------------------------------------------------------

(def vcr (atom {:canvas nil
                :recording false
                :prev-ms nil
                :frames []}))

(defn get-clock
  "Get the number of milliseconds elapsed since 1970."
  []
  (.now js/Date))

(defn record-frame!
  "Record the image and time of this frame."
  []
  (let [ms (get-clock)
        dt (if-let [prev-ms (:prev-ms @vcr)]
             (- ms prev-ms)
             0)
        prev-url (if-let [prev-frame (last (:frames @vcr))]
                   (:data-url prev-frame))
        url (.toDataURL (:canvas @vcr))]
    (when (not= prev-url url)
      (swap! vcr update-in [:frames] conj {:dt dt :data-url url})
      (swap! vcr assoc :prev-ms ms))))

(defn start-record!
  "Start recording."
  []
  (js/console.log "starting record")
  (swap! vcr assoc :canvas (.getElementById js/document "canvas")
                   :prev-ms nil
                   :recording true
                   :frames [])
  (record-frame!))

(defn stop-record!
  "Stop recording."
  []
  (js/console.log "stopping record")
  (swap! vcr assoc :recording false))

(defn toggle-record!
  "Toggle recording."
  []
  (if (:recording @vcr)
    (stop-record!)
    (start-record!)))

(defn publish-record!
  "Push the recording to the server to be rendered."
  []
  (let [data (pr-str (:frames @vcr))]
    (.emit @socket "create-gif" data)))

;;------------------------------------------------------------
;; STATE MONITOR
;;------------------------------------------------------------

(defn create-drawable-board
  "Creates a new drawable board, by combining the current piece with the current board."
  []
  (let [piece (:piece @state)
        [x y] (:position @state)
        board (:board @state)
        ghost (assoc piece :name :G)
        gy    (get-drop-pos piece x y board)
        board1 (write-piece-to-board ghost x gy board)
        board2 (write-piece-to-board piece x y board1)]
    board2))

(defn draw-state
  "Draw the current state of the board."
  []
  (draw-board (create-drawable-board))
  (if (:recording @vcr)
    (record-frame!)))

(add-watch state :draw draw-state)

;;------------------------------------------------------------
;; Game-driven STATE CHANGES
;;------------------------------------------------------------

(defn spawn-piece!
  "Spawns a random piece at the starting position."
  []
  (swap! state assoc :piece (get-rand-piece)
                     :position start-position))

(defn try-collapse!
  "Try to collapse any full rows on the current board."
  []
  (let [board (:board @state)
        cleared-board (remove #(every? cell-filled? %) board)
        n (- (count board) (count cleared-board))
        new-board (into (vec (repeat n empty-row)) cleared-board)]
    (swap! state assoc :board new-board)))

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

;;------------------------------------------------------------
;; Input-driven STATE CHANGES
;;------------------------------------------------------------

(def key-codes {:left 37
                :up 38
                :right 39
                :down 40
                :space 32
                :shift 16})

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

(defn hard-drop!
  "Hard drop the current piece."
  []
  (let [[x y] (:position @state)
        piece (:piece @state)
        board (:board @state)
        ny (get-drop-pos piece x y board)]
    (swap! state assoc :position [x ny])
    (lock-piece!)))

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
           (= code (:space key-codes)) (do (hard-drop!)      (.preventDefault e))
           (= code (:up key-codes))    (do (try-rotate!)     (.preventDefault e))))))
  (.addEventListener js/window "keyup"
     (fn [e]
       (let [code (aget e "keyCode")]
         (cond
           (= code (:shift key-codes)) (toggle-record!))))))

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
  (.on @socket "refresh" #(.reload js/location)))

;;------------------------------------------------------------
;; Entry Point
;;------------------------------------------------------------

(defn init []
  (size-canvas)
  (spawn-piece!)
  (add-key-events)
  (go-go-gravity!)

  (connect-repl)
  (connect-socket!)
  (auto-refresh)
  )

(document-ready init)
