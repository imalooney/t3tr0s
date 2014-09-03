(ns client.pages.spectate
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [cljs.reader :refer [read-string]]
    hiccups.runtime
    [client.dom :as dom]
    [client.game.paint :refer [draw-board! size-canvas!]]
    [client.game.board :refer [empty-board rows-cutoff]]
    [client.util :as util]
    [client.socket :as socket]))

(declare
  place->coords
  place-id)

(def $ js/jQuery)

(def large-board-cell-size 15)
(def small-board-cell-size 12)

(def first-row-height 473)
(def small-row-height 399)
(def small-row-width 200)

;; TODO: finish this idea
(def max-num-places-to-show 100)

;;------------------------------------------------------------------------------
;; Dummy Data for Debugging / Testing
;;------------------------------------------------------------------------------

;; TODO: rewrite this as a function to produce random page-state

(def test-board1
  [[0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "O6" "O12" 0 0 0 0] [0 0 0 0 "O3" "O9" 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "G6" "G12" 0 0 0 0] [0 0 0 0 "G3" "G9" 0 0 0 0] [0 0 0 0 0 "L4" 0 0 0 0] [0 0 0 "L2" "L10" "L9" 0 0 0 0] [0 0 0 0 "T4" 0 0 0 0 0] [0 0 0 "T2" "T11" "T8" 0 0 0 0] ["J4" 0 0 0 0 "J4" 0 0 0 0] ["J3" "J10" "J8" 0 0 "J3" "J10" "J8" 0 0] [0 "O6" "O12" 0 0 0 "Z2" "Z12" 0 0] [0 "O3" "O9" 0 0 "J4" 0 "Z3" "Z8" 0] [0 0 "L4" 0 0 "J3" "J10" "J8" 0 0] ["L2" "L10" "L9" 0 0 0 "I2" "I10" "I10" "I8"] [0 "O6" "O12" 0 0 "Z2" "Z12" 0 0 0] [0 "O3" "O9" 0 0 0 "Z3" "Z8" 0 0]])

(def test-board2
  [[0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "S6" "S8" 0 0 0 0] [0 0 0 "S2" "S9" 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 "Z4" 0 0 "G6" "G8" 0 0 0 0] ["Z6" "Z9" 0 "G2" "G9" "L4" 0 0 "T4" 0] ["Z1" 0 "S6" "S8" 0 "L5" 0 "T2" "T11" "T8"] ["J4" "S2" "S9" 0 0 "L3" "L8" "L4" 0 0] ["J3" "J10" "J8" 0 0 "L2" "L10" "L9" 0 0] [0 "T4" 0 0 0 0 0 "Z2" "Z12" 0] ["T2" "T11" "T8" 0 0 0 0 "L4" "Z3" "Z8"] [0 0 "O6" "O12" 0 "L2" "L10" "L9" 0 0] [0 "T4" "O3" "O9" 0 0 "Z2" "Z12" 0 0] ["T2" "T11" "T8" 0 0 0 0 "Z3" "Z8" 0]])

(def test-board3
  [[0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "T4" 0 0 0 0 0] [0 0 0 "T2" "T11" "T8" 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 0 0 0 0 0 0] [0 0 0 0 "G4" 0 0 0 0 0] [0 "S6" "S8" "G2" "G11" "G8" 0 0 0 0] ["S2" "S9" 0 0 "L4" 0 0 0 0 0] ["Z2" "Z12" 0 0 "L5" 0 0 0 0 0] [0 "Z3" "Z8" 0 "L3" "L8" 0 0 0 0] [0 "O6" "O12" 0 0 "I2" "I10" "I10" "I8" 0] [0 "O3" "O9" "S6" "S8" 0 "S6" "S8" 0 0] [0 "J4" "S2" "S9" 0 "S2" "S9" "T4" 0 0] [0 "J3" "J10" "J8" 0 0 "T2" "T11" "T8" 0]])

