(ns herark.smi.smiv1
  (:require [schema.core :as s]
            [herark.smi.misc :refer :all]))

;; Acording to RFC1155
;; https://www.ietf.org/rfc/rfc1155.txt

(def ^:const AUTHENTICATION_FAILURE_TRAP 4)
(def ^:const COLD_START_TRAP 0)
(def ^:const ENTERPRISE_SPECIFIC_TRAP 6)
(def ^:const LINK_DOWN_TRAP 2)
(def ^:const LINK_UP_TRAP 3)
(def ^:const WARM_START_TRAP 1)

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

(def SMIv1Variable (s/either OID
                             Int
                             Gauge
                             Counter
                             TimeTicks
                             OctetString
                             Opaque
                             IPAddress))

(def SMIv1VarBind (s/pair OID "oid" SMIv1Variable "variable"))

;FIXME determine if we need NetWorkAddress
(s/defrecord V1TrapPDU [enterprise :- OctetString
                        source-address :- IPAddress
                        generic-trap-type :- Int
                        specific-trap-type :- Int
                        timestamp :- TimeTicks
                        varbinds :- [SMIv1VarBind]])

(defn make-v1-trap-pdu
  [enterprise source-address generic-trap-type specific-trap-type timestamp varbinds]
  (->V1TrapPDU enterprise
               source-address
               generic-trap-type
               specific-trap-type
               timestamp
               varbinds))

(s/defrecord V1TrapMessage [version :- (s/eq :v1)
                            source-address :- s/Str
                            community :- [s/Int]
                            pdu :- V1TrapPDU])

(defn make-v1-trap-message
  [source-address community pdu]
  (->V1TrapMessage :v1 source-address community pdu))

