(ns client.state
  (:require
    [client.util :as util]))

;;------------------------------------------------------------------------------
;; State Atoms
;;------------------------------------------------------------------------------

(def chat (atom []))
(def time-left (atom 0))
(def players (atom {}))