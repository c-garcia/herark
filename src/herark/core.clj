(ns herark.core
  (:require [schema.core :as s]
            [herark.smi.smiv1 :as smiv1]
            [herark.smi.smiv2 :as smiv2]))



(def TaggedSMI (s/pair s/Keyword "tag" s/Any "value"))

(def VarBindV2 (s/pair smiv2/OID "oid" (s/either TaggedSMI smiv2/OID) "variable"))

(def SnmpV2CTrapInfo {(s/required-key :version)        (s/eq :v2c)
                      (s/required-key :source-address) s/Str
                      (s/required-key :source-port)    (s/both s/Int (s/pred (fn [x] (and (< x 65336) (> x 0)))))
                      (s/required-key :protocol)       (s/enum :tcp :udp)
                      (s/required-key :community)      [s/Int]
                      (s/required-key :request-id)     s/Int
                      (s/required-key :error-status)   s/Int
                      (s/required-key :error-index)    s/Int
                      (s/required-key :varbinds)       [VarBindV2]
                      (s/required-key :timestamp)      long
                      s/Keyword                        s/Any})



