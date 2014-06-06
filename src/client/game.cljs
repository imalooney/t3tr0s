(ns client.game
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    hiccups.runtime
    [client.socket :refer [socket]]
    client.game.core
    [client.util :as util]))

(def $ js/$)

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
        [:div.clr-22ff3]]
      [:div.change-theme-6bd50 "Press keys 0-9 to change your theme"]
      [:div#theme]
      [:div#theme-details]]
      ; [:div#theme "1984"]
      ; [:div#theme-details "Electronika 60"]]
    [:section#arena]])

(hiccups/defhtml countdown-html []
  [:h1#countdown "Connecting..."])

(hiccups/defhtml gameover-html [ranks]
  [:div#inner-container
   [:div.chat-logo-e38e3
    [:img {:src "/../../img/t3tr0s_logo_200w.png" :width "160px"}]
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

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init-game
  "Show and start the game."
  []
  (.html ($ "#main-container") (game-html))
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
  (.html ($ "#main-container") (countdown-html))
  (.on @socket "countdown" on-countdown)
  )

(declare cleanup)

(defn on-game-over
  "Called when game over message received from server."
  [str-data]
  (js/console.log "game over")
  (cleanup)
  (let [data (read-string str-data)]
    (.html ($ "#main-container") (gameover-html data))
    (.click ($ "#game-over-btn") #(aset js/location "hash" "#/lobby"))))

(defn on-time-left
  "Called when server sends a time-left update."
  [s]
  (js/console.log "time left:" s)

  ; Use this as a mechanism for starting the game
  ; if the players join late.  Otherwise, they
  ; would never leave the countdown screen.
  (if-not @initialized
    (init-game))
  )

(defn init
  []

  (reset! initialized false)

  (client.core/set-bw-background!)

  ; Only show the countdown if we are in a battle game
  ; so we can wait for the game to start.
  (if @client.game.core/battle
    (do
      ; Join the "game" room to receive game-related messages.
      (.emit @socket "join-game")
      (.on @socket "game-over" on-game-over)
      (.on @socket "time-left" on-time-left)
      (init-countdown))
    (init-game))

  )

(defn cleanup
  []

  ; Leave the game room.
  (.emit @socket "leave-game")

  ; Shutdown the game facilities.
  (client.game.core/cleanup)

  ; Ignore the game messages.
  (.removeListener @socket "game-over" on-game-over)
  (.removeListener @socket "time-left" on-time-left)
  (.removeListener @socket "countdown" on-countdown)

  )

