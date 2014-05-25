(ns client.vcr
  (:require
    [client.socket :refer [socket]]))

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

