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
	  [:link {:rel "stylesheet" :href "css/t3tr0s.css"}]]
	"<body>")

(hiccups/defhtml footer [js-string]
	[:script {:src "/socket.io/socket.io.js" :type "text/javascript"}]
	[:script {:src "jquery-1.11.1.min.js" :type "text/javascript"}]
	[:script {:src "out/goog/base.js" :type "text/javascript"}]
	[:script {:src "client.js" :type "text/javascript"}]
	[:script {:type "text/javascript"} "goog.require('client.core')"]
  [:script js-string]
	"</body>"
	"</html>")

(hiccups/defhtml page-shell []
	(head "T3TR0S")
	[:div#main-container]
	(footer "client.html.homeInit();"))