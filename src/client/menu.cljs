(ns client.menu
  (:require-macros [hiccups.core :as hiccups])
  (:require
    hiccups.runtime))

(def $ js/$)

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml menu-html []
  [:div#inner-container
    [:div.logo-31d54]
    [:div.menu-cd25d
      [:button#solo-btn.green-btn-f67eb "SOLO"]
      [:button#lobby-btn.blue-btn-41e23 "BATTLE"]]])

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  []

  (client.core/set-color-background!)

  ; Initialize page content
  (.html ($ "#main-container") (menu-html))

  (.click ($ "#solo-btn") #(aset js/location "hash" "#/solo-game"))
  (.click ($ "#lobby-btn") #(aset js/location "hash" "#/lobby"))

  )

(defn cleanup
  []
  nil)
