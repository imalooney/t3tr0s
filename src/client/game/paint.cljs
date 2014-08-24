(ns client.game.paint
  (:require
    [client.dom :as dom]
    [client.game.multiplayer :refer [opponent-scale]]
    [client.game.board :refer [empty-board
                               read-board
                               board-size
                               piece-type-adj]]))

;;------------------------------------------------------------
;; PAINTING (for showing the game on a canvas)
;;------------------------------------------------------------

(def tilemap-orig-real (let [img (js/Image.)]
                         (aset img "src" "tilemap-orig-real.png")
                         img))

(def tilemap-tengen (let [img (js/Image.)]
                      (aset img "src" "tilemap-tengen.png")
                      img))

(def tilemap-gameboy (let [img (js/Image.)]
                      (aset img "src" "tilemap-gameboy.png")
                      img))

(def tilemap-gameboy-color (let [img (js/Image.)]
                      (aset img "src" "tilemap-gameboy-color.png")
                      img))

(def tilemap-gameboy-real (let [img (js/Image.)]
                            (aset img "src" "tilemap-gameboy-real.png")
                            img))

(def tilemap-gameboy-real-adj (let [img (js/Image.)]
                                (aset img "src" "tilemap-gameboy-real-adj.png")
                                img))

(def tilemap (let [img (js/Image.)]
               (aset img "src" "tilemap.png")
               img))

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
              row theme
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

(defn create-opponent-canvas!
  "Draw each opponents board"
  [id]
  (if (nil? (dom/by-id id))
    (let [arena (dom/by-id "arena")
          canvas (.createElement js/document "canvas")]
      (.appendChild arena canvas)
      (aset canvas "id" id)
      (size-canvas! id empty-board (opponent-scale cell-size))
      )))

(defn delete-opponent-canvas!
  [id]
  (let [arena (dom/by-id "arena")
        canvas (dom/by-id id)]
    (if-not (nil? canvas)
      (.removeChild arena canvas))))

;;------------------------------------------------------------
;; FX Canvas
;;------------------------------------------------------------

(def fx-source nil)
(def fx-canvas nil)
(def fx-texture nil)
(def fx-w nil)
(def fx-h nil)

(defn init-fx-canvas!
  []
  (set! fx-source   (.. js/document (getElementById "game-canvas")))
  (set! fx-w        (.. fx-source -width))
  (set! fx-h        (.. fx-source -height))
  (set! fx-canvas   (.. js/fx canvas))
  (set! fx-texture  (.. fx-canvas (texture fx-source)))

  (.. fx-source -parentNode (insertBefore fx-canvas fx-source))

  )

(defn draw-fx-canvas!
  []
  (.. fx-texture (loadContentsOf fx-source))
  (.. fx-canvas
      (draw fx-texture)
      (bulgePinch (/ fx-w 2) (/ fx-h 2) (* fx-h 0.75) 0.12)
      (vignette 0.25 0.74)
      (update))
  )

