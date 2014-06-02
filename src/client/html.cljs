(ns client.html
	(:require-macros [hiccups.core :as hiccups])
	(:require
		[hiccups.runtime :as hiccupsrt]))

(defn by-id [id]
	(.getElementById js/document id))

(hiccups/defhtml home-page []
	[:div {:style "width:100%;"}
		[:div {:style "width:700px;"}
			[:img {:src "img/t3tr0s_500w.png"}]
			[:div
				[:div.lg-btn-container
					[:button.lg-btn "SOLO"]]				
				[:div.lg-btn-container
					[:button.lg-btn "LOBBY"]]]]])

(defn ^:export homeInit []
	(let [container (by-id "main-container")] 
		(set! (.-innerHTML container) (home-page))))