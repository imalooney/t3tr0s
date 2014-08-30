(ns client.socket
  (:require
    [cljs.reader :refer [read-string]]
    [client.state :as state]
    [client.util :as util]))

(declare
  emit)

;;------------------------------------------------------------------------------
;; Client --> Server
;;------------------------------------------------------------------------------

(defn send-username [username]
  (emit "set-name-d67ca" {
    :cid state/client-id
    :username username }))

(defn send-chat [msg]
  (emit "chat-msg-c3785" {
    :cid state/client-id
    :msg msg }))

(defn send-game-state [game-state]
  (emit "game-update-e25be" {
    :cid state/client-id
    :state game-state }))

;;------------------------------------------------------------------------------
;; Server --> Client
;;------------------------------------------------------------------------------

(defn- receive-chat-update [data-str]
  (let [data (read-string data-str)]
    (reset! client.state/chat data)))

;;------------------------------------------------------------------------------
;; Socket Interop
;;------------------------------------------------------------------------------

(def ^:private socket-id (util/uuid))

(defn on [evt-name evt-fn]
  (.on (aget js/window socket-id) evt-name evt-fn))

(defn emit
  ([evt-name]
    (.emit (aget js/window socket-id) evt-name))
  ([evt-name evt-data]
    (.emit (aget js/window socket-id) evt-name (pr-str evt-data))))

(defn connect!
  "Create a web socket connection to the server."
  []
  ;; create the socket object
  (aset js/window socket-id (js/io))

  ;; listen for updates
  (on "chat-update-d4779" receive-chat-update))

;; TODO: make this function multi-arity:
;; - (socket/removeListener "foo" "bar" "baz")
;; - (socket/removeListener ["foo" "bar"])
(defn removeListener [evt-name]
  (.removeAllListeners (aget js/window socket-id) evt-name))
