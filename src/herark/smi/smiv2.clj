(ns herark.smi.smiv2

  "SMIv2 types defined as Prismatic Schema schemas. They are present in SNMPv2c and SNMPv3
  protocol messages.

  The same as the SMIv1 elements, they can be classified into:

  * Types for SMI Values (SMIv2Value): Clojure tag-value pairs.
  * VarBinds (SMIv2cVarBind): pairs of SMI Values being the first an OID.
  * SNMPv2c PDU (V2cTrapPDU): Includes the SNMPv2c Trap PDU fields as defined by the standards.
  * SNMPv2c Message (V2cTrapMessage): Includes the source address, the community and a PDU.
  * SNMPv3 suppor will be added in the future.

  ### Interesting functions

  The main functions are:

  * `make-v2c-trap-pdu`: Creates a PDU accorinding to the SNMPv2c standard.
  * `make-v2c-trap-message`: Creates a message including a SNMPv2c PDU.
  * `make-sys-uptime-vb`: Creates a SysUptime Variable Binding.
  * `make-trap-id-vb`: Creates a Trap ID Variable binding.

  ### Misc vars

  This namespace includes constants for the SNMPv2 equivalents of the SNMPv1 generic traps and
  for some well-known OIDs."

  (:require [schema.core :as s]
            [herark.smi.misc :refer :all]))

;; According to RFC-2578
;; https://tools.ietf.org/html/rfc2578

