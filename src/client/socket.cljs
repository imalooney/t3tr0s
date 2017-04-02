(ns client.socket
  (:require
    [cljs.reader :refer [read-string]]
    [client.state :as state]
    [client.util :as util]
    [client.config :refer [single-player-only?]]))

(declare
  emit)

;;------------------------------------------------------------------------------
;; Server --> Client
;;------------------------------------------------------------------------------

(defn- receive-chat-update [data-str]
  (let [data (read-string data-str)]
    (reset! client.state/chat data)))

;;------------------------------------------------------------------------------
;; Socket Interop
;;------------------------------------------------------------------------------

(def ^:private socket-id (str (random-uuid)))

(defn on [evt-name evt-fn]
  (.on (aget js/window socket-id) evt-name evt-fn))

(defn emit
  ([evt-name]
   (.emit (aget js/window socket-id) evt-name))
  ([evt-name evt-data]
   (.emit (aget js/window socket-id) evt-name (pr-str evt-data))))

(defn stub-connection!
  "Allow us to play without socket connection w/o changing game code."
  []
  (aset js/window socket-id
    #js {:on (fn [])
         :emit (fn [])
         :removeAllListeners (fn [])}))

(defn connect!
  "Create a web socket connection to the server."
  []
  ;; create the socket object
  (aset js/window socket-id (js/io))

  ;; listen for updates
  (on "chat-update2" receive-chat-update))

;; TODO: make this function multi-arity:
;; - (socket/removeListener "foo" "bar" "baz")
;; - (socket/removeListener ["foo" "bar"])
(defn removeListener [evt-name]
  (.removeAllListeners (aget js/window socket-id) evt-name))
