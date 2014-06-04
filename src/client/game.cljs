(ns client.game
	(:require-macros [hiccups.core :as hiccups])
	(:require
		hiccups.runtime
    client.game.core
    ))

(def $ js/$)

;;------------------------------------------------------------
;; HTML
;;------------------------------------------------------------

(hiccups/defhtml game-html []
	[:div.player-view
		[:div#theme-options 
			"Press keys 0-9 to change your theme."]
		[:div#canvas-wrap
			[:canvas#game-canvas]]
		[:div.right-side
			[:div#scoreboard
				[:div.next-area
					[:span.next-header "Next: "]
					[:canvas#next-canvas]]
				[:div.game-stats
					[:div#score]
					[:div#level]
					[:div#lines]]]
			[:div#theme "1984"]
			[:div#theme-details "Electronika 60"]]
		[:section#arena]])

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init
  []

  ; Initialize page content
  (.html ($ "#main-container") (game-html))

  ; Initialize the game.
  (client.game.core/init)

  )

(defn cleanup
  []
  (client.game.core/cleanup)
  )
