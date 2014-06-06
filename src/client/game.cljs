(ns client.game
	(:require-macros [hiccups.core :as hiccups])
	(:require
    [cljs.reader :refer [read-string]]
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

(hiccups/defhtml gameover-html [ranks]
  [:h1 "Game over"]
  [:ol {:style "color:white"}
    (for [player ranks]
      [:li
       (str
         (:user player) " - "
         (:score player) " points - "
         (:total-lines player) " lines")])])

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

(declare cleanup)

(defn on-game-over
  "Called when game over message received from server."
  [str-data]
  (js/console.log "game over")
  (cleanup)
  (let [data (read-string str-data)]
    (.html ($ "#main-container") (gameover-html data))))

(defn on-time-left
  "Called when server sends a time-left update."
  [s]
  (js/console.log "time left:" s))

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

  (.on @socket "game-over" on-game-over)
  (.on @socket "time-left" on-time-left)

  )

(defn cleanup
  []

  ; Leave the game room.
  (.emit @socket "leave-game")

  ; Ignore the countdown messages.
  (.removeListener @socket "countdown" on-countdown)

  ; Shutdown the game facilities.
  (client.game.core/cleanup)

  (.removeListener @socket "game-over" on-game-over)
  (.removeListener @socket "time-left" on-time-left)

  )

