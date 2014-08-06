(ns client.chat
  (:require-macros [hiccups.core :as hiccups])
  (:require
    hiccups.runtime
    [client.dom :as dom]
    [client.login :refer [get-color]]
    [client.socket :as socket]
    [cljs.reader :refer [read-string]]
    [client.util :as util]))

(def $ js/jQuery)

;; TODO: move this elsewhere, also move (get-color) elsewhere

(defn get-username
  "Gets the currently stored username."
  []
  (if-let [username (aget js/sessionStorage "username")]
    username
    ""))

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml chat-html []
  [:div.inner-6ae9d
    [:div.chat-logo-e38e3
      [:img {:src "/img/t3tr0s_logo_200w.png" :width "160px"}]
      [:span.span-4e536.time-left-8a651 "Waiting to play..."]]
    [:div#chat-and-player-container
      [:div#chat-messages]
      [:div#player-list-container
        [:div.player-list-title-7f811 "Players"]
        [:div#player-list]]]
    [:div.chat-e41e1
      [:input#chatInput {:type "text" :placeholder "Type to chat..."}]
      [:input#sendChatBtn.red-btn-2c9ab {:type "submit" :value "Send"}]
      [:div.clr-22ff3]]])

(hiccups/defhtml chat-msg-html
  [{:keys [user color msg]}]
  [:p.message
    [:span#user {:class (str "color-" color)}(str user ": ")]
    [:span.txt (hiccups.runtime/escape-html msg)]])

(hiccups/defhtml chat-join-html
  [{:keys [user color]}]
  [:p.message
    [:span#user {:class (str "color-" color)}(str user " joined the lobby")]])

(hiccups/defhtml chat-leave-html
  [{:keys [user color]}]
  [:p.message
    [:span#user {:class (str "color-" color)}(str user " left the lobby")]])

(hiccups/defhtml player-name-html
  [{:keys [user color]}]
  [:div {:class (str "player-name-3d2f0 color-" color)} user])

(hiccups/defhtml player-list-html
  [players]
  (map player-name-html players))

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
  (if-let [html (get {"join"  chat-join-html
                      "leave" chat-leave-html
                      "msg"   chat-msg-html}
                     (:type msg))]
    (.append ($ "#chat-messages") (html msg))))

(defn scroll-at-bottom?
  "lets us know if the scroll is all the way to the bottom"
  []
  (let [chat (.getElementById js/document "chat-messages")
        msg-height (.prop (.first ($ ".message")) "offsetHeight")
        chat-visible-height (.-offsetHeight chat)
        chat-total-height (.-scrollHeight chat)
        scroll-position (.-scrollTop chat)]
    (<=  (- chat-total-height chat-visible-height msg-height) scroll-position)))

(defn scroll-chat-area
  "Scrolls the chat area to display the newest message"
  [override]
  (let [chat-area ($ "#chat-messages")
        scroll-config (js-obj "scrollTop" (.prop chat-area "scrollHeight"))]
    (if (or (scroll-at-bottom?) override)
      (.animate chat-area scroll-config))))

(defn submit-message!
  "adds a message, sends it, removes it and scrolls the chat area"
  []
  (let [msg (get-message)]
    (when-not (= msg "")
      (add-message! {:type "msg"
                     :user (get-username)
                     :color (get-color)
                     :msg msg})
      (send-message!)
      (clear-message!)
      (scroll-chat-area true))))

(defn on-new-message
  "Called when we receive a chat message from the server."
  [data]
  (add-message! (read-string data))
  (scroll-chat-area false))

(defn- on-time-left
  "Called when server sends a time-left update."
  [seconds]
  (.html ($ ".time-left-8a651")
    (cond
      (pos? seconds) (str "Time Until Next Game: " (util/seconds->time-str seconds))
      (zero? seconds) "Waiting to play...")))

(defn on-start-game
  "Called when we receive the go-ahead from the server to start the game."
  []

  ; Navigate to the battle page.
  (aset js/location "hash" "#/battle-game"))

(defn- on-players-update
  "Called when the player informatin is updated."
  [data]
  (.html ($ "#player-list") (player-list-html (read-string data))))

;;------------------------------------------------------------------------------
;; Page Init / Cleanup
;;------------------------------------------------------------------------------

(def socket-events
  [["new-message" on-new-message]
   ["time-left" on-time-left]
   ["start-game" on-start-game]
   ["players-update" on-players-update]])

(defn init
  "Starts the chat page"
  []

  (dom/set-bw-background!)
  (dom/set-page-body! (chat-html))

  ;; Add listeners
  (.click ($ "#sendChatBtn") submit-message!)
  (.keyup ($ "#chatInput") #(if (= (.-keyCode %) 13) (submit-message!)))

  ;; Join the "lobby" room.
  (socket/emit "join-lobby")

  ;; Listen to chat updates.
  (doseq [[event-name handler] socket-events]
    (socket/on event-name handler)))

(defn cleanup
  []
  ;; Leave the "lobby" room.
  (socket/emit "leave-lobby")

  ; stop listening for updates
  (doseq [[event-name _] socket-events]
    (socket/removeListener event-name)))