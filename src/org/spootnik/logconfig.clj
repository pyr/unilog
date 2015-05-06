(ns org.spootnik.logconfig
  "Small veneer on top of log4j and commons logging.
   Originally based on the logging initialization in riemann.

   A single public function is exposed: `start-logging!` which
   takes care of configuring log4j, later logging is done through
   standard facilities, such as [clojure.tools.logging](https://github.com/clojure/tools.logging)
  "
  (:import org.apache.log4j.Logger
           org.apache.log4j.BasicConfigurator
           org.apache.log4j.EnhancedPatternLayout
           org.apache.log4j.Level
           org.apache.log4j.ConsoleAppender
           org.apache.log4j.FileAppender
           org.apache.log4j.spi.RootLogger
           org.apache.log4j.rolling.TimeBasedRollingPolicy
           org.apache.log4j.rolling.RollingFileAppender
           org.apache.commons.logging.LogFactory
           net.logstash.log4j.JSONEventLayoutV1))

(def ^{:no-doc true}
  levels
  "Logging level names to log4j level association"
  {"debug" Level/DEBUG
   "info"  Level/INFO
   "warn"  Level/WARN
   "error" Level/ERROR
   "all"   Level/ALL
   "fatal" Level/FATAL
   "trace" Level/TRACE
   "off"   Level/OFF})

(defn start-logging!
  "Initialize log4j logging from a map.

   The map accepts the following keys as keywords:

   - `:level`: Default level at which to log.
   - `:pattern`: The pattern to use for logging text messages
   - `:console`: Append messages to the console using a simple pattern
      layout
   - `:files`: A list of either strings or maps. strings will create
      text files, maps are expected to contain a `:path` key as well
      as an optional `:json` which when present and true will switch
      the layout to a JSONEventLayout for the logger.
   - `:overrides`: A map of namespace or class-name to log level,
      this will supersede the global level.
   - `:json`: When true, console logging will use a JSON Event layout
   - `:external`: Do not proceed with configuration, this
      is useful when logging configuration is provided
      in a different manner (by supplying a log4j properties
      file through the `log4j.configuration` property for instance.

   When called with no arguments, assume an empty map

example:

```clojure
{:console true?
 :level     \"info\"
 :pattern   \"%p [%d] %t - %c - %m%n\"
 :files     [\"/var/log/app.log\"
             {:path \"/var/log/app-json.log\"
              :json true}]
 :overrides {\"some.namespace\" \"debug\"}}
```
  "
  ([{:keys [external console files pattern level overrides json]
     :or   {console true}}]
     (let [j-layout    (JSONEventLayoutV1.)
           p-layout    (EnhancedPatternLayout.
                        (or pattern "%p [%d] %t - %c - %m%n"))
           layout      (fn [json?] (if json? j-layout p-layout))
           root-logger (Logger/getRootLogger)]

       (when-not external

         (.removeAllAppenders root-logger)

         (when console
           (.addAppender root-logger (ConsoleAppender. (layout json))))

         (doseq [file files]
           (let [path           (if (string? file) file (:path file))
                 json           (if (string? file) false (:json file))
                 rolling-policy (doto (TimeBasedRollingPolicy.)
                                  (.setActiveFileName path)
                                  (.setFileNamePattern
                                   (str path ".%d{yyyy-MM-dd}.gz"))
                                  (.activateOptions))
                 log-appender   (doto (RollingFileAppender.)
                                  (.setRollingPolicy rolling-policy)
                                  (.setLayout (layout json))
                                  (.activateOptions))]
             (.addAppender root-logger log-appender)))

         (.setLevel root-logger (get levels level Level/INFO))

         (doseq [[logger level] overrides
                 :let [logger (Logger/getLogger (name logger))
                       level  (get levels level Level/INFO)]]
           (.setLevel logger level)))))
  ([]
     (start-logging! {})))
