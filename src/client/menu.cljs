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
		[:img.logo {:src "img/t3tr0s_500w.png"}]
		[:div
			[:div.lg-btn-container
				[:button#solo-btn.lg-btn "SOLO"]]
			[:div.lg-btn-container
				[:button#lobby-btn.lg-btn "LOBBY"]]]])

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  []

  ; Initialize page content
  (.html ($ "#main-container") (menu-html))

  (.click ($ "#solo-btn") #(aset js/location "hash" "#/solo-game"))
  (.click ($ "#lobby-btn") #(aset js/location "hash" "#/lobby"))

  )

(defn cleanup
  []
  nil)
