(ns herark.smi.misc
  "Namespace including some miscellaneous functions and values helpful to define
  the ones representing the SMI versions. For instance, integer constants
  will be used to check the maximum valid values for some SMI types as Counters and
  Gauges."
  (:require [schema.core :as s]))

(def
  ^{:const true :doc "2**31 - 1"}
  TWO-31-MINUS-1 2147483647)
(def
  ^{:const true :doc "-2**31"}
  MINUS-TWO-31 -2147483648)
(def
  ^{:const true :doc "2**32 - 1"}
  TWO-32-MINUS-1 4294967295)
(def
  ^{:const true :doc "2**64 -1 "}
  TWO-64-MINUS-1 18446744073709551615)
(def
  ^{:const true :doc "-2**7"}
  MINUS-TWO-7 -128)
(def
  ^{:const true :doc "2**8 - 1"}
  TWO-8-MINUS-1 255)
(def
  ^{:const true :doc "2**16 - 1"}
  TWO-16-MINUS-1 65535)
(def
  ^{:const true :doc "Regular Expression that matches an IPv4 address."}
  IP-RE
  (re-pattern
    "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"))

(defn tag-value-pair
  "Generates a Prismatic Schema consisting of a pair with
  the keyword `tag` as a first member and the predicate
  `value_pred` as the condition to be satisfied by the second.

  * `tag`: (keyword) tag value that will be the first pair member.
  * `value_pred`: (predicate) a function of one argument that needs to be satisfied
      by the values that meet the schema.

   *Example:* To define values such as `[:str \"hi\"]`, we can use:

   `(tag-value-pair :str string?)` which will generate `(s/pair (s/eq :str) \"tag\" (s/pred string?) \"value\")`."
  [tag value-pred]
  (s/pair
    (s/eq tag) "tag"
    (s/pred value-pred) "value"))

(defn byte-value?
  "Is `x` an integer between -128 and 255?"
  [x]
  (and
    (integer? x)
    (>= x MINUS-TWO-7)
    (<= x TWO-8-MINUS-1)))

(defn is_port?
  "Is `x` a valid TCP/UDP port?"
  [x]
  (and
    (integer? x)
    (> x 1)
    (<= x 65535)))
