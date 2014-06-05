(ns client.mc
	(:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    [client.socket :refer [socket]]
		hiccups.runtime))

(def $ js/$)

;;------------------------------------------------------------
;; Score Monitor page
;;------------------------------------------------------------

(hiccups/defhtml score-html []
	[:div#inner-container
   [:div#chat
    [:div#chat-messages]]])

(hiccups/defhtml score-line-html
  [{:keys [rank user color score]}]
  [:p.message
   [:span.txt rank]
   [:span#user {:class (str "color-" color)} user]
   [:span.txt score]])

(defn on-score-update
  "Callback for handling the score update."
  [str-data]
  (let [scores (read-string str-data)]
    (.html ($ "#chat-messages")
           (str (map score-line-html scores)))))

(defn init-score-page!
  "Initialize the score page."
  []
  (.html ($ "#main-container") (score-html))

  ; Update the score table when needed.
  (.on @socket "score-update" on-score-update)
  )

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

  (.click ($ "#submit-start-lines")
          #(do (.emit @socket "start-lines")
               (init-score-page!)))
  (.click ($ "#submit-start-time")
          #(do (.emit @socket "start-time")
               (init-score-page!))))

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
     (init-score-page!)
     (init-start-page!)))

(defn init-password-page!
  "Initialize the password page."
  []
  (.html ($ "#main-container") (password-html))

  ; Request access as MC when user submits password.
  (.click ($ "#submit-pass")
          #(.emit @socket "request-mc"
                  (.val ($ "#password"))))

  ; Render either the score page or the start page
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
  (.removeListener @socket "score-update" on-score-update)

  )
