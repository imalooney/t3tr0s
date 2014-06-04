(ns client.chat.core
  (:require
    [client.chat :refer [chat-msg-html]]
    [client.socket :refer [socket]]
    [client.login :refer [get-username
                          get-color]]
    [cljs.reader :refer [read-string]]))

; alias the jquery variable
(def $ js/$)

;;------------------------------------------------------------
;; Chat
;;-----------------------------------------------------------
(defn send-message
  "Sends a message to the chat"
  []
  (.emit @socket "chat-message" (pr-str {:user (get-username)
                                         :msg (get-message)
                                         :color (get-color)})))
(defn get-message
  "Gets the latest message in the input field"
  []
  (let [input (.getElementById js/document "msg")]
    (aget input "value")))

(defn add-message
  "Prints a new chat message"
  ([msg] (add-message (:user msg) (:color msg) (:msg msg)))
  ([user color msg] (let [chat (.getElementById js/document "chat-messages")]
                (.append ($ chat) (chat-msg-html user color msg)))))

(defn init 
  "Starts the chat page" 
  []   
  ;; Add listeners
  (.click ($ "#submit") #(do (add-message (get-username) (get-color) (get-message))
                             (send-message)))
  
  ;; Server messages
  (.on @socket "new-message" #(add-message (read-string %))))

