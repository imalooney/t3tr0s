(ns client.dom
  (:require
    client.html
    [client.util :as util]))

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

(def ^:private app-container-id (util/uuid))
(def ^:private panels-container-id (util/uuid))

(defn set-app-body! [html]
  (if-not (by-id app-container-id)
    (.prepend ($ "body") (str "<div id=" app-container-id "></div>")))
  (set-html! app-container-id html))

(defn set-panel-body! [panel-num html]
  (if-not (by-id panels-container-id)
    (set-app-body! (client.html/panels panels-container-id)))
  (set-html! (str "panel" panel-num) html))

;; NOTE: this function should only be called once on global init
(defn init! []
  (if-not (by-id panels-container-id)
    (set-app-body! (client.html/panels panels-container-id))))

(def panel-width 900)
(def panel-animation-speed 200)

(defn animate-to-panel
  ([panel-num] (animate-to-panel panel-num (fn [] nil)))
  ([panel-num next-fn]
    (.velocity ($ (str "#" panels-container-id))
      (js-obj "left" (str (* -1 panel-width (dec panel-num)) "px"))
      (js-obj
        "complete" next-fn
        "duration" panel-animation-speed))))

(defn set-color-background! []
  (-> ($ "body")
    (.removeClass "bg-grey-e2019")
    (.addClass "bg-color-c025c")))

(defn set-bw-background! []
  (-> ($ "body")
    (.removeClass "bg-color-c025c")
    (.addClass "bg-grey-e2019")))
