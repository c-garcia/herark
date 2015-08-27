# herark

[![Build Status](https://travis-ci.org/c-garcia/herark.svg)](https://travis-ci.org/c-garcia/herark)

A Clojure library designed to receive and process SNMP traps, [Ring](https://github.com/ring-clojure/ring) style. 
It is comprised of:

* core: SNMP Trap event map definition by means of [Prismatic Schema](https://github.com/Prismatic/schema).
* handlers: side-effecting functions that receive a Trap event map and process it.
* middleware: similar to ring middleware. HOF that wrap handlers or other middleware.
* tools: Auxiliary functions
* adapters.snmp4j: Adapter built using the excellent SNMP4J framework.
* svc: an example trap processor.

## Usage

Add the item below to your leiningen `:dependencies`

    [eu.obliquo/herark "0.1.8-SNAPSHOT"]
    
## The big picture

Exactly as with `ring`, behaviour is implemented by means of handlers and middleware, which are composed into function, 
the handler, being passed to an adapter as a _responder_.

FIXME Should we standardize on `handler`?

*Note:* Responder seems to be a somewhat standard SNMP term.

## Writing handlers

As explained above, handlers are functions that take an event map and process it, returning no meaningful data. The
usual way to create a handler is via a HOF.

At the moment, the namespace `herark.handlers` has some basic ones that could serve as the foundations of more 
advanced behaviours. As an example, see the handler below (included in the library):

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
              
As it can be seen, `prismatic schema` tools have been used to define the signature of the handler function. This
is not mandatory but in the future may be used to ensure the handlers receive correct arguments.

## Writing middleware

TODO
              
## Using handlers

Handlers are passed to an adapter which will invoke them when a trap arrives. As an example, to set up an instance
of the included SNMP4J adapter with a responder function, we could do:

    (require '[herark.adapters.snmp4j :refer :all])
    (def sys (-> (snmp-v2c-trap-processor "my-processor" "localhost" 11164)
                 (assoc :responder (print-to-file! "/tmp/traps.txt"))
                 component/start)
              
The aforementioned adapter uses Stuart Sierra's [component](https://github.com/stuartsierra/component) 
library to manage its lifecycle.


## The name: Herark

Lately, I am naming my small projects after characters by my favourite writers. In this case, Herark the Harbinger 
is one of the minor ones appearing in the book "Rhialto the Marvellous", by Jack Vance.

## License

Copyright © 2015 Cristobal Garcia

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
