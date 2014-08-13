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

(defn get-color
  "Gets the currently stored user color."
  []
  (if-let [color (aget js/sessionStorage "color")]
    color
    0))

(defn send-login!
  "Send the login information to the server."
  ([] (send-login! (aget js/localStorage "username")))
  ([username]
    (socket/emit "update-name" {:user username :color (get-color)})))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- on-form-submit [e]
  (.preventDefault e)
  (let [username (dom/get-value "nameInput")]
    ;; TODO: more username validation here
    (when (not= username "")
      (aset js/localStorage "username" username)
      (aset js/sessionStorage "color" (rand-int 7))
      (send-login! username)
      (aset js/location "hash" "#/lobby"))))

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
    (dom/set-value! "nameInput" username))

  ;; Put focus on username field.
  (.focus (dom/by-id "nameInput"))
  )

(defn cleanup []
  nil)