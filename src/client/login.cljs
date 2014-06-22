(ns client.login
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.dom :as dom]
    [client.socket :refer [socket]]
    hiccups.runtime))

(def $ js/$)

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml login-html []
  [:div#inner-container
    [:div.logo-31d54]
    [:div.login-5983e
      [:form
        [:div.input-4a3e3
          [:label.label-66a3b "What is your name?"]
          [:input#login.input-48f1f {:type "text"}]]
        [:button#submit.red-btn-2c9ab "OK"]]]])

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
  (.emit @socket "update-name" (pr-str {:user (get-username)
                                        :color (get-color)})))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

;; TODO: what to do when they don't input a username? validation?
(defn on-submit
  "Handle the submit event."
  [e]
  (.preventDefault e)
  (let [input (.val ($ "#login"))]
    (store-login! input)
    (send-login!)
    (aset js/location "hash" "#/menu")))

;;------------------------------------------------------------------------------
;; Page Initialization
;;------------------------------------------------------------------------------

(defn init []

  (client.core/set-color-background!)

  ; Initialize page content
  (.html ($ "#main-container") (login-html))

  ; Populate username field.
  (.val ($ "#login") (get-username))

  ; Set username on button click.
  (.click ($ "#submit") on-submit)

  ; Put focus on username field.
  (.focus (dom/by-id "login")))

(defn cleanup
  []
  nil)
