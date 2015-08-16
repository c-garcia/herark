(ns herark.smi.smiv2-test
  (:require [clojure.test :refer :all]
            [herark.smi.smiv2 :refer :all]
            [herark.smi.misc :refer :all]
            [schema.core :as s]))

(deftest oid-test
  (testing "Valid OID"
    (are [x] (not (s/check OID x))
             [:oid [0]]
             [:oid [0 1]]
             [:oid [0 TWO_32_MINUS_1]]
             [:oid (repeat 128 0)]))
  (testing "Invalid OID"
    (are [x] (s/check OID x)
             nil
             []
             1
             'a
             [:oid]
             [:other]
             [:other [0 1]]
             [:oid [3]]
             [:oid [0 -1]]
             [:oid [0 (inc TWO_32_MINUS_1)]])))

(deftest int32-test
  (testing "Valid int32"
    (are [x] (not (s/check Int32 x))
             [:int32 -1]
             [:int32 0]
             [:int32 1]
             [:int32 TWO_31_MINUS_1]
             [:int32 MINUS_TWO_31]))
  (testing "Invalid int32"
    (are [x] (s/check Int32 x)
             nil
             []
             1
             'a
             [:int32]
             [:other]
             [:other 1]
             [:int32 'a]
             [:int32 "a"]
             [:int32 TWO_32_MINUS_1])))

(deftest uint32-test
  (testing "Valid uint32"
    (are [x] (not (s/check UInt32 x))
             [:uint32 0]
             [:uint32 1]
             [:uint32 TWO_32_MINUS_1]))
  (testing "Invalid uint32"
    (are [x] (s/check UInt32 x)
             nil
             []
             1
             'a
             [:uint32]
             [:other]
             [:other 1]
             [:uint32 'a]
             [:uint32 "a"]
             [:uint32 -1]
             [:uint32 (inc TWO_32_MINUS_1)])))

(deftest gauge32-test
  (testing "Valid gauge32"
    (are [x] (not (s/check Gauge32 x))
             [:gauge32 0]
             [:gauge32 1]
             [:gauge32 TWO_32_MINUS_1]))
  (testing "Invalid gauge32"
    (are [x] (s/check Gauge32 x)
             nil
             []
             1
             'a
             [:gauge32]
             [:other]
             [:other 1]
             [:gauge32 'a]
             [:gauge32 "a"]
             [:gauge32 -1]
             [:gauge32 (inc TWO_32_MINUS_1)])))

(deftest counter32-test
  (testing "Valid counter32"
    (are [x] (not (s/check Counter32 x))
             [:counter32 0]
             [:counter32 1]
             [:counter32 TWO_32_MINUS_1]))
  (testing "Invalid counter32"
    (are [x] (s/check Counter32 x)
             nil
             []
             1
             'a
             [:counter32]
             [:other]
             [:other 1]
             [:counter32 'a]
             [:counter32 "a"]
             [:counter32 -1]
             [:counter32 (inc TWO_32_MINUS_1)])))

(deftest counter64-test
  (testing "Valid counter64"
    (are [x] (not (s/check Counter64 x))
             [:counter64 0]
             [:counter64 1]
             [:counter64 TWO_64_MINUS_1]))
  (testing "Invalid counter64"
    (are [x] (s/check Counter64 x)
             nil
             []
             1
             'a
             [:counter64]
             [:other]
             [:other 1]
             [:counter64 'a]
             [:counter64 "a"]
             [:counter64 -1]
             [:counter64 (inc TWO_64_MINUS_1)])))


(deftest time-ticks-test
  (testing "Valid time-ticks"
    (are [x] (not (s/check TimeTicks x))
             [:time-ticks 0]
             [:time-ticks 1]
             [:time-ticks TWO_32_MINUS_1]))
  (testing "Invalid time-ticks"
    (are [x] (s/check TimeTicks x)
             nil
             []
             1
             'a
             [:time-ticks]
             [:other]
             [:other 1]
             [:time-ticks 'a]
             [:time-ticks "a"]
             [:time-ticks -1]
             [:time-ticks (inc TWO_32_MINUS_1)])))

(deftest octet-string-test
  (testing "Valid octet-string"
    (are [x] (not (s/check OctetString x))
             [:octet-string []]
             [:octet-string [0 1 2 3 -128 255]]
             [:octet-string (repeat TWO_16_MINUS_1 1)]))
  (testing "Invalid octet-string"
    (are [x] (s/check OctetString x)
             nil
             []
             1
             'a
             [:octet-string]
             [:other]
             [:other []]
             [:other [1]]
             [:octet-string nil]
             [:octet-string [1 \a]]
             [:octet-string [1 2 -129]]
             [:octet-string (repeat (inc TWO_16_MINUS_1) 1)])))

(deftest ip-address-test
  (testing "Valid ip address"
    (are [x] (not (s/check IPAddress x))
             [:ip-address "1.2.3.4"]
             [:ip-address "0.0.0.0"]
             [:ip-address "255.255.255.255"]
             [:ip-address "192.168.1.1"]))
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
             [:ip-address ""]
             [:ip-address "300.254.254.254"]
             [:ip-address "1.2.3"])))

(deftest opaque-test
  (testing "Valid opaque"
    (are [x] (not (s/check Opaque x))
             [:opaque []]
             [:opaque [0 1 2 3 -128 255]]
             [:opaque (repeat TWO_16_MINUS_1 1)]))
  (testing "Invalid opaque"
    (are [x] (s/check Opaque x)
             nil
             []
             1
             'a
             [:opaque]
             [:other]
             [:other []]
             [:other [1]]
             [:opaque nil]
             [:opaque [1 \a]]
             [:opaque [1 2 -129]]
             [:opaque (repeat (inc TWO_16_MINUS_1) 1)])))

(deftest v2-variable-test
  (testing "Valid variables"
    (are [x] (not (s/check SMIv2Variable x))
             [:oid [1 2 3]]
             [:int32 1]
             [:uint32 1]
             [:counter32 1]
             [:gauge32 1]
             [:counter64 1]
             [:octet-string [1 2 3]]
             [:opaque [1 2 3]]
             [:time-ticks 1]
             [:ip-address "192.168.1.1"])))


(deftest v2-varbind-test
  (testing "Valid varbinds"
    (are [x] (not (s/check SMIv2VarBind x))
             [[:oid [1 2 3]] [:oid [1 2 3]]]
             [[:oid [1 2 3]] [:int32 1]])))
