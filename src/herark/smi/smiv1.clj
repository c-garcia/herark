(ns herark.smi.smiv1
  (:require [schema.core :as s]
            [herark.smi.misc :refer :all]))

;; Acording to RFC1155
;; https://www.ietf.org/rfc/rfc1155.txt

(defn oid-value?
  "Is `x` a valid oid"
  [x]
  (and
    (sequential? x)
    (not (empty? x))
    (#{0 1 2} (first x))
    (every? (fn [y]
              (and (integer? y) (>= y 0))) x)))

(def OID (tag-value-pair :oid oid-value?))

(defn int-value?
  [x]
  (and
    (integer? x)))

(def Int (tag-value-pair :int int-value?))

(defn counter-value?
  [x]
  (and
    (integer? x)
    (>= x 0)
    (<= x TWO_32_MINUS_1)))

(def Counter (tag-value-pair :counter counter-value?))

(defn gauge-value?
  [x]
  (and
    (integer? x)
    (>= x 0)
    (<= x TWO_32_MINUS_1)))

(def Gauge (tag-value-pair :gauge gauge-value?))

(defn time-ticks-value?
  [x]
  (and
    (integer? x)
    (>= x 0)))

(def TimeTicks (tag-value-pair :time-ticks time-ticks-value?))

(defn octet-string-value?
  [x]
  (and
    (sequential? x)
    (every? byte-value? x)))

(def OctetString (tag-value-pair :octet-string octet-string-value?))

(defn opaque-value?
  [x]
  (octet-string-value? x))

(def Opaque (tag-value-pair :opaque opaque-value?))

(defn ip-address-value?
  "Is this an SMIv2 Ip Address?
  SMIv2 Ip Address example: \"192.168.1.1\""
  [x]
  (and
    (string? x)
    (re-matches IP_RE x)))

(def IPAddress (tag-value-pair :ip-address ip-address-value?))

;FIXME determine if we need NetWorkAddress
