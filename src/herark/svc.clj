(ns herark.svc
  "A sample implementation of an SNMP Trap Processor that uses the SNMP4J Adapter.
  At the moment, it just logs the received events using the `timbre` library at
  the debug level.

  To interface with the OS service management facilities it implements
  `apache.commons.daemon.Daemon`."
  (:require [herark.adapters.snmp4j :refer [snmp-v2c-trap-processor]]
            [herark.handlers :as hn]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log])
  (:import [org.apache.commons.daemon Daemon DaemonContext])
  (:gen-class
    :implements [org.apache.commons.daemon.Daemon]))

(def ^{:doc "Event processor state."} state (atom nil))

(defn init [args]
  "Builds the system map and initializes the state."
  (let [proc-name (env :name "v2c-proc")
        host (env :host "localhost")
        port-s (env :port "11162")
        port (Integer/parseInt port-s)]
    (log/info "Starting SNMP trap processor:" proc-name)
    (log/debug "host:" host)
    (log/debug "port:" port)
    (->> (component/system-map
           :responder (hn/log-trap! :debug "Received event")
           :app (component/using
                  (snmp-v2c-trap-processor proc-name host port)
                  [:responder]))
         (reset! state))
    (log/debug "system state created")))

(defn start []
  "Starts the system as required by Component."
  (log/debug "starting processor")
  (swap! state component/start)
  (log/debug "started processor"))

(defn stop []
  "Stops the system as required by Component."
  (log/debug "stopping processor")
  (swap! state component/stop)
  (log/debug "stopped processor"))

(defn -init
  "Daemon required initialization."
  [this ^DaemonContext context]
  (init (.getArguments context)))

(defn -start
  "Daemon required start."
  [this]
  (future start))

(defn -stop
  "Daemon required stop."
  [this]
  (stop))

(defn -main
  "Program entry point."
  [& args]
  (init args)
  (start))
