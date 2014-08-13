(ns client.core
  (:require
    client.dashboard
    client.menu
    client.game
    client.chat
    client.mc
    [client.dom :as dom]
    [client.login :refer [send-login!]]
    [client.repl :as repl]
    [client.socket :as socket]
    [client.util :as util]))

(enable-console-print!)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; URL routing
;;------------------------------------------------------------------------------

(def previous-hash (atom nil))

(def pages {
  "#/login" {:init client.login/init :cleanup client.login/cleanup}
  "#/lobby" {:init client.chat/init  :cleanup client.chat/cleanup}
  "#/menu"  {:init client.menu/init  :cleanup client.menu/cleanup}
  "#/mc"    {:init client.mc/init    :cleanup client.mc/cleanup}
  "#/solo-game"   {:init client.game/init-solo   :cleanup client.game/cleanup}
  "#/battle-game" {:init client.game/init-battle :cleanup client.game/cleanup}
  "#/dashboard"   {:init client.dashboard/init :cleanup client.dashboard/cleanup}

  ;; TODO: not finished yet
  "#/game2" {:init client.game/init-battle2 :cleanup client.game/cleanup}})

(defn- dispatch-hash!
  "Call the appropriate function for the given URL hash."
  [h]

  ; Cleanup previous page if possible.
  (if-let [cleanup (-> @previous-hash pages :cleanup)]
    (cleanup))
  (reset! previous-hash h)

  (if (get #{"" "#/"} h)

    ; Redirect blank hash to menu page.
    (aset js/location "hash" "#/menu")

    ; Initialize the new page if possible.
    (if-let [init (-> h pages :init)]
      (init)
      (js/console.error "no page called" h))))

(defn- enable-hash-routing!
  "Monitor the URL hash to auto-dispatch our pages."
  []
  (aset js/window "onhashchange" #(dispatch-hash! (aget js/location "hash"))))

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init []

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
  (dom/init!)

  ;; Add custom hash routing.
  (enable-hash-routing!)

  ;; We have to dispatch the initial hash
  ;; because only changes are auto-dispatched.
  (dispatch-hash! (aget js/location "hash")))

($ init)