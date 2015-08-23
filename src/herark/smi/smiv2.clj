(ns herark.smi.smiv2
  (:require [schema.core :as s]
            [herark.smi.misc :refer :all]))

;; According to RFC-2578
;; https://tools.ietf.org/html/rfc2578

(defn oid-value?
  "Is this an SMIv2 OID?
  SMIv2 OID example: [0 1 3 4]"
  [x]
  (and
    (sequential? x)
    (not (empty? x))
    (#{0 1 2} (first x))
    (<= (count x) 128)
    (every? #(and (>= % 0) (<= % TWO_32_MINUS_1)) x)))

(def OID (tag-value-pair ::oid oid-value?))

(defn int-value?
  "Is this an SMIv2 Integer?
  SMIv2 Integers example: 232"
  [x]
  (and
    (integer? x)
    (>= x MINUS_TWO_31)
    (<= x TWO_31_MINUS_1)))

;(def Int (tag-value-pair :int int-value?)) ; type indistiguishable from int32

(defn int32-value?
  "Is this an SMIv2 Integer32?
  SMIv2 Integer32 example: -132"
  [x]
  (int-value? x))

(def Int32 (tag-value-pair ::int32 int32-value?))

(defn uint32-value?
  "Is this an SMIv2 Unsigned32?
  SMIv2 Unsigned32 example: 4322"
  [x]
  (and
    (integer? x)
    (>= x 0)
    (<= x TWO_32_MINUS_1)))

(def UInt32 (tag-value-pair ::uint32 uint32-value?))

(defn gauge32-value?
  "Is this an SMIv2 Gauge32?
  SMIv2 Gauge32 example: 10322"
  [x]
  (uint32-value? x))

(def Gauge32 (tag-value-pair ::gauge32 gauge32-value?))

(defn counter32-value?
  "Is this an SMIv2 Counte32?
  SMIv2 Counter32 example: 94383"
  [x]
  (uint32-value? x))

(def Counter32 (tag-value-pair ::counter32 counter32-value?))

(defn counter64-value?
  "Is this an SMIv2 Counter64?
  SMIv2 Counter64 example: 4550402"
  [x]
  (and
    (integer? x)
    (>= x 0)
    (<= x TWO_64_MINUS_1)))

(def Counter64 (tag-value-pair ::counter64 counter64-value?))

(defn time-ticks-value?
  "Is this an SMIv2 TimeTicks?
  SMIv2 TimeTicks example: 32329843"
  [x]
  (uint32-value? x))

(def TimeTicks (tag-value-pair ::time-ticks time-ticks-value?))

(defn octet-string-value?
  "Is this an SMIv2 OctetString?
  SMIv2 OctetString example: [1 -128 255]"
  [x]
  (and
    (sequential? x)
    (<= (count x) TWO_16_MINUS_1)
    (every? byte-value? x)))

(def OctetString (tag-value-pair ::octet-string octet-string-value?))

(defn ip-address-value?
  "Is this an SMIv2 Ip Address?
  SMIv2 Ip Address example: \"192.168.1.1\""
  [x]
  (and
    (string? x)
    (re-matches IP_RE x)))

(def IPAddress (tag-value-pair ::ip-address ip-address-value?))

(defn opaque-value?
  "Is this an SMIv2 Opaque?
  SMIv2 Opaque example: [1 2 3 255]"
  [x]
  (octet-string-value? x))

(def Opaque (tag-value-pair ::opaque opaque-value?))

(def SMIv2Value (s/either
                  OID
                  Int32
                  UInt32
                  Counter32
                  Gauge32
                  Counter64
                  OctetString
                  Opaque
                  TimeTicks
                  IPAddress))

(def SMIv2VarBind (s/pair OID "oid" SMIv2Value "variable"))

(s/defrecord V2cTrapPDU [request-id :- Int32
                         error-status :- Int32
                         error-index :- Int32
                         varbinds :- [SMIv2VarBind]])

(s/defn make-v2-trap-pdu :- V2cTrapPDU
  [request-id :- Int32
   error-status :- Int32
   error-index :- Int32
   varbinds :- [SMIv2VarBind]]
  (->V2cTrapPDU request-id error-status error-index varbinds))

(s/defrecord V2cTrapMessage [version :- (s/eq :v2c)
                             source-address :- IPAddress
                             community :- OctetString
                             pdu :- V2cTrapPDU])
(s/defn make-v2-trap-message :- V2cTrapMessage
  [source-address :- IPAddress
   community :- OctetString
   pdu :- V2cTrapPDU]
  (->V2cTrapMessage :v2c source-address community pdu))

(def ^:const COLD-START-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 1]])
(def ^:const WARM-START-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 2]])
(def ^:const LINK-DOWN-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 3]])
(def ^:const LINK-UP-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 4]])
(def ^:const AUTH-FAIL-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 5]])
(def ^:const EGP-NEIGH-LOSS-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 6]])
(def ^:const SYS-UPTIME-OID [::oid [1 3 6 1 2 1 1 3 0]])
(def ^:const TRAP-ID-OID [::oid [1 3 6 1 6 3 1 1 4 1 0]])

(s/defn make-sys-uptime-vb :- SMIv2VarBind
  [t :- TimeTicks]
  [SYS-UPTIME-OID t])

(s/defn make-trap-id-vb :- SMIv2VarBind
  [id :- OID]
  [TRAP-ID-OID id])

;; TODO Introduce v3 support
(s/defrecord V3TrapPDU [])
(s/defrecord V3TrapMessage [])

