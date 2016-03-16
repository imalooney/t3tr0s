(ns client.util
  (:require
    [clojure.string :refer [replace]]))

(defn js-log
  "Log a JavaScript thing."
  [js-thing]
  (js/console.log js-thing))

(defn log
  "Log a Clojure thing."
  [clj-thing]
  (js-log (pr-str clj-thing)))

;; TODO: replace this with goog.i18n.NumberFormat?
;; http://tinyurl.com/mekjre8
(defn format-number [n]
  (replace (str n) #"\B(?=(\d{3})+(?!\d))" ","))

(defn seconds->time-str
  "Converts seconds to mm:ss format for display"
  [seconds]
  (let [m (js/Math.floor (/ seconds 60))
        s (mod seconds 60)
        s-str (if (< s 10) (str "0" s) s)]
    (str m ":" s-str)))