(def test-state [
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

  { :user "Chris Pounds"
    :pid 112
    :board test-board2
    :theme 1
    :score 232323 }
  { :user "Elaine"
    :pid 223
    :board test-board3
    :theme 2
    :score 232323 }
  { :user "Luis"
    :pid 345
    :board test-board1
    :theme 3
    :score 88 }
  { :user "Phil"
    :pid 445
    :board test-board2
    :theme 4
    :score 8282 }
  { :user "Rose"
    :pid 534
    :board test-board3
    :theme 5
    :score 746289 }
  { :user "Brian"
    :pid 633
    :board test-board1
    :theme 6
    :score 23232 }
  { :user "Brett"
    :pid 775
    :board test-board2
    :theme 7
    :score 882922 }
  { :user "Andrew"
    :pid 820
    :board test-board3
    :theme 8
    :score 99723 }
  { :user "AndyAndyAndy"
    :pid 982
    :board test-board1
    :theme 9
    :score 998237 }

  ])

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml board-container [id]
  [:div.container-13c1f {:id id}
    [:div {:id (str id "-name")}]
    [:canvas.canvas-b534a {:id (str id "-canvas")}]
    [:div {:id (str id "-score")}]])

;; TODO: I'm sure large-place and small-place could be combined cleanly

(hiccups/defhtml large-place [p]
  [:div.place-2288d
    {:id (place-id p)
     :style (let [js-coords (place->coords p)]
       (str "top: " (aget js-coords "top") "px; "
            "left: " (aget js-coords "left") "px"))}
    [:svg.circle-8ba96 {:height 40 :width 40}
      [:circle {:cx 20 :cy 20 :r 19 :fill "#fff"}]]
    [:div.num-a9a1f p]])

(hiccups/defhtml small-place [p]
  [:div.place-2288d
    {:id (place-id p)
     :style (let [js-coords (place->coords p)]
       (str "top: " (aget js-coords "top") "px; "
            "left: " (aget js-coords "left") "px"))}
    [:svg.circle-8ba96 {:height 36 :width 36}
      [:circle {:cx 18 :cy 18 :r 17 :fill "#fff"}]]
    [:div.num-cf8ed p]])

