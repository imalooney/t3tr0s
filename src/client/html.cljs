(ns client.html
  (:require-macros [hiccups.core :as hiccups])
  (:require
    hiccups.runtime
    [client.util :as util]))

;;------------------------------------------------------------------------------
;; Panels
;;------------------------------------------------------------------------------

(hiccups/defhtml panels [id]
  [:div.panels-outer-1bae5
    [:div.panels-inner-a4472 {:id id}
      [:div#panel1.panel-855f4]
      [:div#panel2.panel-855f4]
      [:div#panel3.panel-855f4]
      [:div#panel4.panel-855f4]
      [:div#panel5.panel-855f4]
      [:div.clr-22ff3]]])

;;------------------------------------------------------------------------------
;; Menu
;;------------------------------------------------------------------------------

(hiccups/defhtml menu []
  [:div.wrapper-cd25d
    [:img.img-71ee9 {:src "/img/t3tr0s_logo_850w.png" :alt "T3TR0S Logo"}]
    [:div#menuInner.wrapper-c3b83
      [:a#soloBtn.green-btn-f67eb {:href "#/solo"} "Solo"]
      [:a#battleBtn.blue-btn-41e23 {:href "#/login"} "Battle"]
      [:div.clr-22ff3]]])