(ns unilog.config
  "Small veneer on top of logback.
   Originally based on the logging initialization in riemann.
   Now diverged quite a bit.

   For configuration, a single public function is exposed: `start-logging!` which
   takes care of configuring logback, later logging is done through
   standard facilities, such as [clojure.tools.logging](https://github.com/clojure/tools.logging).

   Two extension mechanism are provided to add support for more appenders and encoders,
   see `build-appender` and `build-encoder` respectively"
  (:import org.slf4j.LoggerFactory
           ch.qos.logback.classic.encoder.PatternLayoutEncoder
           ch.qos.logback.classic.Logger
           ch.qos.logback.classic.BasicConfigurator
           ch.qos.logback.classic.Level
           ch.qos.logback.core.ConsoleAppender
           ch.qos.logback.core.FileAppender
           ch.qos.logback.core.rolling.TimeBasedRollingPolicy
           ch.qos.logback.core.rolling.RollingFileAppender
           net.logstash.logback.encoder.LogstashEncoder))

;; Configuration constants
;; =======================

(def levels
  "Logging level names to log4j level association"
  {"debug" Level/DEBUG
   "info"  Level/INFO
   "warn"  Level/WARN
   "error" Level/ERROR
   "all"   Level/ALL
   "trace" Level/TRACE
   "off"   Level/OFF})

(def default-pattern
  "Default pattern for PatternLayoutEncoder"
  "%p [%d] %t - %c %m%n")

(def default-encoder
  "Default encoder and pattern configuration"
  {:encoder :pattern
   :pattern  default-pattern})


;; Open dispatch method to build appender configuration
;; ====================================================

(defmulti appender-config
  "Called by walking through each key/val pair in the main configuration
   map. This allows for early transformation of quick access keys such as:
   `:console`, `:file`, and `:files`"
  first)

(defmethod appender-config :default
  [_]
  nil)

(defmethod appender-config :console
  [[_ val]]
  (println "building console appender configuration: " (pr-str val))
  (when (boolean val)
    (cond (string? val)  {:appender :console
                          :encoder  :pattern
                          :pattern  val}
          (map? val)     (-> (merge default-encoder val)
                             (update-in [:encoder] keyword)
                             (assoc :appender :console))
          :else          {:appender :console
                          :encoder  :pattern
                          :pattern  default-pattern})))

(defmethod appender-config :file
  [[_ val]]
  (println "building file appender configuration: " (pr-str val))
  (cond (string? val) (-> default-encoder
                          (assoc :appender :file)
                          (assoc :file val))
        (map? val)    (-> (merge default-encoder val)
                          (update-in [:encoder] keyword)
                          (assoc :appender :file))
        :else         (throw (ex-info "invalid file appender config"
                                      {:config val}))))

(defmethod appender-config :files
  [[_ files]]
  (println "building file appender configurations: " (pr-str val))
  (for [file files]
    (appender-config [:file file])))

(defmethod appender-config :appenders
  [[_ appenders]]
  (println "building generic appender configurations: " (pr-str val))
  (for [appender appenders
        :when (map? appender)]
    (-> appender
        (update-in [:encoder] keyword)
        (update-in [:appender] keyword))))

;; Open dispatch method to build encoders based on configuration
;; =============================================================

(defmulti build-encoder
  "Given a prepared configuration map, associate a prepared encoder
  to the `:encoder` key."
  :encoder)

(defmethod build-encoder :pattern
  [{:keys [pattern] :as config}]
  (println "building pattern encoder" (pr-str pattern))
  (let [encoder (doto (PatternLayoutEncoder.)
                  (.setPattern (or pattern default-pattern)))]
    (assoc config :encoder encoder)))

(defmethod build-encoder :json
  [config]
  (println "building json encoder")
  (assoc config :encoder (LogstashEncoder.)))

(defmethod build-encoder :default
  [config]
  (println "building default pattern encoder")
  (assoc config :encoder (doto (PatternLayoutEncoder.)
                           (.setPattern default-pattern))))

;; Open dispatch method to build appenders
;; =======================================

(defmulti build-appender
  "Given a prepared configuration map, associate a prepared appender
  to the `:appender` key."
  :appender)

(defmethod build-appender :console
  [config]
  (println "building console appender")
  (assoc config :appender (ConsoleAppender.)))

(defmethod build-appender :file
  [{:keys [file] :as config}]
  (println "building file appender for:" (pr-str file))
  (assoc config :appender (doto (FileAppender.)
                            (.setFile file))))

(defmethod build-appender :default
  [val]7
  (throw (ex-info "invalid log appender configuration" {:config val})))

(defn start-logging!
  "Initialize log4j logging from a map.

   The map accepts the following keys as keywords:

   - `:level`: Default level at which to log.
   - `:pattern`: The pattern to use for logging text messages
   - `:console`: Append messages to the console using a simple pattern
      layout. If value is a boolean, treat it as such and use a default
      encoder. If value is a string, treat it as a pattern and use
      a pattern encoder. If value is a map, expect encoder configuration
      in the map.
   - `:file`:  A file to log to. May either be a string, the log file, or
      a map which accepts optional encoder configuration.
   - `:files`: A list of either strings or maps. strings will create
      text files, maps are expected to contain a `:path` key as well
      as an optional `:json` which when present and true will switch
      the layout to a JSONEventLayout for the logger.
   - `:overrides`: A map of namespace or class-name to log level,
      this will supersede the global level.
   - `:external`: Do not proceed with configuration, this
      is useful when logging configuration is provided
      in a different manner (by supplying your own logback config file
      for instance).

   When called with no arguments, assume an empty map

example:

```clojure
{:console   true
 :level     \"info\"
 :files     [\"/var/log/app.log\"
             {:file \"/var/log/app-json.log\"
              :encoder json}]
 :overrides {\"some.namespace\" \"debug\"}}
```
  "
  ([{:keys [external level overrides] :as config}]
   (let [level   (get levels (some-> level name) Level/INFO)
         root    (LoggerFactory/getLogger Logger/ROOT_LOGGER_NAME)
         context (LoggerFactory/getILoggerFactory)
         configs (->> (merge {:console true} config)
                      (map appender-config)
                      (flatten)
                      (remove nil?))]

     (when-not external
       (.detachAndStopAllAppenders root)

       (doseq [config configs
               :let [config   (-> config build-encoder build-appender)
                     encoder  (:encoder config)
                     appender (:appender config)]]
         (.setContext encoder context)
         (.start encoder)
         (.setEncoder appender encoder)
         (.setContext appender context)
         (.start appender)
         (.addAppender root appender))

       (.setLevel root level)
       (doseq [[logger level] overrides
               :let [logger (LoggerFactory/getLogger (name logger))
                     level  (get levels level Level/INFO)]]
         (.setLevel logger level))
       root)))
  ([]
   (start-logging! {})))
