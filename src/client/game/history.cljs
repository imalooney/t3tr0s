(ns client.game.history
  )

(def prev-columns nil)

(def size 5)

(def back-color "#FFF")
(def drop-color "#EEE")
(def block-color "#BBB")
(def collapse-color "#F00")

(def canvas nil)
(def ctx nil)

(defn resize-canvas!
  [columns]
  (aset canvas "height" (* 22 size))
  (aset canvas "width" (* columns size))
  )

(defn init-canvas!
  [id]
  (set! canvas (.getElementById js/document id))
  (set! ctx (.getContext canvas "2d"))
  )

(defn draw-state!
  [i s]

  (aset ctx "fillStyle" back-color)
  (.fillRect ctx i 0 1 22)

  (aset ctx "fillStyle" drop-color)
  (.fillRect ctx i 0 1 (:drop-y s))

  (aset ctx "fillStyle" block-color)
  (let [h (:height s)]
    (.fillRect ctx i (- 22 h) 1 h))

  (aset ctx "fillStyle" collapse-color)
  (doseq [y (:collapsed s)]
    (.fillRect ctx i y 1 1))
  )

(defn draw-history!
  [history]

  (let [columns (count history)]
    (if (not= columns prev-columns)
      (resize-canvas! columns))
    (set! prev-columns columns))

  (.scale ctx size size)
  (doseq [[i s] (map-indexed vector history)]
    (draw-state! i s))
  (.setTransform ctx 1 0 0 1 0 0)
  )
