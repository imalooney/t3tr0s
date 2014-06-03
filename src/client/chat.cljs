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
   [:div#chat]
   [:input#msg {:type "text"}]
   [:input#submit {:type "submit" :value "Send"}]])
;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  []

  ; Initialize page content
  (.html ($ "#main-container") (chat-html))
  (client.chat.core/init)

  )
