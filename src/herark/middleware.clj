(ns herark.middleware
  (:require [herark.core :as hk]
            [herark.smi.smiv1 :as smiv1]
            [herark.smi.smiv2 :as smiv2]
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
                          [{:message {:version :v2c :pdu {:varbinds [_ [_ [::smiv2/oid o]]]}}}] o
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
              (do
                (log/debug "Received prefix:" (vec received-prefix) " does not match " p)
                (f e)))))
        (do
          (log/debug "Unknown event. ")
          (f e))))))

(defn on-v1-trap-with-prefix!
  [f p action]
  (s/fn [e :- TrapEvent]
    (let [trap-oid (match [e]
                          [{:message {:version :v1
                                      :pdu     {:generic-trap-type [::smiv1/int (g :guard #{0 1 2 3 4 5})]
                                                :enterprise        [::smiv1/oid [1 3 6 1 6 3 1 1 5]]}}}] [1 3 6 1 6 3 1 1 5 (inc g)]
                          [{:message {:version :v1
                                      :pdu     {:generic-trap-type  6
                                                :specific-trap-type s
                                                :enterprise         [::smiv1/oid e]}}}] (conj (vec e) s)
                          :else nil)]
      (if trap-oid
        (do
          (log/debug "V1 Trap received. OID: " trap-oid)
          (let [elems-to-take (count p)
                received-prefix (take elems-to-take trap-oid)]
            (if (= received-prefix p)
              (do
                (log/debug "Matched prefix: " (vec p))
                (action e))
              (do
                (log/debug "Received prefix:" (vec received-prefix) " does not match " p)
                (f e)))))
        (do
          (log/debug "Unknown event. ")
          (f e))))))


