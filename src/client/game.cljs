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
	[:div#inner-container
		[:div.player-view
			[:div#canvas-wrap
				[:canvas#game-canvas]]
			[:div#scoreboard
				[:div.next-area
					[:span.next-header "Next: "]
					[:canvas#next-canvas]]
				[:div.game-stats
					[:div#score]
					[:div#level]
					[:div#lines]]]]
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
