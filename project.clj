(defproject t3tr0s "0.1.0-SNAPSHOT"
  :description "multiplayer Tetris™ for a 30th anniversary celebration tournament"
  :url "https://github.com/imalooney/t3tr0s"
  :license {
    :name "MIT License"
    :url "https://github.com/imalooney/t3tr0s/blob/master/LICENSE"
    :distribution :repo}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [hiccups "0.3.0"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.0-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds {
      :client {
        :source-paths ["src/client"]
        :compiler {
          :output-to "public/js/client.js"
          :output-dir "public/out"
          :optimizations :whitespace}}

      :client-adv {
        :source-paths ["src/client"]
        :compiler {
          :externs ["externs/jquery-1.9.js" "externs/socket.io.js"]
          :output-to "public/js/client.min.js"
          :optimizations :advanced
          :pretty-print false}}

     :server {
      :source-paths ["src/server"]
      :compiler {
        :target :nodejs
        :output-to "server.js"
        :optimizations :simple}}}}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :injections [(require 'weasel.repl.websocket)
               (def brepl #(cemerick.piggieback/cljs-repl :repl-env (weasel.repl.websocket/repl-env)))]
  )
