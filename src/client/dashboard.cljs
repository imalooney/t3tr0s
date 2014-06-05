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
    [:h2.time-left-eb709 "Time Left: 2:32"]
    [:div#joe "joe board"]
    [:div#james "james board"]
    [:div#jenny "jenny board"]
    [:div#jill "jill board"]])

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

(defn- add-events []
  ;; TODO: write me
  nil)

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

(defn init []
  (.html ($ "#main-container") (dashboard-html))
  (add-events))

(defn cleanup
  []
  nil)
