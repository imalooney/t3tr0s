(ns client.core
  (:require
    client.dom
    [client.pages.login :refer [send-login!]]
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

  ;; send the username if we have it
  (if-let [username (aget js/localStorage "username")]
    (socket/send-username username))

  ;; Send user information to server, and again when requested.
  (send-login!)
  (socket/on "request-name" send-login!)

  ;; initialize the DOM
  (client.dom/init!)

  ;; init routing
  (client.routes/init!))

($ init!)