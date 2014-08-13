(ns client.dom
  (:require
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

(defn set-page-body! [h]
  (set-html! "appContainer" h))

(defn set-color-background! []
  (-> ($ "body")
    (.removeClass "bg-grey-e2019")
    (.addClass "bg-color-c025c")))

(defn set-bw-background! []
  (-> ($ "body")
    (.removeClass "bg-color-c025c")
    (.addClass "bg-grey-e2019")))
