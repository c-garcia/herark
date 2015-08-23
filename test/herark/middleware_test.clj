(ns herark.middleware-test
  (:require [clojure.test :refer :all]
            [herark.core :as hk]
            [herark.smi.smiv1 :as smiv1]
            [herark.smi.smiv2 :as smiv2]
            [herark.middleware :refer :all]
            [taoensso.timbre :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [schema.core :as s]))

(defn- nil-handler [_])

(defn- make-v2c-testing-event
  [source-address request-id community uptime trap-oid]
  (s/with-fn-validation
    (let [vbs [(smiv2/make-sys-uptime-vb uptime) (smiv2/make-trap-id-vb trap-oid)]
          pdu (smiv2/make-v2-trap-pdu request-id [::smiv2/int32 0] [::smiv2/int32 0] vbs)
          msg (smiv2/make-v2-trap-message source-address community pdu)
          event (hk/make-trap-event (tc/to-long (t/now)) msg :reason "testing event")]
      (println event)
      event)))

(deftest on-trap-with-prefix!-v2c-test
  (let [source-address [::smiv2/ip-address "127.0.0.1"]
        request-id [::smiv2/int32 0]
        community [::smiv2/octet-string (vec (.getBytes "public"))]
        uptime [::smiv2/time-ticks 100]
        trap-oid smiv2/COLD-START-TRAP-OID
        prefix (vec (take 5 (get smiv2/COLD-START-TRAP-OID 1)))
        evt (make-v2c-testing-event source-address request-id community uptime trap-oid)
        flag (atom false)
        unset-flag (fn [& xs] (log/debug "Unsetting flag") (reset! flag false))
        set-flag (fn [& xs] (log/debug "Setting flag") (reset! flag true))]
    (testing "Middleware invokes function on a strict prefix"
      (try
        (let [sut (on-v2c-trap-with-prefix! nil-handler prefix set-flag)]
          (unset-flag)
          (sut evt)
          (is @flag "the function has been invoked and the flag was set."))
        (finally
          (unset-flag))))
    (testing "Middleware invokes function on an exact match"
      (try
        (let [sut (on-v2c-trap-with-prefix! nil-handler (get trap-oid 1) set-flag)]
          (unset-flag)
          (sut evt)
          (is @flag "the function has been invoked and the flag was set."))
        (finally
          (unset-flag))))
    (testing "Middleware does not invoke function when prefix does not match"
      (try
        (let [sut (on-v2c-trap-with-prefix! nil-handler [0 1 2] set-flag)]
          (unset-flag)
          (sut evt)
          (is (not @flag) "the function has not been invoked and the flag is not set."))
        (finally
          (unset-flag))))
    (testing "Middleware with an empty prefix matches any trap"
      (try
        (let [sut (on-v2c-trap-with-prefix! nil-handler [] set-flag)]
          (unset-flag)
          (sut evt)
          (is @flag "the function has been invoked and the flag was set."))
        (finally
          (unset-flag))))))


(defn- make-v1-generic-testing-event
  [source-address community generic-trap-type]
  (s/with-fn-validation
    (let [vbs []
          pdu (smiv1/make-v1-trap-pdu source-address
                                      generic-trap-type
                                      [::smiv1/oid [1 3 6 1 6 3 1 1 5]]
                                      [::smiv1/int 999]
                                      [::smiv1/time-ticks (tc/to-long (t/now))]
                                      vbs)
          msg (smiv1/make-v1-trap-message source-address community pdu)
          event (hk/make-trap-event (tc/to-long (t/now)) msg :reason "testing event")]
      (println event)
      event)))

(deftest on-trap-with-prefix!-v1-test
  (let [source-address [::smiv1/ip-address "192.168.0.1"]
        generic-trap-type [::smiv1/int 0]                   ;; cold-start
        community [::smiv1/octet-string (vec (map int (seq "private")))]
        evt (make-v1-generic-testing-event source-address community generic-trap-type)
        flag (atom false)
        set-flag (fn [& xs] (log/debug "setting flag") (reset! flag true))
        unset-flag (fn [& xs] (log/debug "unsetting flag") (reset! flag false))]
    (testing "Matches a generic trap X with enterprise 1.3.6.1.6.3.1.1.5.X+1"
      (let [sut (on-v1-trap-with-prefix! nil-handler [1 3 6 1 6 3 1 1 5] set-flag)]
        (try
          (unset-flag)
          (sut evt)
          (is @flag "action has been invokend and the flag was set")
          (finally
            (unset-flag)))))))
