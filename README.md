# unilog: logging should be easy!

[clojure.tools.logging](https://github.com/clojure/tools.logging) is
a great library to perform logging. It walks through several available
options such as [slf4j](http://www.slf4j.org),
[commons-logging](http://commons.apache.org/logging),
[log4j](http://logging.apache.org/log4j/),
and [logback](http://logback.qos.ch).

While the logging itself is simple and straightforward, navigating the
many ways to configure logging can be a bit daunting since the above
logging frameworks which
[clojure.tools.logging](https://github.com/clojure/tools.logging)
allow external configuration.

Unilog provides a simple and somewhat opiniated way of configuring
[logback](http://logback.qos.ch/) through simple clojure maps.


## Coordinates

```clojure
[spootnik/unilog "0.7.4"]
```

## Usage

Let's pretend you have an application, which reads its initial
configuration in a YAML file:


```yaml
other-config:
  foo: bar
logging:
  level: info
  console: true
  files:
    - "/var/log/program.log"
    - file: "/var/log/program-json.log"
      json: true
  overrides:
    some.namespace: debug
```
You would supply configuration by parsing the YAML and then
calling `start-logging!`

```clojure
(require '[clj-yaml.core  :refer [parse-string]]
         '[unilog.config  :refer [start-logging!]])

(let [default-logging  {:level "info" :console true}
      config           (parse-string (slurp "my-config.yml"))]
  (start-logging! (merge default-logging (:logging config)))
  ;; rest of program startup)
```

## API documentation

Full API documentation is available at http://pyr.github.io/unilog

## License

Copyright © 2014 Pierre-Yves Ritschard <pyr@spootnik.org>
MIT/ISC License, See LICENSE file.
