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

(def test-board1
  [[0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "O6" "O12" 0 0 0 0] [0 0 0 0 "O3" "O9" 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "G6" "G12" 0 0 0 0] [0 0 0 0 "G3" "G9" 0 0 0 0] [0 0 0 0 0 "L4" 0 0 0 0] [0 0 0 "L2" "L10" "L9" 0 0 0 0] [0 0 0 0 "T4" 0 0 0 0 0] [0 0 0 "T2" "T11" "T8" 0 0 0 0] ["J4" 0 0 0 0 "J4" 0 0 0 0] ["J3" "J10" "J8" 0 0 "J3" "J10" "J8" 0 0] [0 "O6" "O12" 0 0 0 "Z2" "Z12" 0 0] [0 "O3" "O9" 0 0 "J4" 0 "Z3" "Z8" 0] [0 0 "L4" 0 0 "J3" "J10" "J8" 0 0] ["L2" "L10" "L9" 0 0 0 "I2" "I10" "I10" "I8"] [0 "O6" "O12" 0 0 "Z2" "Z12" 0 0 0] [0 "O3" "O9" 0 0 0 "Z3" "Z8" 0 0]])

(def test-board2
  [[0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "S6" "S8" 0 0 0 0] [0 0 0 "S2" "S9" 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 "Z4" 0 0 "G6" "G8" 0 0 0 0] ["Z6" "Z9" 0 "G2" "G9" "L4" 0 0 "T4" 0] ["Z1" 0 "S6" "S8" 0 "L5" 0 "T2" "T11" "T8"] ["J4" "S2" "S9" 0 0 "L3" "L8" "L4" 0 0] ["J3" "J10" "J8" 0 0 "L2" "L10" "L9" 0 0] [0 "T4" 0 0 0 0 0 "Z2" "Z12" 0] ["T2" "T11" "T8" 0 0 0 0 "L4" "Z3" "Z8"] [0 0 "O6" "O12" 0 "L2" "L10" "L9" 0 0] [0 "T4" "O3" "O9" 0 0 "Z2" "Z12" 0 0] ["T2" "T11" "T8" 0 0 0 0 "Z3" "Z8" 0]])

(def test-board3
  [[0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "T4" 0 0 0 0 0] [0 0 0 "T2" "T11" "T8" 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "G4" 0 0 0 0 0] [0 "S6" "S8" "G2" "G11" "G8" 0 0 0 0] ["S2" "S9" 0 0 "L4" 0 0 0 0 0] ["Z2" "Z12" 0 0 "L5" 0 0 0 0 0] [0 "Z3" "Z8" 0 "L3" "L8" 0 0 0 0] [0 "O6" "O12" 0 0 "I2" "I10" "I10" "I8" 0] [0 "O3" "O9" "S6" "S8" 0 "S6" "S8" 0 0] [0 "J4" "S2" "S9" 0 "S2" "S9" "T4" 0 0] [0 "J3" "J10" "J8" 0 0 "T2" "T11" "T8" 0]])

(declare board)

(def $ js/jQuery)

(def test-leaders [
  { :user "Chris"
    :pid 0
    :board test-board1
    :theme 0
    :score 23586 }
  { :user "Shaun"
    :pid 1
    :board test-board2
    :theme 1
    :score 232323 }
  { :user "Elaine"
    :pid 2
    :board test-board3
    :theme 2
    :score 232323 }
  { :user "Luis"
    :pid 3
    :board test-board1
    :theme 3
    :score 88 }
  { :user "Phil"
    :pid 4
    :board test-board2
    :theme 4
    :score 8282 }
  { :user "Rose"
    :pid 5
    :board test-board3
    :theme 5
    :score 746289 }
  { :user "Brian"
    :pid 6
    :board test-board1
    :theme 6
    :score 23232 }
  { :user "Brett"
    :pid 7
    :board test-board2
    :theme 7
    :score 882922 }
  { :user "Andrew"
    :pid 8
    :board test-board3
    :theme 8
    :score 99723 }
  { :user "Andy"
    :pid 9
    :board test-board1
    :theme 9
    :score 998237 }
  ])

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
  (when-not (by-id id)
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
        $el ($ (by-id id))
        $board ($ (str "#" id " .board-45de4"))]

    ;; hide board that are not in the top three
    (if (<= place 3)
      (.css $board "display" "")
      (.css $board "display" "none"))

    (.removeClass $el place-classes)
    (.addClass $el (str "place-" place))

    (.html ($ (str "#" id " .name-1d96a")) (:user itm))
    (.html ($ (str "#" id " .score-5aeae")) (util/format-number (:score itm)))
    
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
    [:button#btn-shuffle {:style "display:none"} "SHUFFLE"]
    [:h2.time-left-eb709 {:style "display:none"} "Time Left: 2:32"]
    [:div#boardsContainer.boards-ad07f
      [:div.num-2b782.first-100e1
        [:span.number-86f89 "1"]
        [:span.sup-0f414 "st"]]
      [:div.num-2b782.second-e09e1
        [:span.number-86f89 "2"]
        [:span.sup-0f414 "nd"]]
      [:div.num-2b782.third-deef68 
        [:span.number-86f89 "3"]
        [:span.sup-0f414 "rd"]]
      [:div.num-2b782.fourth-a266b
        [:span.number-86f89 "4"]
        [:span.sup-0f414 "th"]]
      [:div.num-2b782.fifth-96fe6
        [:span.number-86f89 "5"]
        [:span.sup-0f414 "th"]]
      [:div.num-2b782.sixth-fd905
        [:span.number-86f89 "6"]
        [:span.sup-0f414 "th"]]
      [:div.num-2b782.seventh-84a13
        [:span.number-86f89 "7"]
        [:span.sup-0f414 "th"]]
      [:div.num-2b782.eight-15b29
        [:span.number-86f89 "8"]
        [:span.sup-0f414 "th"]]
      [:div.num-2b782.ninth-780fc
        [:span.number-86f89 "9"]
        [:span.sup-0f414 "th"]]
      [:div.num-2b782.tenth-34b19
        [:span.number-86f89 "10"]
        [:span.sup-0f414 "th"]]]])

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

(defn on-leader-update
  [str-data]
  (let [data (read-string str-data)]
    (reset! leaders data)))

(defn on-time-left
  [total-seconds]
  (let [m (js/Math.floor (/ total-seconds 60))
        s (mod total-seconds 60)
        s-str (if (< s 10) (str "0" s) s)
        time-str (str m ":" s-str)]
    (.html ($ ".time-left-eb709") (str "Time Left: " time-str))
  ))

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

(defn init []
  (client.core/set-bw-background!)
  (.html ($ "#main-container") (dashboard-html))
  (swap! leaders identity)

  (.click ($ "#btn-shuffle") #(reset! leaders (shuffle test-leaders)))

  (.emit @socket "join-dashboard")

  (.on @socket "leader-update" on-leader-update)
  (.on @socket "time-left" on-time-left)

  (on-time-left 0)
  )

(defn cleanup
  []

  (.emit @socket "leave-dashboard")

  (.removeListener @socket "leader-update" on-leader-update)
  (.removeListener @socket "time-left" on-time-left)

  )
