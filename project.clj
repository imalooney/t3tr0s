(defproject t3tr0s "0.1.0-SNAPSHOT"
  :description "multiplayer Tetris™ for a 30th anniversary celebration tournament"
  :url "https://github.com/imalooney/t3tr0s"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [jayq "2.5.0"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/austin "0.1.4"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds {:client {
              :source-paths ["src/client"]
              :compiler {
                :output-to "public/client.js"
                :output-dir "public/out"
                :optimizations :none
                :source-map true}}
             :server {
              :source-paths ["src/server"]
              :compiler {
                :target :nodejs
                :output-to "server.js"
                :optimizations :simple}}}}

  :injections [; Rig a (brepl) function to setup an Austin REPL and dump the url to a file.
               ; (This code is immediately executed after starting the repl.)
               ; (The url file is read by our clojurescript app so it can connect to it.)
               ; (We don't auto-execute (brepl) because we want the prompt to be colored)
               (require 'cemerick.austin.repls)
               (defn brepl []
                 (let [env (cemerick.austin/repl-env)]
                   (spit "public/repl-url" (:repl-url env))
                   (cemerick.austin.repls/cljs-repl (reset! cemerick.austin.repls/browser-repl-env env))))]
  )
