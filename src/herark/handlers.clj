(ns herark.handlers
  "Some handlers that can be used by herark. A _handler_ is a
  side-effecting function that receives a `TrapEvent` and does something
  useful with it. Its return value is discarded.

  The same as in Ring, we provide here HOF that generate handlers with
  an specific configuration."
  (:require [herark.core :as hk]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import (java.nio.charset Charset)
           (java.nio.file Files FileSystems OpenOption StandardOpenOption)))

(defn log-trap!
  "Generates a handler function that logs traps with the specified log `level`. It prepends `message`.
  CAVEAT: This is not a middleware generator, the returned function will
  end the processing chain."
  [level message]
  (s/fn
    [e :- TrapEvent]
    (log/log level message)
    (log/log level e)))

(defn print-to-file!
  "Generates a handler function that stores traps in the specified `file-path`."
  [file-path]
  (letfn [(open-file [^String p]
                     (Files/newBufferedWriter
                       (.. (FileSystems/getDefault) (getPath p (into-array String [])))
                       (Charset/forName "UTF-8")
                       (into-array OpenOption [StandardOpenOption/WRITE
                                               StandardOpenOption/APPEND
                                               StandardOpenOption/CREATE])))]
    (let [file (open-file file-path)]
      (s/fn
        [e :- TrapEvent]
        (binding [*out* file]
          (println (str e))
          (flush))))))
