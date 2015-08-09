(ns herark.handlers
  (:require [herark.core :as hk]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(defn log-trap!
  "Generates a handler function that logs traps with the specified log `level`. It prepends `message`"
  [level message]
  (s/fn
    [e :- hk/SnmpV2CTrapInfo]
    (log/log level message)
    (log/log level e)))
