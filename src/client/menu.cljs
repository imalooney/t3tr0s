(ns client.menu
  (:require-macros [hiccups.core :as hiccups])
  (:require
    hiccups.runtime
    [client.dom :as dom]))

(def $ js/$)

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml menu-html []
  [:div.inner-6ae9d
    [:div.logo-31d54]
    [:div.menu-cd25d
      [:button#solo-btn.green-btn-f67eb "Solo"]
      [:button#lobby-btn.blue-btn-41e23 "Battle"]]])

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  []

  (dom/set-color-background!)

  ; Initialize page content
  (dom/set-page-body! (menu-html))

  (.click ($ "#solo-btn") #(aset js/location "hash" "#/solo-game"))
  (.click ($ "#lobby-btn") #(aset js/location "hash" "#/lobby"))

  )

(defn cleanup
  []
  nil)
