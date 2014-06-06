(ns server.util)

(defn log
  "Log a Clojure thing."
  [thing]
  (.log js/console (pr-str thing)))

(defn js-log
  "Log a JavaScript thing."
  [& js-things]
  (apply (.-log js/console) js-things))

(defn- now []
  (.toTimeString (js/Date.)))

;; TODO: better timestamp format here
(defn tlog
  "Timestampped log."
  [msg]
  (js-log (str (now) " - " msg)))