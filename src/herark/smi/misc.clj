(ns herark.smi.misc
  (:require [schema.core :as s]))

(def ^:const TWO_31_MINUS_1 2147483647)
(def ^:const MINUS_TWO_31 -2147483648)
(def ^:const TWO_32_MINUS_1 4294967295)
(def ^:const TWO_64_MINUS_1 18446744073709551615)
(def ^:const MINUS_TWO_7 -128)
(def ^:const TWO_8_MINUS_1 255)
(def ^:const TWO_16_MINUS_1 65535)
(def ^:const IP_RE
  (re-pattern
    "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"))

(defn tag-value-pair
  "Generates a Prismatic Schema consisting of a pair with
  the keyword `tag` as a first member and the predicate
  `value_pred` as the condition to be satisfied by the second."
  [tag value-pred]
  (s/pair
    (s/eq tag) "tag"
    (s/pred value-pred) "value"))

(defn byte-value?
  "Returns true if `x` is an integer between -128 and 255."
  [x]
  (and
    (integer? x)
    (>= x MINUS_TWO_7)
    (<= x TWO_8_MINUS_1)))
