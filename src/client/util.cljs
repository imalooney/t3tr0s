(ns client.util)

(defn log
  "Log a Clojure thing."
  [thing]
  (.log js/console (pr-str thing)))

(defn js-log
  "Log a JavaScript thing."
  [js-thing]
  (.log js/console js-thing))

(defn uuid []
  "Create a UUID."
  []
  (apply
   str
   (map
    (fn [x]
      (if (= x \0)
        (.toString (bit-or (* 16 (.random js/Math)) 0) 16)
        x))
    "00000000-0000-4000-0000-000000000000")))

;; TODO: replace this with goog.i18n.NumberFormat?
;; http://tinyurl.com/mekjre8
(defn format-number [n]
  (clojure.string/replace (str n) #"\B(?=(\d{3})+(?!\d))" ","))
