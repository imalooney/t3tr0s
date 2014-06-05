(ns client.login
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.socket :refer [socket]]
    hiccups.runtime))

(def $ js/$)

;; TODO: move this to a dom namespace
(defn- by-id [id]
  (.getElementById js/document id))

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml login-html []
  [:div#inner-container.login
    [:div.login-container
      [:form
        [:label "What is your name?"]
        [:input#login.login-name {:type "text"}]
        [:button#submit.lg-btn "OK"]]]])

;;------------------------------------------------------------------------------
;; Username storage.
;;------------------------------------------------------------------------------

(defn get-username
  "Gets the currently stored username."
  []
  (if-let [username (aget js/localStorage "username")]
    username
    ""))

(defn get-color
  "Gets the currently stored user color."
  []
  (if-let [color (aget js/localStorage "color")]
    color
    0))

(defn store-login!
  "Stores the given username and a random color(0-6)"
  [username]
  (aset js/localStorage "username" username)
  (aset js/localStorage "color" (rand-int 7)))

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

  ; Initialize page content
  (.html ($ "#main-container") (login-html))

  ; Populate username field.
  (.val ($ "#login") (get-username))

  ; Set username on button click.
  (.click ($ "#submit") on-submit)

  ; Put focus on username field.
  (.focus (by-id "login")))

(defn cleanup
  []
  nil)
