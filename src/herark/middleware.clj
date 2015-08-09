(ns herark.middleware
  (:require [herark.core :as hk]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(defn check-static-community!
  "Creates a middleware function that, for each received trap checks the SNMP community `c`. If the community
  matches, the processing continues to the next function. If not, it returns."
  [f c]
  (s/fn [e :- hk/SnmpV2CTrapInfo]
    (if (= (:community e) c)
      (do
        (log/debug "Community ok")
        (f e))
      (log/warn "Community error" e))))
