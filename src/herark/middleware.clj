(ns herark.middleware
  (:require [herark.core :as hk]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [clojure.core.match :refer [match]])
  (:import [herark.core TrapEvent]))

(defn check-static-community!
  "Creates a middleware function that, for each received trap checks the SNMP community `c`. If the community
  matches, the processing continues to the next function. If not, it returns."
  [f c]
  (s/fn [e :- TrapEvent]
    (if (= (get-in e [:message :community]) c)
      (do
        (log/debug "Community ok")
        (f e))
      (log/warn "Community error" e))))

(defn on-v2c-trap-with-prefix!
  "Creates a middleware function that, if a received v2c trap matches an OID prefix `p`, it
  executes a function. If not, it continues the processing chain."
  [f p action]
  (s/fn [e :- TrapEvent]
    (let [trap-oid (match [e]
                          [{:message {:version :v2c :pdu {:varbinds [_ [_ [:oid o]]]}}}] o
                          :else nil)]
      (if trap-oid
        (do
          (log/debug "V2C Trap received. OID: " trap-oid)
          (let [elems-to-take (count p)
                received-prefix (take elems-to-take trap-oid)]
            (if (= received-prefix p)
              (do
                (log/debug "Matched prefix: " (vec p))
                (action e))
              (log/debug "Received prefix:" (vec received-prefix) " does not match " p))))
        (do
          (log/debug "Unknown event. ")
          (f e))))))


