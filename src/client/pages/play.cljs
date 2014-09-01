(ns client.pages.play
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    hiccups.runtime
    client.game.core
    client.state
    [client.dom :as dom]
    [client.socket :as socket]
    [client.util :as util]))

(declare cleanup!)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; Initialization flag
;;------------------------------------------------------------------------------

(def game-started? (atom false))

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml keys-legend [battle-mode?]
  [:div.legend-4d59b
    [:div
      [:span.key-6221e {:style "margin-right: 6px"} "&#9664;"]
      [:span.key-6221e "&#9654;"]]
    [:div.label-29ee4 "Left / Right"]
    [:div
      [:span.key-6221e "&#9660;"]]
    [:div.label-29ee4 "Down"]
    [:div
      [:span.key-6221e "&#9650;"]]
    [:div.label-29ee4 "Rotate Piece"]
    [:div
      [:span.key-af1cf {:style "width:70px"} "Space"]]
    [:div.label-29ee4 "Hard Drop"]
    (if-not battle-mode?
      (hiccups/html
        [:div [:span.key-af1cf {:style "font-family:arial"} "P"]]
        [:div.label-29ee4 "Pause game"]))
    [:div
      [:span.key-af1cf {:style "font-family:arial"} "M"]]
    [:div.label-29ee4 "Music on / off"]
    [:div
      [:span.num-0a5cb "0"]
      [:span.dash-82fa7 "&ndash;"]
      [:span.num-0a5cb "9"]]
    [:div.label-29ee4 "Change Theme"]])

(hiccups/defhtml next-piece-and-stats [battle-mode?]
  [:div.stats-daea8
    [:div.label-39b9c "Next"]
    [:canvas#nextPieceCanvas.next-2f9f7]
    [:div.label-39b9c "Score"]
    [:div#gameScreenScore.metric-b93a8]
    [:div.line-8975a]
    [:div.label-39b9c "Lines"]
    [:div#gameScreenLines.metric-b93a8]
    [:div.line-8975a]
    (if battle-mode?
      (hiccups/html
        [:div.label-39b9c "Time Left"]
        [:div#gameScreenTimeLeft.metric-b93a8])
      (hiccups/html
        [:div.label-39b9c "Level"]
        [:div#gameScreenLevel.metric-b93a8]))])

(hiccups/defhtml battle-header []
  [:div.hdr-93a4f
    [:a {:href "#/menu"}
      [:img.logo-dd80d {:src "/img/t3tr0s_logo_200w.png" :alt "T3TR0S Logo"}]]
    [:h1.title-6637f "Battle"]
    [:a.spectate-link-02d2e {:href "#/spectate" :target "_blank"}
      "Spectate"]])

(hiccups/defhtml solo-header []
  [:div.hdr-93a4f
    [:a {:href "#/menu"}
      [:img.logo-dd80d {:src "/img/t3tr0s_logo_200w.png" :alt "T3TR0S Logo"}]]
    [:h1.title-6637f "Solo"]])

(hiccups/defhtml game-html [battle-mode?]
  (if battle-mode?
    (battle-header)
    (solo-header))
  [:div.wrapper-4b797
    [:div.game-0a564
      [:canvas#mainGameCanvas.canvas-eb427]
      [:div#themeYear.year-050bf]
      [:div#themePlatform.platform-2952d]]
    (next-piece-and-stats battle-mode?)
    (keys-legend battle-mode?)
    [:audio#music {:src "audio/theme.mp3" :preload "none" :loop "loop"}
      "Your browser does not support audio."]
    [:div.history-6ce78 (if battle-mode? {:style "display:none"})
      [:canvas#historyCanvas.history-canvas-0d569]]
    ;; TODO: opponent boards go here
    ])

(hiccups/defhtml countdown-html []
  [:div.countdown-container-22a7f
    [:div#getReadyMsg.ready-msg-e333a "Get" [:br] "Ready!"]
    [:div#countdownNum.countdown-num-68cfb]])

(hiccups/defhtml gameover-html [ranks]
  [:div.hdr-93a4f
    [:a {:href "#/menu"}
      [:img.logo-dd80d {:src "/img/t3tr0s_logo_200w.png" :alt "T3TR0S Logo"}]]
    [:h1.title-6637f "Game over"]]
  [:div.results-1b4f3
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
            [:td (str (+ i 1))]
            [:td {:class (str "color-" (mod i 7))} (:user player)]
            [:td (util/format-number (:score player))]
            [:td (:total-lines player)]])]]]
  [:div.wrapper-0a3ca
    [:button#gameOverBtn.red-btn-2c9ab "Lobby"]])

(defn start-battle!
  "Show and start the game."
  []
  (dom/set-panel-body! 3 (game-html true))
  (client.game.core/init)
  (reset! game-started? true))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn on-countdown
  "Called when the countdown message is received from server."
  [i]
  (if (> i 0)
    (do
      (dom/hide-el! "getReadyMsg")
      (.html ($ "#countdownNum") i))
    (start-battle!)))

(defn- click-game-over-btn []
  (aset js/document "location" "hash" "/lobby"))

(defn on-game-over
  "Called when game over message received from server."
  [str-data]
  (cleanup!)
  (let [data (read-string str-data)]
    (dom/set-panel-body! 4 (gameover-html data))
    (.on ($ "#gameOverBtn") "click" click-game-over-btn)
    (dom/animate-to-panel 4)))

(defn on-time-left
  "Called when server sends a time-left update."
  [seconds-left]

  (.html ($ "#gameScreenTimeLeft") (util/seconds->time-str seconds-left))

  ;; Use this as a mechanism for starting the game
  ;; if the players join late.  Otherwise, they
  ;; would never leave the countdown screen.
  (if-not @game-started?
    (start-battle!)))

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

(defn- init-battle2 []
  (reset! client.game.core/battle true)
  (reset! game-started? false)

  ;; join the "game" room to receive game-related messages
  (socket/emit "join-game")
  (socket/on "game-over" on-game-over)
  (socket/on "time-left" on-time-left)
  (socket/on "countdown" on-countdown)

  (dom/set-bw-background!)
  (dom/set-panel-body! 3 (countdown-html))
  (dom/animate-to-panel 3))

(defn init-battle! []
  ;; user should not be able to see this page unless they have set their username
  (if-not @client.state/username
    (aset js/document "location" "hash" "/menu")
    (init-battle2)))

(defn init-solo! []
  (reset! client.game.core/battle false)
  (dom/set-panel-body! 2 (game-html false))
  (dom/set-bw-background!)
  (client.game.core/init)
  (reset! game-started? true)
  (dom/animate-to-panel 2))

(defn cleanup!
  []
  ;; Leave the game room.
  (socket/emit "leave-game")

  ;; Shutdown the game facilities.
  (client.game.core/cleanup)

  ;; Ignore the game messages.
  (socket/removeListener "game-over")
  (socket/removeListener "time-left")
  (socket/removeListener "countdown"))