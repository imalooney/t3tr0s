(ns client.mc
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    [client.socket :refer [socket]]
    hiccups.runtime))

(def $ js/$)

(defn- by-id [id]
  (.getElementById js/document id))

;;------------------------------------------------------------
;; Stop Game page
;;------------------------------------------------------------

(hiccups/defhtml stop-html []
  [:div#inner-container
    [:div.login-5983e
      [:label#time-left.timeleft-69be1]
      [:button#stopBtn.red-btn-2c9ab "STOP"]]])

(declare init-start-page!)

(defn on-time-left
  "Called when receiving time left from server."
  [i]
  (.html ($ "#time-left") (str "Stopping in " i)))

(defn on-countdown
  "Called when receiving countdown till start from server."
  [i]
  (.html ($ "#time-left") (str "Starting in " i)))

(defn cleanup-stop-page!
  "Remove socket listeners specific to the stop page."
  []
  (.removeListener @socket "time-left" on-time-left)
  (.removeListener @socket "countdown" on-countdown)
  )

(defn init-stop-page!
  "Initialize the start game page."
  []
  (.html ($ "#main-container") (stop-html))

  (.on @socket "time-left" on-time-left)
  (.on @socket "countdown" on-countdown)

  (.click ($ "#stopBtn")
          #(do (.emit @socket "stop-game")
               (cleanup-stop-page!)
               (init-start-page!))))

;;------------------------------------------------------------
;; Start Game page
;;------------------------------------------------------------

(hiccups/defhtml start-html []
  [:div#inner-container
    [:div.login-5983e
      [:label#time-left.timeleft-69be1]
      [:div.input-container-c8147
        [:div.input-4a3e3
          [:label.label-66a3b "Round duration:"]
          [:input#duration.input-48f1f {:type "text"}]]
        [:div.input-4a3e3
          [:label.label-66a3b "Time between rounds:"]
          [:input#cooldown.input-48f1f {:type "text"}]]
      [:div.button-container-8e52e
        [:button#startBtn.green-btn-f67eb "START NOW"]
        [:button#resetTimes.blue-btn-41e23 "RESET TIMES"]]]]])

(defn- get-times
  "Retrieve the time settings inputs"
  []
  { :duration (js/parseInt (.val ($ "#duration")) 10) })

(defn init-start-page!
  "Initialize the start game page."
  []
  (.html ($ "#main-container") (start-html))

  (.click ($ "#startBtn")
          #(do (.emit @socket "start-time")
               (init-stop-page!)))

  (.click ($ "#resetTimes")
          #(do (.emit @socket "reset-times" (pr-str (get-times)))))

  )

;;------------------------------------------------------------
;; Password page
;;------------------------------------------------------------

(hiccups/defhtml password-html []
  [:div#inner-container
    [:div.login-5983e
      [:form
        [:div.input-4a3e3
          [:label.label-66a3b "MC password:"]
          [:input#password.input-48f1f {:type "password"}]]
        [:button#submitPasswordBtn.red-btn-2c9ab "OK"]]]])

(defn- click-login-as-mc [e]
  (.preventDefault e)
  (.emit @socket "request-mc" (.val ($ "#password"))))

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
  (.click ($ "#submitPasswordBtn") click-login-as-mc)

  ; Render either the stop page or the start page
  ; when access as MC is granted.
  (.on @socket "grant-mc" on-grant-mc)

  ; Put focus on the password field.
  (.focus (by-id "password")))

;;------------------------------------------------------------
;; Main page intializer.
;;------------------------------------------------------------

(defn init
  []
  (client.core/set-bw-background!)

  (init-password-page!)
  )

(defn cleanup
  []

  ; Leave the MC role.
  (.emit @socket "leave-mc")

  ; Destroy socket listeners.
  (.removeListener @socket "grant-mc" on-grant-mc)

  (cleanup-stop-page!)

  )
