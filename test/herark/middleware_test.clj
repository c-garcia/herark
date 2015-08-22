(ns herark.middleware-test
  (:require [clojure.test :refer :all]
            [herark.core :as hk]
            [herark.smi.smiv1 :as smiv1]
            [herark.smi.smiv2 :as smiv2]
            [herark.middleware :refer :all]
            [taoensso.timbre :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]))

(defn- nil-handler [_])

(defn- make-v2c-testing-event
  [source-address request-id community uptime trap-oid ]
  (let [vbs [(smiv2/make-sysuptime-vb 0) (smiv2/make-trapid-vb trap-oid)]
        pdu (smiv2/make-v2-trap-pdu request-id vbs)
        msg (smiv2/make-v2-trap-message source-address community pdu)
        event (hk/make-trap-event uptime msg :reason "testing event" )]
    event))

(deftest on-trap-with-prefix!-v2c-test
  (let [source-address "127.0.0.1"
        request-id 0
        community (vec (.getBytes "public"))
        uptime (tc/to-long (t/now))
        trap-oid smiv2/COLD-START-TRAP-OID
        prefix (vec (take 5 (get smiv2/COLD-START-TRAP-OID 1)))
        evt (make-v2c-testing-event source-address request-id community uptime (get trap-oid 1))
        flag (atom false)
        unset-flag (fn [& xs] (log/debug "Unsetting flag")(reset! flag false))
        set-flag (fn [& xs] (log/debug "Setting flag") (reset! flag true))]
    (testing "Middleware invokes function on a strict prefix"
      (try
        (let [sut (on-trap-with-prefix! nil-handler prefix set-flag)]
          (unset-flag)
          (sut evt)
          (is @flag "the function has been invoked and the flag was set."))
        (finally
          (unset-flag))))
    (testing "Middleware invokes function on an exact match"
      (try
        (let [sut (on-trap-with-prefix! nil-handler (get trap-oid 1) set-flag)]
          (unset-flag)
          (sut evt)
          (is @flag "the function has been invoked and the flag was set."))
        (finally
          (unset-flag))))
    (testing "Middleware does not invoke function when prefix does not match"
      (try
        (let [sut (on-trap-with-prefix! nil-handler [0 1 2] set-flag)]
          (unset-flag)
          (sut evt)
          (is (not @flag) "the function has not been invoked and the flag is not set."))
        (finally
          (unset-flag))))
    (testing "Middleware with an empty prefix matches any trap"
      (try
        (let [sut (on-trap-with-prefix! nil-handler [] set-flag)]
          (unset-flag)
          (sut evt)
          (is @flag "the function has been invoked and the flag was set."))
        (finally
          (unset-flag))))))

