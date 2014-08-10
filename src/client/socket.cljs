(ns client.socket
  (:require
    [cljs.reader :refer [read-string]]
    client.state
    [client.util :as util]))

(declare
  emit)

;;------------------------------------------------------------------------------
;; Client ID
;;------------------------------------------------------------------------------

;; get the client-id from localStorage or create a new one if needed
(def ^:private client-id
  (if-let [id (aget js/localStorage "client-id")]
    id
    (util/uuid)))

;; save client-id to localStorage
(aset js/localStorage "client-id" client-id)

;;------------------------------------------------------------------------------
;; Client --> Server
;;------------------------------------------------------------------------------

(defn send-username [username]
  (emit "set-name-d67ca" {
    :cid client-id
    :username username }))

(defn send-chat [msg]
  (emit "chat-msg-c3785" {
    :cid client-id
    :msg msg }))

(defn send-game-state [state]
  (emit "game-update-e25be" {
    :cid client-id
    :state state }))

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

  ;; inform the server that we are connected
  (emit "connect-42b28" client-id)

  ;; listen for updates
  (on "chat-update-d4779" receive-chat-update)
  )

;; TODO: make this function multi-arity:
;; - (socket/removeListener "foo" "bar" "baz")
;; - (socket/removeListener ["foo" "bar"])
(defn removeListener [evt-name]
  (.removeAllListeners (aget js/window socket-id) evt-name))
