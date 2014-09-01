(ns client.pages.menu
  (:require
    [client.dom :as dom]
    client.html))

;;------------------------------------------------------------------------------
;; Page Initialization
;;------------------------------------------------------------------------------

(defn init! []
  (dom/set-color-background!)
  (dom/set-panel-body! 1 (client.html/menu))
  (dom/animate-to-panel 1))