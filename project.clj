(defproject eu.obliquo/herark "0.1.10-SNAPSHOT"
  :description "An SNMP Trap forwarding and management tool"
  :url "https://github.com/c-garcia/herark"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories { "snmpj" "https://oosnmp.net/dist/release" }
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [org.snmp4j/snmp4j "2.3.4"]
                 [com.taoensso/timbre "4.0.2"]
                 [slingshot "0.12.2"]
                 [clj-time "0.10.0"]
                 [environ "1.0.0"]
                 [commons-daemon/commons-daemon "1.0.15"]
                 [prismatic/schema "0.4.3"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/test.check "0.7.0"]
                 [schema-gen "0.1.5"]]
  :plugins [[lein-environ "1.0.0"]
            [codox "0.8.13"]]
  :codox {:output-dir "target/codox"
          :defaults {:doc/format :markdown}}
  :main herark.svc
  :profiles {:dev {:env {:msg "TESTING ENV"}}
             :uberjar {:aot :all}})
