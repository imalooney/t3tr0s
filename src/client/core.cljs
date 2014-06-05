(ns client.core
  (:require
    client.dashboard
    client.menu
    client.game
    client.chat
    client.mc
    [client.login :refer [send-login!]]
    [client.repl :as repl]
    [client.socket :refer [socket connect-socket!]]))

(enable-console-print!)

;;------------------------------------------------------------
;; URL routing
;;------------------------------------------------------------

(def previous-hash (atom nil))

(def pages {"#/login" {:init client.login/init :cleanup client.login/cleanup}
            "#/lobby" {:init client.chat/init :cleanup client.chat/cleanup}
            "#/menu"  {:init client.menu/init :cleanup client.menu/cleanup}
            "#/mc"    {:init    client.mc/init :cleanup client.mc/cleanup}
            "#/solo-game"   {:init #(do
                                      (reset! client.game.core/battle false)
                                      (client.game/init))
                             :cleanup client.game/cleanup}
            "#/battle-game" {:init #(do
                                      (reset! client.game.core/battle true)
                                      (client.game/init))
                             :cleanup client.game/cleanup}
            "#/dashboard" {:init client.dashboard/init
                           :cleanup client.dashboard/cleanup}})

(defn dispatch-hash!
  "Call the appropriate function for the given URL hash."
  [h]

  ; Cleanup previous page if possible.
  (if-let [cleanup (-> @previous-hash pages :cleanup)]
    (cleanup))
  (reset! previous-hash h)

  (if (get #{"" "#/"} h)

    ; Redirect blank hash to login page.
    (aset js/location "hash" "#/login")

    ; Initialize the new page if possible.
    (if-let [init (-> h pages :init)]
      (init)
      (js/console.error "no page called" h))))

(defn enable-hash-routing!
  "Monitor the URL hash to auto-dispatch our pages."
  []
  (aset js/window "onhashchange" #(dispatch-hash! (.-hash js/location))))

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init []

  ; Connect to REPL for development.
  (repl/connect)

  ; Make connection to server.
  (connect-socket!)

  ; Send user information to server, and again when requested.
  (send-login!)
  (.on @socket "request-name" send-login!)

  ; Add custom hash routing.
  (enable-hash-routing!)

  ; We have to dispatch the initial hash
  ; because only changes are auto-dispatched.
  (dispatch-hash! (.-hash js/location))

  )

(.addEventListener js/window "load" init)
