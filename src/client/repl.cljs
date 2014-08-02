(ns client.repl
  (:require
    [weasel.repl :as repl]))

(defn connect!
  "Connect to the websocket REPL to allow debugging."
  []
  (repl/connect "ws://localhost:9001" :verbose true))