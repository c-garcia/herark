(ns herark.handlers
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
    [e :- hk/SnmpV2CTrapInfo]
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
        [e :- hk/SnmpV2CTrapInfo]
        (binding [*out* file]
          (println (str e))
          (flush))))))
