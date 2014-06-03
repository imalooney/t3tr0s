(ns client.chat.core
  (:require [client.socket :refer [socket]]))

; alias the jquery variable
(def $ js/$)

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
  "Starts the chat page" 
  []   
  ;; Add listeners
  (.on ($ "#submit") "click" send-message)
  
  ;; Server messages
  (.on @socket "new-message" add-message))


