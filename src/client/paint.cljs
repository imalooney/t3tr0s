(ns client.paint
  (:require
    [client.board :refer [read-board
                          n-rows
                          n-rows-vis
                          n-cols]]))

;;------------------------------------------------------------
;; PAINTING (for showing the game on a canvas)
;;------------------------------------------------------------

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
   :G "#555"  ; ghost piece
   :H "#DDD"  ; highlighted (filled or about to collapse)
   })

(defn size-canvas!
  "Set the size of the canvas."
  []
  (let [canvas (.getElementById js/document "canvas")]
    (aset canvas "width" (* cell-size n-cols))
    (aset canvas "height" (* cell-size n-rows-vis))))

(defn draw-cell!
  "Draw the given cell of the given board."
  [ctx x y board]
  (let [color (cell-colors (read-board x y board))
        y-diff (- n-rows n-rows-vis)
        left (* cell-size x)
        top  (* cell-size (- y y-diff))]
    (aset ctx "fillStyle" color)
    (.fillRect ctx left top cell-size cell-size)))

(defn draw-board!
  "Draw the given board to the canvas."
  [board]
  (let [canvas (.getElementById js/document "canvas")
        ctx    (.getContext canvas "2d")]
    (doseq [x (range n-cols) y (range n-rows)]
      (draw-cell! ctx x y board))
    nil))

