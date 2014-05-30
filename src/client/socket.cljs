(ns client.socket)

;;------------------------------------------------------------
;; Web Socket (for connecting to the server)
;;------------------------------------------------------------

(def socket (atom nil))
(def user-id (atom nil))

(defn connect-socket!
  "Create a web socket connection to the server."
  []
  (let [url (.-href js/location)]
    (reset! socket (.connect js/io url))
    (.on @socket "establish-id" (fn [id]
                                  (reset! user-id id)
                                  (js/console.log "USER-ID" id)))))

