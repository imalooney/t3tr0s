(ns client.game.vcr
  (:require
    [client.socket :as socket]))

;;------------------------------------------------------------------------------
;; VCR (record game)
;;------------------------------------------------------------------------------

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
        url (.toDataURL (:canvas @vcr))
        data (select-keys @client.game.core/state [:next-piece
                                                   :piece
                                                   :position
                                                   :board
                                                   :theme
                                                   :score
                                                   :level
                                                   :total-lines])]
    (swap! vcr update-in [:frames] conj {:dt dt :data-url url :state data})
    (swap! vcr assoc :prev-ms ms)))

(defn start-record!
  "Start recording."
  []
  (js/console.log "starting record")
  (swap! vcr assoc :canvas (.getElementById js/document "game-canvas")
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

(defn publish-canvas-record!
  "Push the recording to the server to be rendered."
  []
  (socket/emit "create-canvas-gif" (:frames @vcr)))

(defn publish-html-record!
  "Push the recording to the server to be rendered."
  []
  (socket/emit "create-html-gif" (:frames @vcr)))
