(ns client.pages.menu
  (:require
    [client.dom :as dom]))

;;------------------------------------------------------------------------------
;; Page Initialization
;;------------------------------------------------------------------------------

(defn init! []
  (dom/set-color-background!)
  (dom/animate-to-panel 1)
  (dom/show-el! "menuContainer")
  (dom/hide-el! "loginContainer"))