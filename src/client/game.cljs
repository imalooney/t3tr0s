(ns client.game
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    hiccups.runtime
    client.game.core
    [client.dom :as dom]
    [client.socket :as socket]
    [client.util :as util]))

(def $ js/jQuery)

;;------------------------------------------------------------
;; Initialization flag
;;------------------------------------------------------------

(def initialized (atom false))

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml game-html []
  [:div.top-62e29
    [:img {:src "/img/t3tr0s_logo_200w.png" :alt ""}]]
  [:div.player-view
    [:div.wrap-3b65f
      [:canvas#game-canvas]]
    [:div.right-1d870
      [:div.scoreboard-d49ce
        [:div.left-5c06b
          [:div.next-df9e7 "Next"]
          [:canvas#next-canvas]]
        [:div.right-6aa51
          [:div#score.push-down-e2e2a]
          [:div#level.push-down-e2e2a]
          [:div#lines]]
        [:div.clr-22ff3]
       [:div.time-left-1369c]]
      [:canvas#history-canvas]
      [:div.change-theme-6bd50 "Press keys 0-9 to change your theme"]
      [:audio#music {:src "audio/theme.mp3" :preload "none" :loop "loop"}]
      [:div#theme]
      [:div#theme-details]]
      ; [:div#theme "1984"]
      ; [:div#theme-details "Electronika 60"]]
    [:section#arena]])

(hiccups/defhtml countdown-html []
  [:h1#countdown "Connecting..."])

(hiccups/defhtml gameover-html [ranks]
  [:div.inner-6ae9d
    [:div.chat-logo-e38e3
      [:img {:src "/img/t3tr0s_logo_200w.png" :width "160px"}]
      [:span.span-4e536 "Game over"]]
    [:div#chat-messages
      [:table.table-9be14
        [:thead
          [:tr
            [:th.th-147ad "place"]
            [:th "name"]
            [:th "score"]
            [:th "lines"]]]
        [:tbody
          (for [[i player] (map-indexed vector ranks)]
            [:tr.tr-cf247
              [:td (str (+ i 1) ".")]
              [:td {:class (str "color-" (mod i 7))} (:user player)]
              [:td (util/format-number (:score player))]
              [:td (:total-lines player)]])]]]
    [:button#game-over-btn.red-btn-2c9ab "LOBBY"]])

(defn init-game
  "Show and start the game."
  []
  (dom/set-page-body! (game-html))
  (client.game.core/init)
  (reset! initialized true)
  )

(defn on-countdown
  "Called when the countdown message is received from server."
  [i]
  (if (> i 0)
    (.html ($ "#countdown") i)
    (init-game))
  )

(defn init-countdown
  "Show and listen for the countdown messages."
  []
  (dom/set-page-body! (countdown-html))
  (socket/on "countdown" on-countdown))

(declare cleanup)

(defn on-game-over
  "Called when game over message received from server."
  [str-data]
  (js/console.log "game over")
  (cleanup)
  (let [data (read-string str-data)]
    (dom/set-page-body! (gameover-html data))
    (.click ($ "#game-over-btn") #(aset js/location "hash" "#/lobby"))))

(defn on-time-left
  "Called when server sends a time-left update."
  [total-seconds]
  (.html ($ ".time-left-1369c") (str "Time Left: " (util/seconds->time-str total-seconds)))

  ; Use this as a mechanism for starting the game
  ; if the players join late.  Otherwise, they
  ; would never leave the countdown screen.
  (if-not @initialized
    (init-game))

  )

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

(defn- init
  []
  (reset! initialized false)
  (dom/set-bw-background!)

  ; Only show the countdown if we are in a battle game
  ; so we can wait for the game to start.
  (if @client.game.core/battle
    (do
      ; Join the "game" room to receive game-related messages.
      (socket/emit "join-game")
      (socket/on "game-over" on-game-over)
      (socket/on "time-left" on-time-left)
      (init-countdown))
    (init-game)))

(defn init-solo []
  (reset! client.game.core/battle false)
  (init))

(defn init-battle []
  (reset! client.game.core/battle true)
  (init))

(defn cleanup
  []

  ; Leave the game room.
  (socket/emit "leave-game")

  ; Shutdown the game facilities.
  (client.game.core/cleanup)

  ; Ignore the game messages.
  (socket/removeListener "game-over")
  (socket/removeListener "time-left")
  (socket/removeListener "countdown"))
