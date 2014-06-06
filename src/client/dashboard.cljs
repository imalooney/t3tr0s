(ns client.dashboard
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.socket :refer [socket]]
    hiccups.runtime))

(def $ js/jQuery)

;; TODO: move this to a util namespace
(defn log [& things]
  (js/console.log
    (->> things
         (map pr-str)
         (clojure.string/join " "))))

(defn js-log [thing]
  (.log js/console thing))

;;------------------------------------------------------------------------------
;; Username to UUID Mapping
;;------------------------------------------------------------------------------

;; TODO: move this to a util namespace
;; http://tinyurl.com/lz3bpg6
(defn- uuid []
  (apply
   str
   (map
    (fn [x]
      (if (= x \0)
        (.toString (bit-or (* 16 (.random js/Math)) 0) 16)
        x))
    "00000000-0000-4000-0000-000000000000")))

(def ids 
  "Stores a mapping of username --> UUID for use as DOM ids"
  (atom {}))

(defn create-uuid-if-needed
  "creates a mapping of username --> UUID in the ids atom if one does not already exist"
  [l]
  (if-not (get @ids (:name l))
    (swap! ids (fn [old-ids]
      (assoc old-ids (:name l) (uuid))))))

;;------------------------------------------------------------------------------
;; Leaderboard State
;;------------------------------------------------------------------------------

(def leaders (atom [
  { :name "Chris"
    :board {}
    :score 282372 }
  { :name "Shaun"
    :board {}
    :score 232323 }
  { :name "Elaine"
    :board {}
    :score 232323 }
  { :name "Luis"
    :board {}
    :score 88 }
  { :name "Phil"
    :board {}
    :score 8282 }
  { :name "Rose"
    :board {}
    :score 746289 }
  { :name "Brian"
    :board {}
    :score 23232 }
  { :name "Brett"
    :board {}
    :score 882922 }
  { :name "Andrew"
    :board {}
    :score 99723 }
  { :name "Andy"
    :board {}
    :score 998237 }
  ]))

(def place-classes ".place-1")

(defn- update-board [idx itm]
  (let [place (+ idx 1)]
    (log idx)
    (log itm)
    (log "------------")
    ))

(defn- on-change-leaders [_ _ o n]
  (doall (map create-uuid-if-needed n))
  (doall (map-indexed
    update-board
    n)))

(add-watch leaders :main on-change-leaders)

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml board [b]
  [:div.board-a3a91 {:id (:name b)}
    [:div.name-1d96a (:name b)]
    [:div.board-45de4 "the board"]
    [:div.score-5aeae]])

(hiccups/defhtml dashboard-html []
  [:div.dashboard-0e330
    [:button#btn1 "Test 1"]
    [:button#btn2 "Test 2"]
    [:h2.time-left-eb709 "Time Left: 2:32"]
    [:div.boards-ad07f
      [:div#shaun.board-a3a91.place-1]
      [:div#elaine.board-a3a91.place-2]
      [:div#luis.board-a3a91.place-3]
      [:div#phil.board-a3a91.place-4]
      [:div#andrew.board-a3a91.place-5]
      [:div#chris.board-a3a91.place-6]
      [:div#brian.board-a3a91.place-7]
      [:div#zaki.board-a3a91.place-8]
      [:div#katie.board-a3a91.place-9]
      [:div#brett.board-a3a91.place-10]

      [:div.num-2b782.first-100e1 "1" [:sup "st"]]
      [:div.num-2b782.second-e09e1 "2" [:sup "nd"]]
      [:div.num-2b782.third-deef68 "3" [:sup "rd"]]
      [:div.num-2b782.fourth-a266b "4" [:sup "th"]]
      [:div.num-2b782.fifth-96fe6 "5" [:sup "th"]]
      [:div.num-2b782.sixth-fd905 "6" [:sup "th"]]
      [:div.num-2b782.seventh-84a13 "7" [:sup "th"]]
      [:div.num-2b782.eight-15b29 "8" [:sup "th"]]
      [:div.num-2b782.ninth-780fc "9" [:sup "th"]]
      [:div.num-2b782.tenth-34b19 "10" [:sup "th"]]

      ]])

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
  (swap! leaders reverse))

(defn- click-test2 []
  nil)

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
