# unilog: logging should be easy!

[![Build Status](https://secure.travis-ci.org/pyr/unilog.png)](http://travis-ci.org/pyr/unilog)


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
[spootnik/unilog "0.7.24"]
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
      encoder: json
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

## Configuration details

The configuration, given as a map to `start-logging!` understands
a number of keys.

### Global Options

* `:level`: Default logging level
  * any of `:all`, `:trace`, `:debug`, `:info`, `:warn`, `:error`, `:off`
* `:external`
  * If it is `true`, do not try to configure logging. An external configuration is supplied.
* `:overrides`
  * Provide a map of namespace to level, overriding the provided default level.

### Console

If the `:console` key is present in the configuration map, it may be any of:

* `false`
  * Do not log to the console.
* `true`
  * Log to the console, using a pattern encoder and the default pattern.
* A string
  * Log to the console, using a pattern encoder and the supplied pattern string.
* A map
  * Log to the console, other attributes are taken from the map.
  * For instance: `{:console {:encoder :json}}`.

### File

If the `:file` key is present in the configuration map, it may be any of:

* A string: Log to the provided file, using a pattern encoder and the default pattern.
* A map: Log to a file, taking configuration attributes from the map.
  * For instance: `{:file {:file "/var/log/foo.log" :encoder :json}}`

### Files

Expects a sequence of valid configurations for `File`.

### Appenders

As for `Files`, but do not assume a specific appender, expect it to be supplied in the configuration map.

## Example configuration map

```clojure
{:level   :info
 :console false
 :files ["/var/log/standard.log"
         {:file "/var/log/standard-json.log" :encoder :json}]
 :file {:file "/var/log/file.log" :encoder :json}
 :appenders [{:appender :file
              :encoder  :json
              :file     "/var/log/other-json.log"}

             {:appender :file
              :encoder  :pattern
              :pattern  "%p [%d] %t - %c %m%n"
              :file     "/var/log/other-pattern.log"}

             {:appender :rolling-file
              :file     "/var/log/rolling-file.log"}

             {:appender :rolling-file
              :rolling-policy :fixed-window
              :triggering-policy :size-based
              :file     "/var/log/rolling-file.log"}

             {:appender :rolling-file
              :rolling-policy {:type :fixed-window
                               :max-index 5}
              :triggering-policy {:type :size-based
                                  :max-size 5120}
              :file     "/var/log/rolling-file.log"}]
 :overrides  {"org.apache.http"      :debug
              "org.apache.http.wire" :error}}
```

## Encoders

You could specify encoder arguments in some appenders. Not every appender supports encoders.
The following encoders are currently supported in `:appenders`.

`PatternLayoutEncoder` uses a default pattern of `"%p [%d] %t - %c %m%n"`.

```clojure
{:appender :file
 :file     "/var/log/file.log"
 ;; PatternLayoutEncoder
 ;; Without :pattern argument in an appender config, the default pattern is used.
 :encoder  :pattern}

{:appender :file
 :file     "/var/log/file2.log"
 :encoder  :pattern
 :pattern  "%p [%d] %t - %c %m%n"}
```

`LogstashEncoder` formats messages for logstash.

```clojure
{:appender :file
 :file     "/var/log/file3.log"
 ;; LogstashEncoder
 :encoder  :json}
```

## Appenders

The following appenders are currently supported:

### `:console` appender

* Optional Arguments
  * `:encoder`
  * `:pattern`

```clojure
{:appender :console}

{:appender :console
 :encoder  :pattern}

{:appender :console
 :encoder  :pattern
 :pattern  "%p [%d] %t - %c %m%n"}

{:appender :console
 :encoder  :json}
```

### `:file` appender

* Mandatory Arguments
  * `:file`
* Optional Arguments
  * `:encoder`
  * `:pattern`

```clojure
{:appender :file
 :file     "/var/log/file.log"}

{:appender :file
 :file     "/var/log/file.log"
 :encoder  :pattern}

{:appender :file
 :file     "/var/log/file.log"
 :encoder  :pattern
 :pattern  "%p [%d] %t - %c %m%n"}

{:appender :file
 :file     "/var/log/file.log"
 :encoder  :json}
```

### `:rolling-file` appender

* Mandatory Arguments
  * `:file`
* Optional Arguments
  * `:rolling-policy`
  * `:triggering-policy`
  * `:encoder`
  * `:pattern`

There are two rolling policies.

* `:fixed-window`
  * Renames files according to a fixed window algorithm.
* `:time-based`
  * Defines a rollover based on time.

Don't use a triggering policy with `:time-based` rolling policy since `:time-based` rolling policy is its own triggering policy as well.
You can specify a rolling policy by the keyword.

```clojure
{:appender       :rolling-file
 :rolling-policy :fixed-window
 :file           "/var/log/rolling-file.log"
 :encoder        :pattern}

{:appender :rolling-file
 :rolling-policy :time-based
 :file           "/var/log/rolling-file2.log"
 :encoder        :pattern
 :pattern        "%p [%d] %t - %c %m%n"}
```

If you want to specify arguments for a rolling policy, you can pass a map to `:rolling-policy` as below. every argument to a rolling policy except `:type` is optional.

```clojure
{:appender :rolling-file
 :file           "rolling-file.log"
 :rolling-policy {:type      :fixed-window
                  :min-index 1
                  :max-index 5
                  ;; :pattern combines with :file to make the name of a rolled log file.
                  ;; For example, "rolling-file.log.%i.gz"
                  ;; %i is index.
                  :pattern  ".%i.gz"}
 :encoder        :json}

{:appender :rolling-file
 :file "rolling-file2.log"
 ;; If you use this rolling policy, don't use a triggering policy
 :rolling-policy {:type        :time-based
                  ;; log files are kept for :max-history periods.
                  ;; periods can be hours, days, months, and so on.
                  :max-history 5
                  ;; Before a period ends, if a log file reaches :max-size, it is rolled.
                  ;; :max-size adds %i to :pattern. Without :max-size, you shouldn't
                  ;; specify %i in :pattern.
                  ;; Refer to http://logback.qos.ch/manual/appenders.html#SizeAndTimeBasedFNATP
                  ;; for elaborate description of :max-size
                  :max-size    51200 ; bytes
                  ;; :pattern combines with :file
                  ;; The rolling period is defined by :pattern.
                  ;; Refer to http://logback.qos.ch/manual/appenders.html#tbrpFileNamePattern
                  :pattern    ".%d{yyyy-MM-dd}.%i"}
 :encoder :pattern
 :pattern "%p [%d] %t - %c %m%n"}
```

There is only one triggering policy, `:size-based`.

```clojure
{:appender :rolling-file
 :rolling-policy :fixed-window
 ;; If you don't pass any argument to :size-based triggering policy, it triggers a rollover
 ;; when a log file grow beyond SizeBasedTriggeringPolicy/DEFAULT_MAX_FILE_SIZE.
 :triggering-policy :size-based
 :file          "rolling-file.log"}

{:appender :rolling-file
 :rolling-policy :fixed-window
 :triggering-policy {:type     :size-based
                     ;; Refer to
                     ;; http://logback.qos.ch/manual/appenders.html#SizeBasedTriggeringPolicy
                     :max-size 51200}} ; 51200 bytes
```

### `:socket` appender

* Optional Arguments
  * `:remote-host`
  * `:port`
  * `:queue-size`
  * `:reconnection-delay`
  * `:event-delay-limit`

```clojure
{:appender            :socket
 :remote-host        "localhost"
 :port                2004
 :queue-size          500
 :reconnection-delay "10 seconds"
 :event-delay-limit  "10 seconds"}
```

### `:syslog` appender

* Optional Arguments
  * `:host`
  * `:port`

```clojure
{:appender :syslog
 :host    "localhost"
 :port     514}
```

## Extending

If you wish to supply your own configuration functions for appenders or encoders, you may do so by
adding multi-methods for `build-appender` and `build-encoder`. `build-appender` dispatches
on the `:appender` key in a configuration map while `build-encoder` dispatches on the `:encoder` key.

These functions receive the provided configuration map and may thus expect specific keys to be present
to perform their configuration.

You may need to add a multimethod for `start-appender!` if your appender needs a specialized initialization procedure.

## API documentation

Full API documentation is available at http://pyr.github.io/unilog

## Releases

### 0.7.24

- Introduce mdc-fn and mdc-fn* which preserve MDC context across threads

### 0.7.22

- Dependency upgrades
- Switch to clojure 1.9, paving the way for specs

### 0.7.21

- Dependency upgrades

### 0.7.20

- Upgrade to logback 1.2.0

### 0.7.19

- Add tests to ensure base functionality is preserved.
- Hold-off on upgrading to logback 1.2.0 until logstash-encoder is compatible.


### 0.7.17

- Coda Hale (https://github.com/codahale) updated dependencies.

### 0.7.15 

- Coda Hale (https://github.com/codahale) added a `java.util.logging` bridge for applications relying on this logging method.

## License

Copyright © 2014 Pierre-Yves Ritschard <pyr@spootnik.org>
MIT/ISC License, See LICENSE file.
