(ns client.core
  (:require
    client.login
    client.menu
    client.game
    [client.repl :as repl]
    [client.socket :refer [socket connect-socket!]]))

(enable-console-print!)

;;------------------------------------------------------------
;; URL routing
;;------------------------------------------------------------

(defn dispatch-hash!
  "Call the appropriate function for the given URL hash."
  [h]
  (let [page-funcs {""        #(aset js/location "hash" "#/login")
                    "#/"      #(aset js/location "hash" "#/login")
                    "#/login" client.login/init
                    "#/menu"  client.menu/init
                    "#/game"  client.game/init}]
    (if-let [f (page-funcs h)]
      (f)
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

  ; Add custom hash routing.
  (enable-hash-routing!)

  ; We have to dispatch the initial hash
  ; because only changes are auto-dispatched.
  (dispatch-hash! (.-hash js/location))

  )

(.addEventListener js/window "load" init)
