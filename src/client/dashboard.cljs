(ns client.dashboard
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.util :as util]
    [cljs.reader :refer [read-string]]
    [client.socket :refer [socket]]
    hiccups.runtime))

;; TODO:
;; - hide empty boards that are not in the leaderboard 10
;; - more efficient board updating code?

(declare board)

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; Username to UUID Mapping
;;------------------------------------------------------------------------------

(def ids 
  "Stores a mapping of username --> UUID for use as DOM ids"
  (atom {}))

(defn create-uuid-if-needed!
  "creates a mapping of username --> UUID in the ids atom if one does not already exist"
  [n]
  (if-not (get @ids n)
    (swap! ids #(assoc % n (util/uuid)))))

(defn- by-id [id]
  (.getElementById js/document id))

(defn- create-board-if-needed! [id]
  (if-not (by-id id)
    (.append ($ "#boardsContainer") (board id))))

;;------------------------------------------------------------------------------
;; Leaderboard State
;;------------------------------------------------------------------------------

(def leaders1 [
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
  ])

(def leaders2 [
  { :name "Shaun"
    :board {}
    :score 232323 }
  { :name "Chris"
    :board {}
    :score 282372 }
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
  ])

(def leaders (atom [
  { :name "Chris"
    :board {}
    :score 282372 }
  { :name "Elaine"
    :board {}
    :score 232323 }
  { :name "Shaun"
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

(def place-classes (str "place-1 place-2 place-3 place-4 place-5"
  " place-6 place-7 place-8 place-9 place-10"))

(defn- update-board [idx itm]
  (create-uuid-if-needed! (:name itm))
  (create-board-if-needed! (get @ids (:name itm)))
  (let [place (+ idx 1)
        id (get @ids (:name itm))
        $el ($ (by-id id))]
    (.removeClass $el place-classes)
    (.addClass $el (str "place-" place))
    (.html ($ (str "#" id " .name-1d96a")) (:name itm))
    (.html ($ (str "#" id " .score-5aeae")) (:score itm))
    ;; TODO: update the board right here, the element is:
    ;; (str "#" id ".board-45de4")
    ))

(defn- on-change-leaders [_ _ _ new-leaders]
  (doall
    (map-indexed
      update-board
      new-leaders))
  ;; TODO: hide any .board-a3a91 elements that do not have a .place class
  )

(add-watch leaders :main on-change-leaders)

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml board [id]
  [:div.board-a3a91 {:id id}
    [:div.name-1d96a]
    [:div.board-45de4]
    [:div.score-5aeae]])

(hiccups/defhtml dashboard-html []
  [:div.header-d680b
    [:img {:src "/img/t3tr0s_logo_200w.png" :alt ""}]
    [:h1.title-d49ea "Scoreboard"]]
  [:div.dashboard-0e330
    [:button#btn1 "Test 1"]
    [:button#btn2 "Test 2"]
    [:h2.time-left-eb709 "Time Left: 2:32"]
    [:div#boardsContainer.boards-ad07f
      [:div.num-2b782.first-100e1 "1" [:sup "st"]]
      [:div.num-2b782.second-e09e1 "2" [:sup "nd"]]
      [:div.num-2b782.third-deef68 "3" [:sup "rd"]]
      [:div.num-2b782.fourth-a266b "4" [:sup "th"]]
      [:div.num-2b782.fifth-96fe6 "5" [:sup "th"]]
      [:div.num-2b782.sixth-fd905 "6" [:sup "th"]]
      [:div.num-2b782.seventh-84a13 "7" [:sup "th"]]
      [:div.num-2b782.eight-15b29 "8" [:sup "th"]]
      [:div.num-2b782.ninth-780fc "9" [:sup "th"]]
      [:div.num-2b782.tenth-34b19 "10" [:sup "th"]]]])

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

(defn on-leader-update
  [str-data]
  (let [data (read-string str-data)]
    nil)
  )

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- click-test1 []
  (reset! leaders leaders1))

(defn- click-test2 []
  (reset! leaders leaders2))

(defn- add-events []
  (.on ($ "#btn1") "click" click-test1)
  (.on ($ "#btn2") "click" click-test2))

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

(defn init []
  (client.core/set-bw-background!)
  (.html ($ "#main-container") (dashboard-html))
  (add-events)
  (swap! leaders identity)

  (.emit @socket "join-dashboard")

  (.on @socket "leader-update" on-leader-update)

  )

(defn cleanup
  []

  (.emit @socket "leave-dashboard")

  (.removeListener @socket "leader-update" on-leader-update)

  )
