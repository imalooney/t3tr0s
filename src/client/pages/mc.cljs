(ns client.pages.mc
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    hiccups.runtime
    [client.dom :as dom]
    [client.socket :as socket]))

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
    (.val ($ "#mcDurationInput") duration)
    (.val ($ "#mcCooldownInput") cooldown)))

(add-watch game-settings :main on-change-game-settings)

(defn- on-settings-update
  "Update the game settings inputs"
  [new-settings]
  (swap! game-settings merge new-settings))

;;------------------------------------------------------------------------------
;; Stop Game
;;------------------------------------------------------------------------------

(hiccups/defhtml stop-html []
  [:div.mc-wrapper-6af28
    [:div#mcTimeLeft.timeleft-69be1]
    [:button#mcStopBtn.red-btn-2c9ab "Stop"]])

(defn on-time-left
  "Called when receiving time left from server."
  [i]
  (.html ($ "#mcTimeLeft") (str "Stopping in " i)))

(defn on-countdown
  "Called when receiving countdown till start from server."
  [i]
  (.html ($ "#mcTimeLeft") (str "Starting in " i)))

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
  (dom/set-panel-body! 2 (stop-html))
  (dom/animate-to-panel 2)

  (socket/on "time-left" on-time-left)
  (socket/on "countdown" on-countdown)

  (.click ($ "#mcStopBtn") on-click-stop-btn))

;;------------------------------------------------------------------------------
;; Start Game
;;------------------------------------------------------------------------------

(hiccups/defhtml start-html []
  [:div.mc-wrapper-6af28
    [:div#mcTimeLeft.timeleft-69be1]
    [:label.mc-label-382e6 "Round duration:"]
    [:input#mcDurationInput.mc-input-45faf
      {:type "text"
       :value (:duration @game-settings)}]
    [:label.mc-label-382e6 "Time between rounds:"]
    [:input#mcCooldownInput.mc-input-45faf
      {:type "text"
       :value (:cooldown @game-settings)}]
    [:div
      [:button#mcStartNowBtn.green-btn-f67eb "Start Now"]
      [:button#mcUpdateTimesBtn.blue-btn-41e23 "Update Times"]]])

(defn- get-times
  "Retrieve the time settings inputs"
  []
  {:duration (int (.val ($ "#mcDurationInput")))
   :cooldown (int (.val ($ "#mcCooldownInput")))})

(defn- click-start-btn []
  (socket/emit "start-time")
  (init-stop-page!))

(defn- click-update-times-btn []
  (socket/emit "update-times" (get-times)))

(defn init-start-page!
  "Initialize the start game page."
  []
  (dom/set-panel-body! 2 (start-html))
  (dom/animate-to-panel 2)
  (.click ($ "#mcStartNowBtn") click-start-btn)
  (.click ($ "#mcUpdateTimesBtn") click-update-times-btn))

;;------------------------------------------------------------------------------
;; Password
;;------------------------------------------------------------------------------

(hiccups/defhtml password-html []
  [:div.mc-wrapper-6af28
    [:form#mcPasswordForm
      [:input#mcPasswordInput.password-input-e6204
        {:type "password" :placeholder "MC password"}]
      [:button#mcSubmitPasswordBtn.red-btn-2c9ab "OK"]]])

(defn- on-submit-password-form [js-evt]
  (.preventDefault js-evt)
  (socket/emit "request-mc" (.val ($ "#mcPasswordInput"))))

(defn- on-grant-mc
  "Callback for handling the MC access grant."
  [str-data]
  (if-let [game-running (read-string str-data)]
    (init-stop-page!)
    (init-start-page!)))

;;------------------------------------------------------------------------------
;; Page Init / Cleanup
;;------------------------------------------------------------------------------

(defn init!
  []
  (dom/set-bw-background!)
  (dom/set-panel-body! 1 (password-html))
  (dom/animate-to-panel 1)

  ;; Request access as MC when user submits password.
  (.on ($ "#mcPasswordForm") "submit" on-submit-password-form)

  ;; Render either the stop page or the start page
  ;; when access as MC is granted.
  (socket/on "grant-mc" on-grant-mc)

  ;; Listen for any settings updates
  (socket/on "settings-update" #(on-settings-update (read-string %)))

  ;; Put focus on the password field.
  (.focus (dom/by-id "mcPasswordInput")))

(defn cleanup!
  []
  ; Leave the MC role.
  (socket/emit "leave-mc")

  ; Destroy socket listeners.
  (socket/removeListener "grant-mc")
  (socket/removeListener "settings-update")

  (cleanup-stop-page!))