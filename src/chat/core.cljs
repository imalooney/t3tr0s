(ns chat.core)

;;------------------------------------------------------------
;; Web Socket (for connectin to the server)
;;------------------------------------------------------------

(def socket (atom nil))
; alias the jquery variable
(def $ js/$)

(defn connect-socket!
  "Create a web socket connection to the server."
  []
  (let [url (.-origin js/location)]
    (reset! socket (.connect js/io url))))

;;------------------------------------------------------------
;; Chat
;;-----------------------------------------------------------
(defn send-message
  "Sends a message to the chat"
  []
  (let [input (.getElementById js/document "msg")]
    (.emit @socket "chat-message" (aget input "value"))))

(defn add-message
  "Prints a new chat message"
  [msg]
  (let [chat (.getElementById js/document "chat")
        p (.createElement js/document "p")]
    (.text ($ p) msg)
    (.appendChild chat p)))

(defn init 
  "Starts the chat page" []
   
  ;; Start the socket
  (connect-socket!)

  ;; Add listeners
  (.on ($ "#submit") "click" send-message)
  
  ;; Server messages
  (.on @socket "new-message" add-message))

(.addEventListener js/window "load" init)
