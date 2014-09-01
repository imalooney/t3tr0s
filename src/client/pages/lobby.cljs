(ns client.pages.lobby
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    hiccups.runtime
    [client.dom :as dom]
    [client.socket :as socket]
    client.state
    [client.util :as util]))

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml chat-html []
  [:div.hdr-93a4f
    [:a {:href "#/menu"}
      [:img.logo-dd80d {:src "/img/t3tr0s_logo_200w.png" :alt "T3TR0S Logo"}]]
    [:h1#lobbyTimeLeft.title-6637f "Waiting..."]]
  [:div.wrapper-4b797
    [:div.chat-wrapper-ac7fd
      [:div.title-87c6c [:i.fa.fa-comments]]
      [:div#chatMessages.chat-messages-5de97]
      [:div.chat-input-30e3b
        [:input#chatInput.input-6ab0c
          {:type "text"
           :placeholder "Type message and press enter"}]]]
    [:div.players-wrapper-0624c
      [:div.title-87c6c [:i.fa.fa-user]]
      [:div#playersList.players-b766b]]])

(hiccups/defhtml chat-msg-html
  [{:keys [user color msg]}]
  [:div.message-6ab4b
    [:div.user-eb71d (hiccups.runtime/escape-html user)]
    [:div.text-d43c1 (hiccups.runtime/escape-html msg)]])

(hiccups/defhtml chat-join-html
  [{:keys [user color]}]
  [:div.message-6ab4b
    [:div.joined-3a6ea (str user " joined the lobby")]])

(hiccups/defhtml chat-leave-html
  [{:keys [user color]}]
  [:div.message-6ab4b
    [:div.left-bcb89 (str user " left the lobby")]])

(hiccups/defhtml player-name [p]
  [:div.player-name-f93cf (:user p)])

(hiccups/defhtml player-list [players]
  (map player-name players))

;;------------------------------------------------------------------------------
;; Chat
;;------------------------------------------------------------------------------

(defn get-message
  "Gets the latest message in the input field"
  []
  (.val ($ "#chatInput")))

(defn clear-message!
  "Clear the current message in the input field"
  []
  (.val ($ "#chatInput") ""))

(defn send-message!
  "Sends a message to the chat"
  []
  (socket/emit "chat-message" (get-message)))

(defn add-message!
  [msg]
  (if-let [html-fn (get {"join"  chat-join-html
                         "leave" chat-leave-html
                         "msg"   chat-msg-html}
                     (:type msg))]
    (.append ($ "#chatMessages") (html-fn msg))))

(defn scroll-to-bottom! []
  (.scrollTop ($ "#chatMessages") 99999))

(defn submit-message!
  "adds a message, sends it, removes it and scrolls the chat area"
  []
  (let [msg (get-message)]
    (when-not (= msg "")
      (add-message! {:type "msg"
                     :user @client.state/username
                     :color client.state/chat-color
                     :msg msg})
      (send-message!)
      (clear-message!)
      (scroll-to-bottom!))))

(defn on-new-message
  "Called when we receive a chat message from the server."
  [data]
  (add-message! (read-string data))
  (scroll-to-bottom!))

(defn- on-time-left
  "Called when server sends a time-left update."
  [seconds]
  (.html ($ "#lobbyTimeLeft")
    (cond
      (pos? seconds) (str "Next Game in " (util/seconds->time-str seconds))
      (zero? seconds) "Waiting...")))

(defn on-start-game
  "Called when we receive the go-ahead from the server to start the game."
  []
  (aset js/document "location" "hash" "/battle"))

(defn on-players-update
  "Called when the player information is updated."
  [data-str]
  (let [players (read-string data-str)
        sorted-players (sort-by :user players)]
    (.html ($ "#playersList") (player-list sorted-players))))

(defn on-keydown-chat-input [js-evt]
  (let [key-code (aget js-evt "keyCode")]
    (if (= key-code 13)
      (js/setTimeout submit-message! 1))))

(defn on-click-messages-container []
  (.focus (dom/by-id "chatInput")))

(defn- add-events []
  (.on ($ "#chatMessages") "click" on-click-messages-container)
  (.on ($ "#chatInput") "keydown" on-keydown-chat-input))

;;------------------------------------------------------------------------------
;; Page Init / Cleanup
;;------------------------------------------------------------------------------

(def socket-events
  [["new-message" on-new-message]
   ["time-left" on-time-left]
   ["start-game" on-start-game]
   ["players-update" on-players-update]])

(defn- init2! []
  (dom/set-bw-background!)
  (dom/set-html! "panel2" (chat-html))
  (add-events)
  (dom/animate-to-panel 2)

  ;; Join the "lobby" room.
  (socket/emit "join-lobby")

  ;; Listen to chat updates.
  (doseq [[event-name handler] socket-events]
    (socket/on event-name handler)))

(defn init! []
  ;; user should not be able to see this page unless they have set their username
  (if-not @client.state/username
    (aset js/document "location" "hash" "/menu")
    (init2!)))

(defn cleanup!
  []
  ;; Leave the "lobby" room.
  (socket/emit "leave-lobby")

  ;; stop listening for updates
  (doseq [[event-name _] socket-events]
    (socket/removeListener event-name)))