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

- `:level`: Default logging level, any of `debug`, `info`, `warn`, `error`, `all`, `trace`, `off`.
- `:external`: Do not try to configure logging, an external configuration has been supplied.
- `:overrides`: Provide a map of namespace to level, overriding the provided default level.

### Console

If the `:console` key is present in the configuration map, it may be any of:

- `false`: Do not log to the console.
- `true`: Log to the console, using a pattern encoder and the default pattern.
- A string: Log to the console, using a pattern encoder and the supplied pattern string.
- A map: Log to the console, other attributes are taken from the map. For instance: `{:console {:encoder :json}}`.

### File

If the `:file` key is present in the configuration map, it may be any of:

- A string: Log to the provided file, using a pattern encoder and the default pattern.
- A map: Log to a file, taking configuration attributes from the map. For instance: `{:file {:file "/var/log/foo.log" :encoder :json}}`

### Files

Expects a sequence of valid configurations for `File`.

### Appenders

As for `Files`, but do not assume a specific appender, expect it to be supplied in the configuration map.

## Encoders

The following encoders are currently supported:

- `PatternLayoutEncoder`: Using a default pattern of `"%p [%d] %t - %c%n%m%n"`. Use `:encoder :pattern`.
- `LogstashEncoder`: If you wish to format messages for logstash. Use `:encoder :json`.

## Appenders

The following appenders are currently supported:

- `:console`.
- `:file`. Understands the following arguments: `:file`.
- `:rolling-file`. Log to files and rotate. Understands the following arguments: `:file`, `:rolling-policy`, `:triggering-policy`.
- `:socket`. Understands the following arguments: `:remote-host`, `:port`, `:queue-size`, `reconnection-delay`, `:event-delay-limit`.
- `:syslog`. Understands the following arguments: `:host`, `:port`.

## Extending

If you wish to supply your own configuration functions for appenders or encoders, you may do so by
adding multi-methods for `build-appender` and `build-encoder` respectively. `build-appender` dispatches
on the `:appender` key in a configuration map while `build-encoder` dispatches on the `:encoder` key.

These functions receive the provided configuration map and may thus expect specific keys to be present
to perform their configuration.

## Example configuration map

```clojure
{:console false
 :files ["/var/log/standard.log"
         {:file "/var/log/standard-json.log" :encoder :json}]
 :appenders [{:appender :file
              :encoder  :json
              :file     "/var/log/other-json.log"}
             {:appender :file
              :encoder  :pattern
              :pattern  "%p [%d] %t - %c %m%n"
              :file     "/var/log/other-pattern.log"}]}
```


## API documentation

Full API documentation is available at http://pyr.github.io/unilog

## License

Copyright © 2014 Pierre-Yves Ritschard <pyr@spootnik.org>
MIT/ISC License, See LICENSE file.
