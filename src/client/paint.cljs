(ns client.paint
  (:require
    [client.board :refer [read-board
                          board-size]]))

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
  ([id board scale] (size-canvas! id board scale 0))
  ([id board scale y-cutoff]
   (let [canvas (.getElementById js/document id)
         [w h] (board-size board)]
     (aset canvas "width" (* scale w))
     (aset canvas "height" (* scale (- h y-cutoff))))))

(defn draw-board!
  "Draw the given board to the canvas."
  ([id board scale] (draw-board! id board scale 0))
  ([id board scale y-cutoff]
    (let [canvas (.getElementById js/document id)
          ctx (.getContext canvas "2d")
          [w h] (board-size board)]
      (doseq [x (range w) y (range h)]
        (let [color (cell-colors (read-board x y board))
              left (* scale x)
              top  (* scale (- y y-cutoff))]
          (aset ctx "fillStyle" color)
          (.fillRect ctx left top scale scale)))
      nil)))

