(ns client.html
	(:require-macros [hiccups.core :as hiccups])
	(:require
		[hiccups.runtime :as hiccupsrt]))

(defn by-id [id]
	(.getElementById js/document id))

(hiccups/defhtml login []
	[:div#inner-container.login
		[:div.login-container
			[:label "What is your name?"]
			[:input.login-name {:type "text"}]
			[:button#submit.lg-btn "OK"]]])

(hiccups/defhtml homepage []
	[:div#inner-container
		[:img.logo {:src "img/t3tr0s_500w.png"}]
		[:div
			[:div.lg-btn-container
				[:button.lg-btn "SOLO"]]				
			[:div.lg-btn-container
				[:button.lg-btn "LOBBY"]]]])

(defn ^:export homeInit []
	(let [container (by-id "main-container")] 
		(set! (.-innerHTML container) (homepage))))