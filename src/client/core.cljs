(ns client.core
  (:require
    cljsjs.jquery
    client.dom
    [client.repl :as repl]
    client.routes
    [client.socket :as socket]
    [client.util :as util]))

(enable-console-print!)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init! []

  ;; Connect to REPL for development.
  (if (aget js/window "T3TR0S_CONFIG" "use-repl")
    (repl/connect!))

  ;; initialize the socket connection
  (socket/connect!)

  ;; init routing
  (client.routes/init!))

($ init!)
