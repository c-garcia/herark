(ns herark.smi.smiv2-test
  (:require [clojure.test :refer :all]
            [herark.smi.smiv2 :refer :all]
            [herark.smi.smiv2 :as smiv2]
            [herark.smi.misc :refer :all]
            [schema.core :as s]))

(deftest oid-test
  (testing "Valid OID"
    (are [x] (not (s/check OID x))
             [::smiv2/oid [0]]
             [::smiv2/oid [0 1]]
             [::smiv2/oid [0 TWO-32-MINUS-1]]
             [::smiv2/oid (repeat 128 0)]))
  (testing "Invalid OID"
    (are [x] (s/check OID x)
             nil
             []
             1
             'a
             [::smiv2/oid]
             [:other]
             [:other [0 1]]
             [::smiv2/oid [3]]
             [::smiv2/oid [0 -1]]
             [::smiv2/oid [0 (inc TWO-32-MINUS-1)]])))

(deftest int32-test
  (testing "Valid int32"
    (are [x] (not (s/check Int32 x))
             [::smiv2/int32 -1]
             [::smiv2/int32 0]
             [::smiv2/int32 1]
             [::smiv2/int32 TWO-31-MINUS-1]
             [::smiv2/int32 MINUS-TWO-31]))
  (testing "Invalid int32"
    (are [x] (s/check Int32 x)
             nil
             []
             1
             'a
             [::smiv2/int32]
             [:other]
             [:other 1]
             [::smiv2/int32 'a]
             [::smiv2/int32 "a"]
             [::smiv2/int32 TWO-32-MINUS-1])))

(deftest uint32-test
  (testing "Valid uint32"
    (are [x] (not (s/check UInt32 x))
             [::smiv2/uint32 0]
             [::smiv2/uint32 1]
             [::smiv2/uint32 TWO-32-MINUS-1]))
  (testing "Invalid uint32"
    (are [x] (s/check UInt32 x)
             nil
             []
             1
             'a
             [::smiv2/uint32]
             [:other]
             [:other 1]
             [::smiv2/uint32 'a]
             [::smiv2/uint32 "a"]
             [::smiv2/uint32 -1]
             [::smiv2/uint32 (inc TWO-32-MINUS-1)])))

(deftest gauge32-test
  (testing "Valid gauge32"
    (are [x] (not (s/check Gauge32 x))
             [::smiv2/gauge32 0]
             [::smiv2/gauge32 1]
             [::smiv2/gauge32 TWO-32-MINUS-1]))
  (testing "Invalid gauge32"
    (are [x] (s/check Gauge32 x)
             nil
             []
             1
             'a
             [::smiv2/gauge32]
             [:other]
             [:other 1]
             [::smiv2/gauge32 'a]
             [::smiv2/gauge32 "a"]
             [::smiv2/gauge32 -1]
             [::smiv2/gauge32 (inc TWO-32-MINUS-1)])))

(deftest counter32-test
  (testing "Valid counter32"
    (are [x] (not (s/check Counter32 x))
             [::smiv2/counter32 0]
             [::smiv2/counter32 1]
             [::smiv2/counter32 TWO-32-MINUS-1]))
  (testing "Invalid counter32"
    (are [x] (s/check Counter32 x)
             nil
             []
             1
             'a
             [::smiv2/counter32]
             [:other]
             [:other 1]
             [::smiv2/counter32 'a]
             [::smiv2/counter32 "a"]
             [::smiv2/counter32 -1]
             [::smiv2/counter32 (inc TWO-32-MINUS-1)])))

(deftest counter64-test
  (testing "Valid counter64"
    (are [x] (not (s/check Counter64 x))
             [::smiv2/counter64 0]
             [::smiv2/counter64 1]
             [::smiv2/counter64 TWO-64-MINUS-1]))
  (testing "Invalid counter64"
    (are [x] (s/check Counter64 x)
             nil
             []
             1
             'a
             [::smiv2/counter64]
             [:other]
             [:other 1]
             [::smiv2/counter64 'a]
             [::smiv2/counter64 "a"]
             [::smiv2/counter64 -1]
             [::smiv2/counter64 (inc TWO-64-MINUS-1)])))


(deftest time-ticks-test
  (testing "Valid time-ticks"
    (are [x] (not (s/check TimeTicks x))
             [::smiv2/time-ticks 0]
             [::smiv2/time-ticks 1]
             [::smiv2/time-ticks TWO-32-MINUS-1]))
  (testing "Invalid time-ticks"
    (are [x] (s/check TimeTicks x)
             nil
             []
             1
             'a
             [::smiv2/time-ticks]
             [:other]
             [:other 1]
             [::smiv2/time-ticks 'a]
             [::smiv2/time-ticks "a"]
             [::smiv2/time-ticks -1]
             [::smiv2/time-ticks (inc TWO-32-MINUS-1)])))

(deftest octet-string-test
  (testing "Valid octet-string"
    (are [x] (not (s/check OctetString x))
             [::smiv2/octet-string []]
             [::smiv2/octet-string [0 1 2 3 -128 255]]
             [::smiv2/octet-string (repeat TWO-16-MINUS-1 1)]))
  (testing "Invalid octet-string"
    (are [x] (s/check OctetString x)
             nil
             []
             1
             'a
             [::smiv2/octet-string]
             [:other]
             [:other []]
             [:other [1]]
             [::smiv2/octet-string nil]
             [::smiv2/octet-string [1 \a]]
             [::smiv2/octet-string [1 2 -129]]
             [::smiv2/octet-string (repeat (inc TWO-16-MINUS-1) 1)])))

(deftest ip-address-test
  (testing "Valid ip address"
    (are [x] (not (s/check IPAddress x))
             [::smiv2/ip-address "1.2.3.4"]
             [::smiv2/ip-address "0.0.0.0"]
             [::smiv2/ip-address "255.255.255.255"]
             [::smiv2/ip-address "192.168.1.1"]))
  (testing "Invalid ip address"
    (are [x] (s/check IPAddress x)
             nil
             []
             1
             'a
             "1.2.3.4"
             [:other "1.2.3.4"]
             [:other nil]
             [:other []]
             [::smiv2/ip-address ""]
             [::smiv2/ip-address "300.254.254.254"]
             [::smiv2/ip-address "1.2.3"])))

(deftest opaque-test
  (testing "Valid opaque"
    (are [x] (not (s/check Opaque x))
             [::smiv2/opaque []]
             [::smiv2/opaque [0 1 2 3 -128 255]]
             [::smiv2/opaque (repeat TWO-16-MINUS-1 1)]))
  (testing "Invalid opaque"
    (are [x] (s/check Opaque x)
             nil
             []
             1
             'a
             [::smiv2/opaque]
             [:other]
             [:other []]
             [:other [1]]
             [::smiv2/opaque nil]
             [::smiv2/opaque [1 \a]]
             [::smiv2/opaque [1 2 -129]]
             [::smiv2/opaque (repeat (inc TWO-16-MINUS-1) 1)])))

(deftest v2-variable-test
  (testing "Valid variables"
    (are [x] (not (s/check SMIv2Value x))
             [::smiv2/oid [1 2 3]]
             [::smiv2/int32 1]
             [::smiv2/uint32 1]
             [::smiv2/counter32 1]
             [::smiv2/gauge32 1]
             [::smiv2/counter64 1]
             [::smiv2/octet-string [1 2 3]]
             [::smiv2/opaque [1 2 3]]
             [::smiv2/time-ticks 1]
             [::smiv2/ip-address "192.168.1.1"])))


(deftest v2-varbind-test
  (testing "Valid varbinds"
    (are [x] (not (s/check SMIv2VarBind x))
             [[::smiv2/oid [1 2 3]] [::smiv2/oid [1 2 3]]]
             [[::smiv2/oid [1 2 3]] [::smiv2/int32 1]])))
