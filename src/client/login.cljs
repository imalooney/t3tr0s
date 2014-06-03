(ns client.login
	(:require-macros [hiccups.core :as hiccups])
  (:require
		hiccups.runtime))

(def $ js/$)

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml login-html []
	[:div#inner-container.login
		[:div.login-container
			[:label "What is your name?"]
			[:input.login-name {:type "text"}]
			[:button#submit.lg-btn "OK"]]])

;;------------------------------------------------------------
;; Username storage.
;;------------------------------------------------------------

(defn get-username
  "Gets the currently stored username."
  []
  (if-let [username (aget js/localStorage "username")]
    username
    ""))

(defn set-username!
  "Stores the given username."
  [username]
  (aset js/localStorage "username" username))

;;------------------------------------------------------------
;; Events
;;------------------------------------------------------------

(defn on-submit
  "Handle the submit event."
  []
  (let [input (.val ($ ".login-name"))]
    (set-username! input)
    (aset js/location "hash" "#/menu")))

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  []

  ; Initialize page content
  (.html ($ "#main-container") (login-html))

  ; Populate username field.
  (.val ($ ".login-name") (get-username))

  ; Set username on button click.
  (.click ($ "#submit") on-submit)

  )
