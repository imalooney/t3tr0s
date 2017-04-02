(ns client.config)

(def config (aget js/window "T3TR0S_CONFIG"))

(def use-repl? (aget config "use-repl"))
(def single-player-only? (aget config "single-player-only"))
