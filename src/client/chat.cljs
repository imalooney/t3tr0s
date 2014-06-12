(ns client.chat
  (:require-macros [hiccups.core :as hiccups])
  (:require
    hiccups.runtime
    [client.socket :refer [socket]]
    [client.login :refer [get-username
                          get-color]]
    [cljs.reader :refer [read-string]]
    [client.util :as util]))

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml chat-html []
  [:div#inner-container
    [:div.chat-logo-e38e3
      [:img {:src "/img/t3tr0s_logo_200w.png" :width "160px"}]
      [:span.span-4e536.time-left-8a651 "Waiting to play..."]]
    [:div#chat-messages]
    [:div#chat-input
      [:input#msg {:type "text" :placeholder "Type to chat..."}]
      [:input#submit.red-btn-2c9ab {:type "submit" :value "Send"}]]])

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


; alias the jquery variable
(def $ js/$)

;;------------------------------------------------------------
;; Chat
;;-----------------------------------------------------------

(defn get-message
  "Gets the latest message in the input field"
  []
  (.val ($ "#msg")))

(defn clear-message!
  "Clear the current message in the input field"
  []
  (.val ($ "#msg") ""))

(defn send-message!
  "Sends a message to the chat"
  []
  (.emit @socket "chat-message" (get-message)))

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
  (js/console.log seconds)
    (js/console.log (util/seconds->time-str seconds))
  (.html ($ ".time-left-8a651")
    (cond
      (pos? seconds) (str "Time Until Next Game: " (util/seconds->time-str seconds))
      (zero? seconds) "Waiting to play...")))

(defn on-start-game
  "Called when we receive the go-ahead from the server to start the game."
  []

  ; Navigate to the battle page.
  (aset js/location "hash" "#/battle-game")

  )
;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  "Starts the chat page"
  []

  (client.core/set-bw-background!)

  (.html ($ "#main-container") (chat-html))

  ;; Add listeners
  (.click ($ "#submit") submit-message!)
  (.keyup ($ "#msg") #(if (= (.-keyCode %) 13) (submit-message!)))

  ;; Join the "lobby" room.
  (.emit @socket "join-lobby")

  ;; Listen to chat updates.
  (.on @socket "new-message" on-new-message)

  (.on @socket "time-left" on-time-left)

  (.on @socket "start-game" on-start-game)

  )

(defn cleanup
  []

  ;; Leave the "lobby" room.
  (.emit @socket "leave-lobby")

  ;; Ignore chat updates.
  (.removeListener @socket "new-message" on-new-message)

  ;; Ignore start game message.
  (.removeListener @socket "start-game" on-start-game)

  (.removeListener @socket "time-left" on-time-left)

  )
