(ns client.mc
	(:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    [client.socket :refer [socket]]
		hiccups.runtime))

(def $ js/$)

;;------------------------------------------------------------
;; Stop Game page
;;------------------------------------------------------------

(hiccups/defhtml stop-html []
	[:div#inner-container.login
		[:div.login-container
      [:button#submit-stop-game.lg-btn "Stop"]]])

(declare init-start-page!)

(defn init-stop-page!
  "Initialize the start game page."
  []
  (.html ($ "#main-container") (stop-html))

  (.click ($ "#submit-stop-game")
          #(do (.emit @socket "stop-game")
               (init-start-page!))))

;;------------------------------------------------------------
;; Start Game page
;;------------------------------------------------------------

(hiccups/defhtml start-html []
	[:div#inner-container.login
		[:div.login-container
			[:button#submit-start-lines.lg-btn "Start - 40 lines"]
      [:button#submit-start-time.lg-btn "Start - 5 minutes"]]])

(defn init-start-page!
  "Initialize the start game page."
  []
  (.html ($ "#main-container") (start-html))

  (.click ($ "#submit-start-time")
          #(do (.emit @socket "start-time")
               (init-stop-page!))))

;;------------------------------------------------------------
;; Password page
;;------------------------------------------------------------

(hiccups/defhtml password-html []
	[:div#inner-container.login
		[:div.login-container
			[:label "MC password:"]
			[:input#password.login-name {:type "password"}]
			[:button#submit-pass.lg-btn "OK"]]])

(defn on-grant-mc
  "Callback for handling the MC access grant."
  [str-data]
  (if-let [game-running (read-string str-data)]
     (init-stop-page!)
     (init-start-page!)))

(defn init-password-page!
  "Initialize the password page."
  []
  (.html ($ "#main-container") (password-html))

  ; Request access as MC when user submits password.
  (.click ($ "#submit-pass")
          #(.emit @socket "request-mc"
                  (.val ($ "#password"))))

  ; Render either the stop page or the start page
  ; when access as MC is granted.
  (.on @socket "grant-mc" on-grant-mc))

;;------------------------------------------------------------
;; Main page intializer.
;;------------------------------------------------------------

(defn init
  []
  (init-password-page!)
  )

(defn cleanup
  []

  ; Leave the MC role.
  (.emit @socket "leave-mc")

  ; Destroy socket listeners.
  (.removeListener @socket "grant-mc" on-grant-mc)

  )
