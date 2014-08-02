(ns html
  (:require-macros [hiccups.core :as hiccups])
  (:require
    [hiccups.runtime :as hiccupsrt]))

(hiccups/defhtml head [page-title]
  "<!doctype html>"
  "<html>"
  [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
    [:title page-title]
    [:meta {:name "viewport" :content "width=device-width"}]
    [:link {:rel "stylesheet" :href "/css/t3tr0s.min.css"}]]
  "<body>")

;; NOTE: add other config items here as needed
(defn- client-config []
  (js-obj
    "use-repl" (boolean (:use-repl server.core.config))))

(hiccups/defhtml footer []
  [:script {:src "/socket.io/socket.io.js" :type "text/javascript"}]
  [:script {:src "/js/jquery-1.11.1.min.js" :type "text/javascript"}]
  [:script "window.T3TR0S_CONFIG = " (.stringify js/JSON (client-config)) ";"]
  (if (:minified-client server.core.config)
    [:script {:src "/js/client.min.js" :type "text/javascript"}]
    [:script {:src "/js/client.js" :type "text/javascript"}])
  "</body>"
  "</html>")

(hiccups/defhtml page-shell []
  (head "T3TR0S")
  [:div#main-container]
  (footer))
