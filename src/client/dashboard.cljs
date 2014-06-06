(ns client.dashboard
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [client.game.paint :refer [draw-board! size-canvas!]]
    [client.game.board :refer [empty-board rows-cutoff]]
    [client.util :as util]
    [cljs.reader :refer [read-string]]
    [client.socket :refer [socket]]
    hiccups.runtime))

(def cell-size 24)

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
    (.append ($ "#boardsContainer") (board id))
    (size-canvas! (str "canvas-" id) empty-board cell-size rows-cutoff)
    ))

;;------------------------------------------------------------------------------
;; Leaderboard State
;;------------------------------------------------------------------------------

(def leaders (atom []))

(def place-classes (str "place-1 place-2 place-3 place-4 place-5"
  " place-6 place-7 place-8 place-9 place-10"))

(defn- update-board [idx itm]
  (create-uuid-if-needed! (:pid itm))
  (create-board-if-needed! (get @ids (:pid itm)))
  (let [place (+ idx 1)
        id (get @ids (:pid itm))
        $el ($ (by-id id))]

    (.removeClass $el place-classes)
    (.addClass $el (str "place-" place))

    (.html ($ (str "#" id " .name-1d96a")) (:user itm))
    (.html ($ (str "#" id " .score-5aeae")) (:score itm))
    (js/console.log (:score itm))

    (draw-board! (str "canvas-" id) (:board itm) cell-size (:theme itm) rows-cutoff)

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
    [:div.board-45de4
     [:canvas.canvas-ad036 {:id (str "canvas-" id)}]]
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
    (reset! leaders data)))

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

(defn init []
  (client.core/set-bw-background!)
  (.html ($ "#main-container") (dashboard-html))
  (swap! leaders identity)

  (.emit @socket "join-dashboard")

  (.on @socket "leader-update" on-leader-update)

  )

(defn cleanup
  []

  (.emit @socket "leave-dashboard")

  (.removeListener @socket "leader-update" on-leader-update)

  )
