(ns client.socket
  (:require
    [client.util :refer [uuid]]))

;;------------------------------------------------------------------------------
;; Web Socket (for connecting to the server)
;;------------------------------------------------------------------------------

(def ^:private socket-id (uuid))

(defn connect!
  "Create a web socket connection to the server."
  []
  (aset js/window socket-id (js/io)))

(defn emit
  ([evt-name]
    (.emit (aget js/window socket-id) evt-name))
  ([evt-name evt-data]
    (.emit (aget js/window socket-id) evt-name (pr-str evt-data))))

(defn on [evt-name evt-fn]
  (.on (aget js/window socket-id) evt-name evt-fn))

;; TODO: make this function multi-arity:
;; - (socket/removeListener "foo" "bar" "baz")
;; - (socket/removeListener ["foo" "bar"])
(defn removeListener [evt-name]
  (.removeAllListeners (aget js/window socket-id) evt-name))