(defproject t3tr0s "0.1.0-SNAPSHOT"
  :description "multiplayer Tetris™ for a 30th anniversary celebration tournament"
  :url "https://github.com/imalooney/t3tr0s"
  :license {:name "MIT License"
            :url "https://github.com/imalooney/t3tr0s/blob/master/LICENSE"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [hiccups "0.3.0"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.0-SNAPSHOT"]
                 [cljsjs/jquery "2.1.4-0"]]

  :plugins [[lein-cljsbuild "1.1.3"]]

  :source-paths ["src"]

  :clean-targets ["public/js/client.js"
                  "public/js/client.min.js"
                  "server.js"]

  :cljsbuild
    {:builds
      [{:id "client"
        :source-paths ["src/client"]
        :compiler {:output-to "public/js/client.js"
                   :output-dir "public/out"
                   :optimizations :whitespace}}

       {:id "client-adv"
        :source-paths ["src/client"]
        :compiler {:externs ["externs/velocity.js" "externs/socket.io.js"]
                   :output-to "public/js/client.min.js"
                   :optimizations :advanced
                   :pretty-print false}}

       {:id "server"
        :source-paths ["src/server"]
        :compiler {:language-in :ecmascript5
                   :language-out :ecmascript5
                   :target :nodejs
                   :output-to "server.js"
                   :optimizations :simple}}]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :injections [(require 'weasel.repl.websocket)
               (def brepl #(cemerick.piggieback/cljs-repl :repl-env
                             (weasel.repl.websocket/repl-env)))])
