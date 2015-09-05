# herark

[![Build Status](https://travis-ci.org/c-garcia/herark.svg)](https://travis-ci.org/c-garcia/herark)

A Clojure library designed to receive and process SNMP traps, [Ring](https://github.com/ring-clojure/ring) style. 
It is comprised of:

* core: SNMP Trap event map definition by means of [Prismatic Schema](https://github.com/Prismatic/schema).
* handlers: side-effecting functions that receive a Trap event map and process it.
* middleware: similar to ring middleware. HOF that wrap handlers or other middleware.
* adapters.snmp4j: Adapter built using the excellent SNMP4J framework.
* adapters.tools: Tools for testing adapter implementations.
* svc: an example trap processor.

## Usage

Add the item below to your leiningen `:dependencies`

    [eu.obliquo/herark "0.1.13-SNAPSHOT"]
    
## The big picture

Exactly as with `ring`, behaviour is implemented by means of handlers and middleware, which are composed into a function, 
being passed to an adapter as a _responder_.


*Note:* Responder seems to be a somewhat standard SNMP term.

## Writing handlers

As explained above, handlers are functions that take an event map and process it but return no meaningful data. The
usual way to create a handler is via a HOF. It will return a closure able to access any required handler 
configuration. 

At the moment, the namespace `herark.handlers` has some basic ones that could serve as the foundations of more 
advanced behaviours. As an example, see the handler below (included in the library) which shows not only
the basic trap processing but also how to define and use handler configuration elements via a HOF:

```clojure
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
```
              
As it can be seen, `prismatic schema` tools have been used to define the signature of the handler function. This
is not mandatory but in the future may be used to ensure the handlers receive correct arguments.

## Writing middleware

The same as in Ring, middleware are functions that enrich the capabilities of handlers or other middleware. 
Middleware and a handler are composed into a function that will be passed to an _adapter_ as a _responder_.
 
Again, this is normally done via a HOF. Let's see below an example:

```clojure
(defn check-static-community!
  "Creates a middleware function that, for each received trap checks the SNMP community `c`. If the community
  matches, the processing continues to the next function. If not, it returns."
  [f c]
  (s/fn [e :- TrapEvent]
    (let [comm-match (match [e]
                            [{:message {:version   :v1
                                        :community [::smiv1/octet-string pdu-c]}}] (= pdu-c c)
                            [{:message {:version  :v2c
                                        :community [::smiv2/octet-string pdu-c]}}] (= pdu-c c)
                            :else nil)]
      (if comm-match
        (do
          (log/debug "communities match.")
          (f e))
        (log/error "communities do not match.")))))

```
              
## Using handlers and middleware: responder functions

Responder functions are invoked by a an adapter when a trap arrives. They are compositions 
of several middleware and a handler (the latest function to be invoked).

```clojure
(require '[herark.adapters.snmp4j :refer :all])
(def sys (-> (snmp-v2c-trap-processor "my-processor" "localhost" 11164)
             (assoc :responder (print-to-file! "/tmp/traps.txt"))
             component/start)
```
              
The aforementioned adapter uses Stuart Sierra's [component](https://github.com/stuartsierra/component) 
library to manage its lifecycle. Therefore, the `start` function call. That being said,
other adapters may use different ways to get started.


## The name: Herark

Lately, I am naming my small projects after characters by my favourite writers. In this case, Herark the Harbinger 
is one of the minor ones appearing in the book "Rhialto the Marvellous", by Jack Vance.

## License

Copyright © 2015 Cristobal Garcia

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