(defn oid-value?
  "Is this an SMIv2 OID?

  SMIv2 OID example: `[0 1 3 4]`"
  [x]
  (and
    (sequential? x)
    (not (empty? x))
    (#{0 1 2} (first x))
    (<= (count x) 128)
    (every? #(and (>= % 0) (<= % TWO-32-MINUS-1)) x)))

(s/defschema
  OID
  "Schema for SMIv2 OID."
  (tag-value-pair ::oid oid-value?))

(defn int-value?
  "Is this an SMIv2 Integer?

  SMIv2 Integers example: `232`"
  [x]
  (and
    (integer? x)
    (>= x MINUS-TWO-31)
    (<= x TWO-31-MINUS-1)))

;(def Int (tag-value-pair :int int-value?)) ; type indistiguishable from int32

(defn int32-value?
  "Is this an SMIv2 Integer32?

  SMIv2 Integer32 example: `-132`"
  [x]
  (int-value? x))

(s/defschema
  Int32
  "Schema for SMIv2 Integer32."
  (tag-value-pair ::int32 int32-value?))

(defn uint32-value?
  "Is this an SMIv2 Unsigned32?

  SMIv2 Unsigned32 example: `4322`"
  [x]
  (and
    (integer? x)
    (>= x 0)
    (<= x TWO-32-MINUS-1)))

(s/defschema UInt32
  "Schema for SMIv2 Unsigned Integer32."
  (tag-value-pair ::uint32 uint32-value?))

(defn gauge32-value?
  "Is this an SMIv2 Gauge32?

  SMIv2 Gauge32 example: `10322`"
  [x]
  (uint32-value? x))

(s/defschema
  Gauge32
  "Schema for SMIv2 Gauge32."
  (tag-value-pair ::gauge32 gauge32-value?))

(defn counter32-value?
  "Is this an SMIv2 Counte32?

  SMIv2 Counter32 example: `94383`"
  [x]
  (uint32-value? x))

(s/defschema
  Counter32
  "Schema for SMIv2 Counter32."
  (tag-value-pair ::counter32 counter32-value?))

(defn counter64-value?
  "Is this an SMIv2 Counter64?

  SMIv2 Counter64 example: `4550402`"
  [x]
  (and
    (integer? x)
    (>= x 0)
    (<= x TWO-64-MINUS-1)))

(s/defschema
  Counter64
  "Schema for SMIv2 Counter64."
  (tag-value-pair ::counter64 counter64-value?))

(defn time-ticks-value?
  "Is this an SMIv2 TimeTicks?

  SMIv2 TimeTicks example: `32329843`"
  [x]
  (uint32-value? x))

(s/defschema
  TimeTicks
  "Schema for SMIv2 TimeTicks."
  (tag-value-pair ::time-ticks time-ticks-value?))

(defn octet-string-value?
  "Is this an SMIv2 OctetString?

  SMIv2 OctetString example: `[1 -128 255]`"
  [x]
  (and
    (sequential? x)
    (<= (count x) TWO-16-MINUS-1)
    (every? byte-value? x)))

(s/defschema
  OctetString
  "Schema for SMIv2 OctetString."
  (tag-value-pair ::octet-string octet-string-value?))

(defn ip-address-value?
  "Is this an SMIv2 Ip Address?

  SMIv2 Ip Address example: `\"192.168.1.1\"`"
  [x]
  (and
    (string? x)
    (re-matches IP-RE x)))

(s/defschema
  IPAddress
  "Schema for SMIv2 IP Address."
  (tag-value-pair ::ip-address ip-address-value?))

(defn opaque-value?
  "Is this an SMIv2 Opaque?

  SMIv2 Opaque example: `[1 2 3 255]`"
  [x]
  (octet-string-value? x))

(s/defschema
  Opaque
  "Schema for SMIv2 Opaque."
  (tag-value-pair ::opaque opaque-value?))

(s/defschema
  SMIv2Value
  "Schema for SMIv2 possible values."
  (s/either
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

(s/defschema
  SMIv2VarBind
  "Schema for SMIv2 Variable Bindings."
  (s/pair OID "oid" SMIv2Value "variable"))

(s/defschema V2cTrapPDU
  "Schema for an SNMP v2c Trap PDU (so it does not include the PDU type).
  It includes:

  * `:request-id`:
  * `:error-status`: SNMP error estatus, normally 0.
  * `:error-index`: Index of the VarBind that caused the error.
  * `varbinds`: Variable bindings including timestamp and trap type."
  {(s/required-key :request-id)   Int32
   (s/required-key :error-status) Int32
   (s/required-key :error-index)  Int32
   (s/required-key :varbinds)     [SMIv2VarBind]})

(s/defn make-v2-trap-pdu :- V2cTrapPDU
  "Creates a SNMP v2c Trap PDU from their SMIv2 constituents. For more
  information on the parameters, please, have a look at the V2cTrapPDUS schema."
  [request-id :- Int32
   error-status :- Int32
   error-index :- Int32
   varbinds :- [SMIv2VarBind]]
  {:request-id   request-id
   :error-status error-status
   :error-index  error-index
   :varbinds     varbinds})

(s/defschema V2cTrapMessage
  "Schema for an SNMP v2c Trap Message. It includes:

  * `:source-address`: the ip address the message is coming from.
  * `:community`: the community string.
  * `:pdu`: the PDU."
  {(s/required-key :version)        (s/eq :v2c)
   (s/required-key :source-address) IPAddress
   (s/required-key :community)      OctetString
   (s/required-key :pdu)            V2cTrapPDU})

(s/defn make-v2-trap-message :- V2cTrapMessage
  [source-address :- IPAddress
   community :- OctetString
   pdu :- V2cTrapPDU]
  {:version        :v2c
   :source-address source-address
   :community      community
   :pdu            pdu})

(def
  ^{:const true :doc "Cold start trap"}
  COLD-START-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 1]])
(def
  ^{:const true :doc "Warm start trap"}
  WARM-START-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 2]])
(def
  ^{:const true :doc "Link down trap"}
  LINK-DOWN-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 3]])
(def
  ^{:const true :doc "Link up trap"}
  LINK-UP-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 4]])
(def
  ^{:const true :doc "Authentication failed trap"}
  AUTH-FAIL-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 5]])
(def
  ^{:const true :doc "EGP neighbour lost trap"}
  EGP-NEIGH-LOSS-TRAP-OID [::oid [1 3 6 1 6 3 1 1 5 6]])
(def
  ^{:const true :doc "Sys uptime OID."}
  SYS-UPTIME-OID [::oid [1 3 6 1 2 1 1 3 0]])
(def
  ^{:const true :doc "Trap id OID."}
  TRAP-ID-OID [::oid [1 3 6 1 6 3 1 1 4 1 0]])

(s/defn make-sys-uptime-vb :- SMIv2VarBind
  "Creates a sys-uptime varbind as defined in SMIv2."
  [t :- TimeTicks]
  [SYS-UPTIME-OID t])

(s/defn make-trap-id-vb :- SMIv2VarBind
  "Creates a trap-id varbind as defined in SMIv2."
  [id :- OID]
  [TRAP-ID-OID id])

;; TODO Introduce v3 support
(s/defrecord V3TrapPDU [])
(s/defrecord V3TrapMessage [])

