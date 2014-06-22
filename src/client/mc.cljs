(ns client.mc
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    [client.socket :as socket]
    [client.dom :refer [by-id]]
    hiccups.runtime))

(declare init-start-page!)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; MC Atoms
;;------------------------------------------------------------------------------

(def game-settings
  "Current multiplier settings in effect"
  (atom {}))

(defn- on-change-game-settings [_ _ _ new-settings]
  (let [{:keys [duration cooldown]} new-settings]
    (.val ($ "#duration") duration)
    (.val ($ "#cooldown") cooldown)))

(add-watch game-settings :main on-change-game-settings)

(defn- on-settings-update
  "Update the game settings inputs"
  [new-settings]
  (js/console.log "settings updated")
  (swap! game-settings merge new-settings))

;;------------------------------------------------------------------------------
;; Stop Game page
;;------------------------------------------------------------------------------

(hiccups/defhtml stop-html []
  [:div#inner-container
    [:div.login-5983e
      [:label#time-left.timeleft-69be1]
      [:button#stopBtn.red-btn-2c9ab "STOP"]]])

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
  (socket/removeListener "time-left")
  (socket/removeListener "countdown"))

(defn- on-click-stop-btn []
  (socket/emit "stop-game")
  (cleanup-stop-page!)
  (init-start-page!))

(defn init-stop-page!
  "Initialize the start game page."
  []
  (.html ($ "#main-container") (stop-html))

  (socket/on "time-left" on-time-left)
  (socket/on "countdown" on-countdown)

  (.click ($ "#stopBtn") on-click-stop-btn))

;;------------------------------------------------------------------------------
;; Start Game page
;;------------------------------------------------------------------------------

(hiccups/defhtml start-html []
  [:div#inner-container
    [:div.login-5983e
      [:label#time-left.timeleft-69be1]
      [:div.input-container-c8147
        [:div.input-4a3e3
          [:label.label-66a3b "Round duration:"]
          [:input#duration.input-48f1f {:type "text" :value (:duration @game-settings)}]]
        [:div.input-4a3e3
          [:label.label-66a3b "Time between rounds:"]
          [:input#cooldown.input-48f1f {:type "text" :value (:cooldown @game-settings)}]]
      [:div.button-container-8e52e
        [:button#startBtn.green-btn-f67eb "START NOW"]
        [:button#updateTimes.blue-btn-41e23 "UPDATE TIMES"]]]]])

(defn- get-times
  "Retrieve the time settings inputs"
  []
  {:duration (js/parseInt (.val ($ "#duration")) 10)
   :cooldown (js/parseInt (.val ($ "#cooldown")) 10)})

(defn- click-start-btn []
  (socket/emit "start-time")
  (init-stop-page!))

(defn- click-update-times-btn []
  (socket/emit "update-times" (get-times)))

(defn init-start-page!
  "Initialize the start game page."
  []
  (.html ($ "#main-container") (start-html))
  (.click ($ "#startBtn") click-start-btn)
  (.click ($ "#updateTimes") click-update-times-btn))

;;------------------------------------------------------------------------------
;; Password page
;;------------------------------------------------------------------------------

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
  (socket/emit "request-mc" (.val ($ "#password"))))

(defn- on-grant-mc
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
  (socket/on "grant-mc" on-grant-mc)

  ; Put focus on the password field.
  (.focus (by-id "password")))

;;------------------------------------------------------------------------------
;; Page Init / Cleanup
;;------------------------------------------------------------------------------

(defn init
  []
  (client.core/set-bw-background!)
  ; Listen for any settings updates
  (socket/on "settings-update" #(on-settings-update (read-string %)))

  (init-password-page!))

(defn cleanup
  []
  ; Leave the MC role.
  (socket/emit "leave-mc")

  ; Destroy socket listeners.
  (socket/removeListener "grant-mc")
  (socket/removeListener "settings-update")

  (cleanup-stop-page!))