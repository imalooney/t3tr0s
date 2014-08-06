(ns client.menu
  (:require-macros [hiccups.core :as hiccups])
  (:require
    hiccups.runtime
    [client.dom :as dom]))

(def $ js/$)

;;------------------------------------------------------------------------------
;; HTML
;;------------------------------------------------------------------------------

(hiccups/defhtml menu-html []
  [:div.wrapper-cd25d
    [:img {:src "/img/t3tr0s_logo_850w.png" :alt "T3TR0S Logo"}]
    [:button#soloBtn.green-btn-f67eb "Solo"]
    [:button#battleBtn.blue-btn-41e23 "Battle!"]
    [:div.clr-22ff3]])

;;------------------------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------------------------

(defn- add-events []
  (.click ($ "#soloBtn") #(aset js/location "hash" "#/solo-game"))
  (.click ($ "#battleBtn") #(aset js/location "hash" "#/login")))

(defn init
  []
  (dom/set-color-background!)
  (dom/set-page-body! (menu-html))
  (add-events))

(defn cleanup
  []
  nil)
