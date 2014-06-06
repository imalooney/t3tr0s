(ns client.game
	(:require-macros [hiccups.core :as hiccups])
	(:require
		hiccups.runtime
    [client.socket :refer [socket]]
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

(hiccups/defhtml countdown-html []
  [:h1#countdown "Connecting..."])

;;------------------------------------------------------------
;; Page initialization.
;;------------------------------------------------------------

(defn init-game
  "Show and start the game."
  []
  (.html ($ "#main-container") (game-html))
  (client.game.core/init)
  )

(defn on-countdown
  "Called when the countdown message is received from server."
  [i]
  (if (> i 0)
    (.html ($ "#countdown") i)
    (init-game))
  )

(defn init-countdown
  "Show and listen for the countdown messages."
  []
  (.html ($ "#main-container") (countdown-html))
  (.on @socket "countdown" on-countdown)
  )

(defn init
  []

  ; NOTE: we cannot join a BATTLE-GAME by simply navigating
  ; to its URL.  It must be joined from the lobby, where
  ; we emit a "join-game" message.

  ; Only show the countdown if we are in a battle game
  ; so we can wait for the game to start.
  ; NOTE: this prevents the player from rejoining a game
  ;       which is what we want.
  (if @client.game.core/battle
    (init-countdown)
    (init-game))

  )

(defn cleanup
  []

  ; Leave the game room.
  (.emit @socket "leave-game")

  ; Ignore the countdown messages.
  (.removeListener @socket "countdown" on-countdown)

  ; Shutdown the game facilities.
  (client.game.core/cleanup)

  )

