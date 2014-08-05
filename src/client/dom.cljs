(ns client.dom
  (:require
    [client.util :as util]))

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; DOM Helper Functions
;;------------------------------------------------------------------------------

(defn by-id
  "Returns a DOM element."
  [el-or-id]
  (if (= (type el-or-id) js/String)
    (.getElementById js/document el-or-id)
    el-or-id))

(defn set-html! [el html]
  (aset (by-id el) "innerHTML" html))

(defn show-el! [el]
  (aset (by-id el) "style" "display" ""))

(defn hide-el! [el]
  (aset (by-id el) "style" "display" "none"))

(defn remove-el! [el]
  (let [el1 (by-id el)
        parent-el (aget el1 "parentNode")]
    (.removeChild parent-el el1)))

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
