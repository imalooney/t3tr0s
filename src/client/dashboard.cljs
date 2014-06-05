(ns client.dashboard
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.socket :refer [socket]]
    hiccups.runtime))

(def $ js/jQuery)

;; TODO: move this to a dom namespace
(defn- by-id [id]
  (.getElementById js/document id))

;;------------------------------------------------------------------------------
;; Dashboard State
;;------------------------------------------------------------------------------

(def state (atom nil))

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml dashboard-html []
  [:div.dashboard-0e330
    [:button#btn1 "Test 1"]
    [:button#btn2 "Test 2"]
    [:h2.time-left-eb709 "Time Left: 2:32"]
    [:div.boards-ad07f
      [:div#joe.board-a3a91.place-1 "joe board"]
      [:div#james.board-a3a91.place-2 "james board"]
      [:div#jenny.board-a3a91.place-3 "jenny board"]
      [:div#jill.board-a3a91.place-4 "jill board"]]])

  ; [:div#inner-container.login
  ;   [:div.login-container
  ;     [:form
  ;       [:label "What is your name?"]
  ;       [:input#login.login-name {:type "text"}]
  ;       [:button#submit.lg-btn "OK"]]]])

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

; (defn send-login!
;   "Send the login information to the server."
;   []
;   (.emit @socket "update-name" (pr-str {:user (get-username)
;                                         :color (get-color)})))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- click-test1 []
  (.removeClass ($ "#joe") "place-1")
  (.addClass ($ "#joe") "place-3")
  (.removeClass ($ "#jenny") "place-3")
  (.addClass ($ "#jenny") "place-1")
  )

(defn- click-test2 []
  (.removeClass ($ "#joe") "place-3")
  (.addClass ($ "#joe") "place-1")
  (.removeClass ($ "#jenny") "place-1")
  (.addClass ($ "#jenny") "place-3")
  )

(defn- add-events []
  (.on ($ "#btn1") "click" click-test1)
  (.on ($ "#btn2") "click" click-test2))

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

(defn init []
  (.html ($ "#main-container") (dashboard-html))
  (add-events))

(defn cleanup
  []
  nil)
