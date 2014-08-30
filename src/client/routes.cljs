(ns client.routes
  (:require
    [client.pages.lobby :as lobby-page]
    [client.pages.login :as login-page]
    [client.pages.menu :as menu-page]
    [client.pages.mc :as mc-page]
    [client.pages.play :as play-page]
    [client.pages.spectate :as spectate-page]
    [client.util :as util]))

(def function? js/goog.isFunction)

;;------------------------------------------------------------------------------
;; Routes
;;------------------------------------------------------------------------------

(def default-route "/menu")

(def routes {
  ;; initial page + login
  "/menu"  menu-page/init!
  "/login" login-page/init!

  ;; solo play screen
  "/play-solo" [play-page/init-solo! play-page/cleanup!]

  ;; the "game loop" screens
  "/lobby" [lobby-page/init! lobby-page/cleanup!]
  "/play"  [play-page/init-battle! play-page/cleanup!]
  "/spectate" [spectate-page/init! spectate-page/cleanup!]

  ;; MC / admin
  "/mc" [mc-page/init! mc-page/cleanup!]

  ;; experimental - work in progress
  "/play2" [play-page/init-battle2! play-page/cleanup!]

  ;; redirects - where is my HTTP 301? :)
  "/dashboard" #(aset js/document "location" "hash" "/spectate")
  })

(def previous-page-cleanup-fn (atom nil))

(defn- on-hash-change []
  (let [new-route (.replace (aget js/document "location" "hash") #"^#" "")
        page (get routes new-route)
        init-fn (if (vector? page) (first page) page)
        cleanup-fn (if (vector? page) (second page))]
    (if-not (function? init-fn)
      (aset js/document "location" "hash" default-route)
      (do
        ;; run cleanup function from the last page
        (if (and @previous-page-cleanup-fn
                 (function? @previous-page-cleanup-fn))
          (@previous-page-cleanup-fn))

        ;; run init function for the new page
        (init-fn)

        ;; store cleanup function for next hash change
        (reset! previous-page-cleanup-fn cleanup-fn)))))

;;------------------------------------------------------------------------------
;; Routes Init
;;------------------------------------------------------------------------------

(def initialized? (atom false))

;; NOTE: this "run-once" function should be called on global app init
(defn init! []
  (when-not @initialized?
    ;; add the event handler
    (aset js/window "onhashchange" on-hash-change)

    ;; kick off the initial page
    (on-hash-change)

    ;; toggle initialized flag
    (reset! initialized? true)))