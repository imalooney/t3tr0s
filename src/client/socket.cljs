(ns client.socket)

;;------------------------------------------------------------
;; Web Socket (for connecting to the server)
;;------------------------------------------------------------

(def socket (atom nil))

(defn connect-socket!
  "Create a web socket connection to the server."
  []
  (let [url (aget js/location "href")]
    (reset! socket (.connect js/io url))))
