(ns client.core
  (:require
    cljsjs.jquery
    client.dom
    [client.repl :as repl]
    client.routes
    [client.socket :as socket]
    [client.util :as util]
    [client.config :refer [use-repl? single-player-only?]]
    [client.pages.play :as play-page]))

(enable-console-print!)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init-single-player []
  (socket/stub-connection!)
  (play-page/init-solo!))

(defn- init-multi-player []
  (socket/connect!)
  (client.routes/init!))

(defn- init! []

  ;; Connect to REPL for development.
  (if use-repl?
    (repl/connect!))

  ;; initialize the socket connection
  (if single-player-only?
    (init-single-player)
    (init-multi-player)))

($ init!)
