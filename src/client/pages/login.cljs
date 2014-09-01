(ns client.pages.login
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.dom :as dom]
    client.html
    [client.socket :as socket]
    client.state
    [client.util :as util]
    hiccups.runtime))

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml login-form []
  [:form#loginForm
    [:input#nameInput
      {:maxlength 15
       :placeholder "Enter your name..."
       :type "text"}]
    [:button#playBtn.red-btn-2c9ab "Play"]])

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

(defn- add-events! []
  (.on ($ "#loginForm") "submit" on-form-submit))

;;------------------------------------------------------------------------------
;; Page Initialization
;;------------------------------------------------------------------------------

(defn init! []
  (dom/set-color-background!)
  (dom/animate-to-panel 1)

  ;; NOTE: this should never really happen, but it's a safeguard
  (if-not (dom/by-id "menuInner")
    (dom/set-panel-body! 1 (client.html/menu)))

  (dom/set-html! "menuInner" (login-form))
  (add-events!)

  ;; Put focus on username field.
  (.focus (dom/by-id "nameInput")))