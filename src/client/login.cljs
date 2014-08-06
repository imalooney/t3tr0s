(ns client.login
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.dom :as dom]
    [client.socket :as socket]
    [client.util :as util]
    hiccups.runtime))

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml login-html []
  [:div.wrapper-cd25d
    [:img {:src "/img/t3tr0s_logo_850w.png" :alt "T3TR0S Logo"}]
    [:form#loginForm
      [:input#nameInput {:type "text" :placeholder "Enter your name..."}]
      [:button#playBtn.red-btn-2c9ab "Play!"]]
    [:div.clr-22ff3]])

;;------------------------------------------------------------------------------
;; Username storage.
;;------------------------------------------------------------------------------

(defn get-username
  "Gets the currently stored username."
  []
  (if-let [username (aget js/sessionStorage "username")]
    username
    ""))

(defn get-color
  "Gets the currently stored user color."
  []
  (if-let [color (aget js/sessionStorage "color")]
    color
    0))

(defn store-login!
  "Stores the given username and a random color(0-6)"
  [username]
  (aset js/sessionStorage "username" username)
  (aset js/sessionStorage "color" (rand-int 7)))

(defn send-login!
  "Send the login information to the server."
  []
  (socket/emit "update-name" {:user (get-username) :color (get-color)}))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

;; TODO: what to do when they don't input a username? validation?
(defn- on-form-submit [e]
  (.preventDefault e)
  (let [input (.val ($ "#nameInput"))]
    (store-login! input)
    (send-login!)
    (aset js/location "hash" "#/lobby")))

(defn- add-events []
  (.on ($ "#loginForm") "submit" on-form-submit))

;;------------------------------------------------------------------------------
;; Page Initialization
;;------------------------------------------------------------------------------

(defn init []
  (dom/set-color-background!)
  (dom/set-page-body! (login-html))
  (add-events)

  ;; Populate name field if they have a name stored in localStorage.
  (.val ($ "#nameInput") (get-username))

  ;; Put focus on username field.
  (.focus (dom/by-id "nameInput"))
  )

(defn cleanup []
  nil)