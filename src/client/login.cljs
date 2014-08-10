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

(hiccups/defhtml login-inner []
  [:form#loginForm
    [:input#nameInput {:type "text" :placeholder "Enter your name..."}]
    [:button#playBtn.red-btn-2c9ab "Play"]]
  [:div.clr-22ff3])

(hiccups/defhtml login-html []
  [:div.wrapper-cd25d
    [:img {:src "/img/t3tr0s_logo_850w.png" :alt "T3TR0S Logo"}]
    [:div#menuInnerWrapper
      (login-inner)]])

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

  ;; this just helps prevent the image "flashing" when you transition from the
  ;; menu page to the login page (99% of cases)
  (if (dom/by-id "menuInnerWrapper")
    (dom/set-html! "menuInnerWrapper" (login-inner))
    (dom/set-page-body! (login-html)))

  (add-events)

  ;; Populate name field if they have a name stored in localStorage
  (if-let [username (aget js/localStorage "username")]
    (aset (dom/by-id "nameInput") "value" username))

  ;; Put focus on username field.
  (.focus (dom/by-id "nameInput"))
  )

(defn cleanup []
  nil)