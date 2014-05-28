(ns client.paint
  (:require
    [client.board :refer [read-board
                          board-size]]))

;;------------------------------------------------------------
;; PAINTING (for showing the game on a canvas)
;;------------------------------------------------------------

(def tilemap (let [img (js/Image.)]
               (aset img "src" "tilemap.png")
               img))

; The size of a cell in pixels.
(def cell-size 32)

(def cell-columns
  { 0 0
   :I 1
   :L 2
   :J 3
   :S 4
   :Z 5
   :O 6
   :T 7
   :G 8  ; ghost piece
   :H 9  ; highlighted (filled or about to collapse)
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
  ([id board scale level] (draw-board! id board scale level 0))
  ([id board scale level y-cutoff]
    (let [canvas (.getElementById js/document id)
          ctx (.getContext canvas "2d")
          [w h] (board-size board)]
      (doseq [x (range w) y (range h)]
        (let [; tilemap position
              row (mod level 10)
              col (cell-columns (read-board x y board))

              ; source coordinates (on tilemap)
              sx (* scale col)
              sy (* scale row)
              sw scale
              sh scale

              ; destination coordinates (on canvas)
              dx (* scale x)
              dy  (* scale (- y y-cutoff))
              dw scale
              dh scale]

          (.drawImage ctx tilemap sx sy sw sh dx dy dw dh)))
      nil)))

