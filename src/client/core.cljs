(ns client.core
  (:require
    client.dashboard
    client.menu
    client.game
    client.chat
    client.mc
    [client.login :refer [send-login!]]
    [client.repl :as repl]
    [client.socket :as socket]
    [client.util :as util]))

(enable-console-print!)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; Client ID
;;------------------------------------------------------------------------------

;; get the client-id from localStorage or create a new one if needed
(def client-id
  (if-let [id (aget js/localStorage "client-id")]
    id
    (util/uuid)))

;; save client-id to localStorage
(aset js/localStorage "client-id" client-id)

;;------------------------------------------------------------------------------
;; Set Background
;;------------------------------------------------------------------------------

(defn set-color-background! []
  (-> ($ "body")
    (.removeClass "bw-e2019")
    (.addClass "color-c025c")))

(defn set-bw-background! []
  (-> ($ "body")
    (.removeClass "color-c025c")
    (.addClass "bw-e2019")))

;;------------------------------------------------------------------------------
;; URL routing
;;------------------------------------------------------------------------------

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
  (aset js/window "onhashchange" #(dispatch-hash! (aget js/location "hash"))))

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init []

  ; Connect to REPL for development.
  (if (aget js/window "T3TR0S_CONFIG" "use-repl")
    (repl/connect!))

  ;; initialize the socket connection
  (socket/connect!)

  ; Send user information to server, and again when requested.
  (send-login!)
  (socket/on "request-name" send-login!)

  ; Add custom hash routing.
  (enable-hash-routing!)

  ; We have to dispatch the initial hash
  ; because only changes are auto-dispatched.
  (dispatch-hash! (aget js/location "hash")))

(.addEventListener js/window "load" init)