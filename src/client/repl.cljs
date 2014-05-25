(ns client.repl
  (:require
    [clojure.browser.repl :as repl]))

(def $ js/$)

(defn connect []
  (.ajax $ #js {:url "repl-url"
                :cache false
                :dataType "text"
                :success #(repl/connect %)}))