(hiccups/defhtml pieces-table []
  [:table.tbl-c8c6b
    [:tbody
      [:tr
        [:td.piece-fbcb9 [:img {:src "/img/pieces/i.png" :alt "I Piece" :style "width: 48px"}]]
        [:td.number-ca2a6 [:span#pieceCountI "0"]]
        [:td.spacer-67b8d]
        [:td.piece-fbcb9 [:img {:src "/img/pieces/s.png" :alt "S Piece" :style "width: 36px"}]]
        [:td.number-ca2a6 [:span#pieceCountS "0"]]]
      [:tr
        [:td.piece-fbcb9 [:img {:src "/img/pieces/j.png" :alt "J Piece" :style "width: 36px"}]]
        [:td.number-ca2a6 [:span#pieceCountJ "0"]]
        [:td.spacer-67b8d]
        [:td.piece-fbcb9 [:img {:src "/img/pieces/t.png" :alt "T Piece" :style "width: 36px"}]]
        [:td.number-ca2a6 [:span#pieceCountT "0"]]]
      [:tr
        [:td.piece-fbcb9 [:img {:src "/img/pieces/l.png" :alt "L Piece" :style "width: 36px"}]]
        [:td.number-ca2a6 [:span#pieceCountL "0"]]
        [:td.spacer-67b8d]
        [:td.piece-fbcb9 [:img {:src "/img/pieces/z.png" :alt "Z Piece" :style "width: 36px"}]]
        [:td.number-ca2a6 [:span#pieceCountZ "0"]]]
      [:tr
        [:td.piece-fbcb9 [:img {:src "/img/pieces/o.png" :alt "O Piece" :style "width: 24px"}]]
        [:td.number-ca2a6 [:span#pieceCountO "0"]]]]])

(hiccups/defhtml stats []
  [:div.label-39b9c "Pieces"]
  (pieces-table)
  [:div.line-8975a]
  [:div.label-39b9c "Lines"]
  [:div#totalLines.metric-b93a8 "0"]
  [:div.line-8975a]
  [:div.label-39b9c "Time Left"]
  [:div#timeLeft.metric-b93a8])

(hiccups/defhtml page-shell []
  [:div.wrapper-2ba66
    [:div.hdr-93a4f
      [:img.logo-dd80d {:src "/img/t3tr0s_logo_200w.png" :alt "T3TR0S Logo"}]
      [:h1.title-6637f "Spectate"]]
    ;; NOTE: this button is for testing purposes
    [:button#shuffleBtn {:style "padding: 12px 24px; display: none"} "Shuffle!"]
    [:div#boardsContainer.wrapper-4b797
      [:div.stats-2cda4 (stats)]]])

;;------------------------------------------------------------------------------
;; Pid to UUID Mapping
;;------------------------------------------------------------------------------

(def ids
  "Stores a mapping of pid --> UUID for use as DOM ids"
  (atom {}))

(defn- create-uuid-if-needed!
  "creates a mapping of pid --> UUID in the ids atom if one does not already exist"
  [n]
  (if-not (get @ids n)
    (swap! ids #(assoc % n (util/uuid)))))

;;------------------------------------------------------------------------------
;; Dashboard State
;;------------------------------------------------------------------------------

(def page-state (atom []))

(defn- update-board-canvas! [id itm place]
  (let [cell-size (if (<= place 3) large-board-cell-size small-board-cell-size)
        canvas-id (str id "-canvas")]
    (size-canvas! canvas-id (:board itm) cell-size rows-cutoff)
    (draw-board! canvas-id (:board itm) cell-size (:theme itm) rows-cutoff)))

;; NOTE: does NOT apply to places 1, 2, 3
(defn- place->row
  "Given a place, returns the row it is on."
  [p]
  (if (<= p 3)
    0
    (js/Math.ceil (/ (- p 3) 5))))

;; NOTE: does NOT apply to places 1, 2, 3
(defn- place->col
  "Given a place, returns the column it is on."
  [p]
  (let [r (rem (- p 3) 5)]
    (if (= r 0)
      5
      r)))

(defn- place->coords
  "Returns top + left coordinates for a place.
   NOTE: returns a js-object"
  [p]
  (cond
    (= p 1) (js-obj "top" 0 "left" 0)
    (= p 2) (js-obj "top" 0 "left" 240)
    (= p 3) (js-obj "top" 0 "left" 480)
    :else (js-obj
      "top" (+ (* (dec (place->row p)) small-row-height) first-row-height)
      "left" (* (dec (place->col p)) small-row-width))))

(defn- place-id [p]
  (str "dashboardPlace" p))

(defn- calc-height [num-boards]
  (if (<= num-boards 3)
    (+ first-row-height 40)
    (+ first-row-height (* (place->row num-boards) small-row-height) 40)))

(defn- set-container-height!
  "Sets the height of the boards container based on how many boards there are."
  [num-boards]
  (.css ($ "#boardsContainer")
    "height" (str (calc-height num-boards) "px")))

(defn- add-place-if-needed! [p]
  (if-not (dom/by-id (place-id p))
    (.append ($ "#boardsContainer")
      (if (<= p 3) (large-place p) (small-place p)))))

(defn- update-place-numbers! [num-places]
  (doall (map add-place-if-needed! (range 1 (inc num-places))))

  ;; remove places that are no longer in use
  (.each ($ "#boardsContainer .place-2288d") (fn [_idx $el]
    (let [id (aget $el "id")
          place-num (int (.replace id "dashboardPlace" ""))]
      (if (> place-num num-places)
        (.remove $el)))))
  )

(defn- create-board-container-if-needed! [id]
  (when-not (dom/by-id id)
    (.append ($ "#boardsContainer") (board-container id))))

(defn- update-board! [idx itm]
  (create-uuid-if-needed! (:pid itm))
  (create-board-container-if-needed! (get @ids (:pid itm)))
  (let [place (+ idx 1)
        id (get @ids (:pid itm))
        name-id (str id "-name")
        score-id (str id "-score")]
    (update-board-canvas! id itm place)
    (dom/set-html! name-id (:user itm))
    (dom/set-html! score-id (-> itm :score util/format-number))

    ;; different sizes for name, scores when in top 3
    ;; TODO: this should probably be replaced with a single class on the container
    (when (<= place 3)
      (.removeClass ($ (str "#" name-id)) "small-name-ecd97")
      (.addClass ($ (str "#" name-id)) "large-name-2eee0")
      (.removeClass ($ (str "#" score-id)) "small-score-7d14a")
      (.addClass ($ (str "#" score-id)) "large-score-a2b90"))

    (when (> place 3)
      (.removeClass ($ (str "#" name-id)) "large-name-2eee0")
      (.addClass ($ (str "#" name-id)) "small-name-ecd97")
      (.removeClass ($ (str "#" score-id)) "large-score-a2b90")
      (.addClass ($ (str "#" score-id)) "small-score-7d14a"))

    ;; animate to the new place
    (.velocity ($ (str "#" id))
      (place->coords place)
      (js-obj "duration" 200))))

(defn- update-all-boards! [boards]
  (doall (map-indexed update-board! boards))

  ;; clean up boards that are no longer used
  (let [board-ids (into #{} (vals @ids))]
    (.each ($ "#boardsContainer .container-13c1f") (fn [_idx $el]
      (let [id (aget $el "id")]
        (if-not (contains? board-ids id)
          (.remove $el))))))
  )

(defn- update-total-lines! [players]
  (dom/set-html! "totalLines"
    (if (empty? players)
      "0"
      (reduce + (map :total-lines players)))))

(defn- on-change-page-state [_ _ _ new-s]
  (update-total-lines! new-s)
  (set-container-height! (count new-s))
  (update-place-numbers! (count new-s))
  (update-all-boards! new-s))

(add-watch page-state :main on-change-page-state)

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

;; Shaun - can we send the position information from the server on the
;;   "board-update" event?
;; would be nice to not have to loop through page-state here
(defn on-board-update
  "Called when receiving a board update from a visible leader."
  [str-data]
  (let [board-state (read-string str-data)
        pid (:pid board-state)
        id (get @ids pid)]

    (doall (map-indexed
      (fn [idx itm]
        (if (= pid (:pid itm))
          (update-board-canvas! id board-state (inc idx))))
      @page-state))))

;; TODO: this should be renamed from "leader", but need to change the backend
(defn on-leader-update
  "Called when receiving an updated list of leaders and their scores."
  [str-data]
  (let [data (read-string str-data)]
    (reset! page-state data)))

(defn- on-piece-stats
  "Called when we receive new piece stats from the server."
  [data-str]
  (let [piece-counts (read-string data-str)]
    (dom/set-html! "pieceCountI" (util/format-number (:I piece-counts)))
    (dom/set-html! "pieceCountS" (util/format-number (:S piece-counts)))
    (dom/set-html! "pieceCountJ" (util/format-number (:J piece-counts)))
    (dom/set-html! "pieceCountT" (util/format-number (:T piece-counts)))
    (dom/set-html! "pieceCountL" (util/format-number (:L piece-counts)))
    (dom/set-html! "pieceCountZ" (util/format-number (:Z piece-counts)))
    (dom/set-html! "pieceCountO" (util/format-number (:O piece-counts)))))

(defn on-time-left
  "Called when receiving time-left update from server."
  [total-seconds]
  (let [m (js/Math.floor (/ total-seconds 60))
        s (mod total-seconds 60)
        s-str (if (< s 10) (str "0" s) s)
        time-str (str m ":" s-str)]
    (dom/set-html! "timeLeft" time-str))

  ;; clear the ids atom when the round is over
  ;; NOTE: this is a hack, but it should do the trick to reset ids every round
  (if (= total-seconds 0)
    (reset! ids {}))
  )

(defn- shuffle-page-state []
  (reset! page-state (shuffle test-state)))

;;------------------------------------------------------------------------------
;; DOM Events
;;------------------------------------------------------------------------------

(defn- add-events []
  (.on ($ "#shuffleBtn") "click" shuffle-page-state))

;;------------------------------------------------------------------------------
;; Page Initialization / Cleanup
;;------------------------------------------------------------------------------

;; TODO: this page should never be in an invalid state
;; ie: waiting for game to start, show results of previous round, etc
;; should show *something* no matter what

(defn init! []
  (dom/set-bw-background!)
  (dom/set-app-body! (page-shell))
  (add-events)

  (swap! page-state identity)

  ;; NOTE: for debugging
  ; (shuffle-page-state)
  ; (js/setTimeout shuffle-page-state 2000)
  ; (js/setTimeout shuffle-page-state 4000)
  ; (js/setTimeout shuffle-page-state 6000)
  ; (js/setInterval shuffle-page-state 2000)

  (socket/emit "join-dashboard")

  (socket/on "board-update" on-board-update)
  (socket/on "leader-update" on-leader-update)
  (socket/on "piece-stats" on-piece-stats)
  (socket/on "time-left" on-time-left)

  (on-time-left 0))

(defn cleanup!
  []
  (socket/emit "leave-dashboard")
  (socket/removeListener "board-update")
  (socket/removeListener "leader-update")
  (socket/removeListener "piece-stats")
  (socket/removeListener "time-left"))
