(ns client.game.history
  )

(def size 5)

(def back-color "#FFF")
(def drop-color "#DDD")
(def block-color "#888")
(def collapse-color "#F00")

(def canvas nil)
(def ctx nil)

(defn init-canvas!
  [id]
  (set! canvas (.getElementById js/document id))
  (set! ctx (.getContext canvas "2d"))
  (aset canvas "height" (* 22 size))
  (aset canvas "width" (* 400 size))
  (.scale ctx size size))

(defn draw-state!
  [i s]
  (js/console.log i (:drop-y s) (:height s) (pr-str (:collapsed s)))
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
  (doseq [[i s] (map-indexed vector history)]
    (draw-state! i s)))
