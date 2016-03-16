(ns client.dom
  (:require
    client.html
    [client.util :refer [js-log log]]))

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; DOM Helper Functions
;;------------------------------------------------------------------------------

(defn by-id [id]
  (.getElementById js/document id))

(defn set-html! [id html]
  (aset (by-id id) "innerHTML" html))

(defn show-el! [id]
  (aset (by-id id) "style" "display" ""))

(defn hide-el! [id]
  (aset (by-id id) "style" "display" "none"))

(defn remove-el! [id]
  (let [el (by-id id)
        parent-el (aget el "parentNode")]
    (.removeChild parent-el el)))

(defn get-value [id]
  (aget (by-id id) "value"))

(defn set-value! [id v]
  (aset (by-id id) "value" v))

;;------------------------------------------------------------------------------
;; Set App State Functions
;;------------------------------------------------------------------------------

(defn set-color-background! []
  (-> ($ "body")
      (.removeClass "bg-grey-e2019")
      (.addClass "bg-color-c025c")))

(defn set-bw-background! []
  (-> ($ "body")
      (.removeClass "bg-color-c025c")
      (.addClass "bg-grey-e2019")))

(def ^:private app-container-id (str (random-uuid)))
(def ^:private panels-container-id (str (random-uuid)))

(defn set-app-body!
  "Fills the app with html from the root."
  [html]
  (when-not (by-id app-container-id)
    (.prepend ($ "body") (str "<div id=" app-container-id "></div>")))
  (set-html! app-container-id html))

(defn set-panel-body!
  "Fills a panel with html. Will create the panels if they do not exist."
  [panel-num html]
  (when-not (by-id panels-container-id)
    (set-app-body! (client.html/panels panels-container-id)))
  (set-html! (str "panel" panel-num) html))

(def current-panel (atom nil))

(def panel-width 900)
(def panel-animation-speed 200)

(defn animate-to-panel
  ([panel-num]
   (animate-to-panel panel-num (fn [] nil)))
  ([panel-num next-fn]
   (reset! current-panel panel-num)
   (let [$panel ($ (str "#" panels-container-id))]
     (.velocity $panel
       (js-obj "left" (str (* -1 panel-width (dec panel-num)) "px"))
       (js-obj "complete" next-fn
               "duration" panel-animation-speed)))))
