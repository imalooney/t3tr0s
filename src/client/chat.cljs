(ns client.chat
	(:require-macros [hiccups.core :as hiccups])
	(:require
		hiccups.runtime))

(def $ js/$)

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml chat-html []
	[:div#inner-container
   [:div#chat
    [:div#chat-messages]
    [:div#chat-input
     [:input#msg {:type "text" :placeholder "Type to chat"}]
     [:input#submit {:type "submit" :value "Send"}]]]])

(hiccups/defhtml chat-msg-html [user color msg]
  [:p.message
   [:span#user {:class (str "color-" color)}(str user ": ")]
   [:span.txt msg]])

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  []

  ; Initialize page content
  (.html ($ "#main-container") (chat-html))
  (client.chat.core/init)

  )

(defn cleanup
  []
  nil)
