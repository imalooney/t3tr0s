(ns server.util)

(def moment (js/require "moment"))

(defn log
  "Log a Clojure thing."
  [thing]
  (.log js/console (pr-str thing)))

(defn js-log
  "Log a JavaScript thing."
  [& js-things]
  (apply (.-log js/console) js-things))

(defn- now []
  (.format (moment) "YYYY-MM-DD HH:mm:ss"))

;; TODO: investigate using a proper logging library
;; - https://github.com/flatiron/winston
;; - https://github.com/trentm/node-bunyan
(defn tlog
  "Timestampped log."
  [& msgs]
  (js-log (str "[" (now) "] " (apply str msgs))))