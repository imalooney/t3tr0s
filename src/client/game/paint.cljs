(ns client.game.paint
  (:require
    [client.dom :as dom]
    [client.game.board :refer [empty-board
                               read-board
                               board-size
                               piece-type-adj]]))

;;------------------------------------------------------------
;; PAINTING (for showing the game on a canvas)
;;------------------------------------------------------------

(defn get-image
  [path]
  (let [img (js/Image.)]
    (aset img "src" path)
    img))

(def tilemap-orig-real        (get-image "tilemap-orig-real.png"))
(def tilemap-tengen           (get-image "tilemap-tengen.png"))
(def tilemap-gameboy          (get-image "tilemap-gameboy.png"))
(def tilemap-gameboy-color    (get-image "tilemap-gameboy-color.png"))
(def tilemap-gameboy-real     (get-image "tilemap-gameboy-real.png"))
(def tilemap-gameboy-real-adj (get-image "tilemap-gameboy-real-adj.png"))
(def tilemap                  (get-image "tilemap.png"))

; The size of a cell in pixels.
(def cell-size 32)

(def value-position
  "An ordering imposed on the possible cell types, used for tilemap position."
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

(defn get-image-region
  "Get the tilemap and position for the image of the given cell value and theme."
  [theme value]
  (let [string-value (str value)]
    (cond 

      ; ORIGINAL REAL
      (= theme 10)
      (let [[k a] (piece-type-adj value)
            row 0
            col (value-position k)
            size 32]
        [tilemap-orig-real row col size])

      ; TENGEN
      (= theme 2)
        (let [[k a] (piece-type-adj value)
              row (value-position k)
              col a]
          [tilemap-tengen row col])

      ; GAMEBOY I-PIECE
      (and (= theme 3) (= (subs string-value 0 1) "I"))
        (let [[k a] (piece-type-adj value)
              row 0
              col a
              size 32]
          [tilemap-gameboy row col])

      ; GAMEBOY COLOR I-PIECE
      (and (= theme 6) (= (subs string-value 0 1) "I"))
        (let [[k a] (piece-type-adj value)
              row 0
              col a
              size 32]
          [tilemap-gameboy-color row col])

      ; GAMEBOY REAL
      (and (= theme 13) (not= (subs string-value 0 1) "I"))
        (let [[k a] (piece-type-adj value)
              row 0
              col (value-position k)
              size 40]
          [tilemap-gameboy-real row col size])

      ; GAMEBOY REAL I-PIECE
      (and (= theme 13) (= (subs string-value 0 1) "I"))
        (let [[k a] (piece-type-adj value)
              row 0
              col a
              size 40]
          [tilemap-gameboy-real-adj row col size])

      ; DEFAULT TILEMAP
      :else
        (let [[k _] (piece-type-adj value)
              row (if (= theme 19)
                    10
                    theme)
              col (value-position k)]
          [tilemap row col]))
    )
  )

(defn size-canvas!
  "Set the size of the canvas."
  ([id board scale] (size-canvas! id board scale 0))
  ([id board scale y-cutoff]
   (let [canvas (dom/by-id id)
         [w h] (board-size board)]
     (aset canvas "width" (* scale w))
     (aset canvas "height" (* scale (- h y-cutoff))))))

(defn draw-board!
  "Draw the given board to the canvas."
  ([id board scale theme] (draw-board! id board scale theme 0))
  ([id board scale theme y-cutoff]
    (let [canvas (dom/by-id id)
          ctx (.getContext canvas "2d")
          [w h] (board-size board)]
      (doseq [x (range w) y (range h)]
        (let [; tilemap region
              [img row col size] (get-image-region theme (read-board x y board))
              size (or size cell-size)

              ; source coordinates (on tilemap)
              sx (* size col) ; Cell-size is based on tilemap, always extract with that size
              sy (* size row)
              sw size
              sh size

              ; destination coordinates (on canvas)
              dx (* scale x)
              dy  (* scale (- y y-cutoff))
              dw scale
              dh scale]

          (.drawImage ctx img sx sy sw sh dx dy dw dh)))
      nil)))
