(ns herark.core
  (:require [schema.core :as s]))

(def OID (s/pair (s/eq :oid) "tag" [s/Int] "oid"))

(def TaggedSMI (s/pair s/Keyword "tag" s/Any "value"))

(def VarBind (s/pair OID "oid" TaggedSMI "variable"))

(def SnmpV2CTrapInfo {(s/required-key :version) (s/eq :v2c)
                      (s/required-key :source-address) s/Str
                      (s/required-key :source-port) (s/both s/Int (s/pred (fn [x] (and (< x 65336) (> x 0)))))
                      (s/required-key :protocol) (s/enum :tcp :udp)
                      (s/required-key :community) [s/Int]
                      (s/required-key :request-id) s/Int
                      (s/required-key :error-status) s/Int
                      (s/required-key :error-index) s/Int
                      (s/required-key :varbinds) [VarBind]
                      (s/required-key :timestamp) long
                      s/Keyword s/Any})



