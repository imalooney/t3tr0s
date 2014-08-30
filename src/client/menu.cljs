(ns client.menu
  (:require-macros [hiccups.core :as hiccups])
  (:require
    hiccups.runtime
    [client.dom :as dom]))

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml menu-html []
  [:div.wrapper-cd25d
    [:img {:src "/img/t3tr0s_logo_850w.png" :alt "T3TR0S Logo"}]
    [:div#menuInnerWrapper
      [:a#soloBtn.green-btn-f67eb {:href "#/play-solo"} "Solo"]
      [:a#battleBtn.blue-btn-41e23 {:href "#/login"} "Battle"]
      [:div.clr-22ff3]]])

;;------------------------------------------------------------------------------
;; Page Initialization
;;------------------------------------------------------------------------------

(defn init []
  (dom/set-color-background!)
  (dom/set-page-body! (menu-html)))