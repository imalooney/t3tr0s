(ns client.pages.login
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.dom :as dom]
    [client.socket :as socket]
    client.state
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
;; Events
;;------------------------------------------------------------------------------

(defn send-login!
  "Send the login information to the server."
  [username]
    (socket/emit "update-name" {:user username
                                :color client.state/chat-color}))

(defn- on-form-submit [js-evt]
  (.preventDefault js-evt)
  (let [username (dom/get-value "nameInput")]
    ;; TODO: more username validation here
    (when (not= username "")
      (reset! client.state/username username)
      (send-login! username)
      (aset js/location "hash" "#/lobby"))))

(def events-have-been-added? (atom false))

;; this only needs to happen once
(defn- add-events! []
  (when-not @events-have-been-added?
    (.on ($ "#loginForm") "submit" on-form-submit)
    (reset! events-have-been-added? true)))

;;------------------------------------------------------------------------------
;; Page Initialization
;;------------------------------------------------------------------------------

(defn init! []
  (dom/set-color-background!)
  (dom/animate-to-panel 1)
  (dom/hide-el! "menuContainer")
  (dom/show-el! "loginContainer")
  (add-events!)

  ;; Put focus on username field.
  (.focus (dom/by-id "nameInput")))