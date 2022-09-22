(ns unilog.encoders
  (:import [ch.qos.logback.classic.encoder PatternLayoutEncoder]
           [ch.qos.logback.classic.spi ILoggingEvent]
           [ch.qos.logback.core Layout]
           [ch.qos.logback.classic Logger]
           [java.util Comparator]
           [net.logstash.logback.encoder.org.apache.commons.lang3 StringUtils]))

(defrecord LoggerEncoder [logger pattern encoder])

(defn- make-encoder
  "Make an encoder for the given params"
  [logger pattern output-pattern-as-header]
  (let [encoder (doto (PatternLayoutEncoder.)
                  (.setPattern pattern)
                  (.setOutputPatternAsHeader output-pattern-as-header))]
    (->LoggerEncoder logger pattern encoder)))

(defn- counteq-prefixes [[v1 & vrest1] [v2 & vrest2]]
  (if (or (nil? v1)
          (nil? v2)
          (not= v1 v2))
    0
    (+ 1 (counteq-prefixes vrest1 vrest2))))


(defn- find-best-layout
  "Makes a `LoggerEncoder` comparator that puts the best match for a given `logger-name` first.
  Uses the `logger` field to compare against `logger-name`."
  [^String logger-name encoders]
  (let [parts (vec (.split logger-name "[.]"))]
    (->> encoders
         (map (fn [enc]
                (let [eparts (vec (.split ^String (name (:logger enc)) "[.]"))
                      common (counteq-prefixes parts eparts)
                      ;; ensure that  eg: "amazon" does not match an encoder "amazonaws"
                      match? (= (count eparts) common)]
                  [enc (if match? common 0)])))
         (filter (fn [[_ score]] (pos-int? score)))
         (sort-by second)
         (reverse)
         (ffirst))))

(defn- closest-match
  "Find the closest encoder for the give `logger-name` and coll of `encoder`s.
  Fallback to `root-layout` if none found"
  ^Layout [logger-name encoders root-layout]
  (if (or (.equalsIgnoreCase Logger/ROOT_LOGGER_NAME logger-name)
          (empty? encoders))
    root-layout
    (let [encoder (find-best-layout logger-name encoders)]
      (def encoders encoders)
      (if (some? encoder)
          (.getLayout ^PatternLayoutEncoder (:encoder encoder))
          root-layout))))

(defn make-multilayout-encoder
  "Create a custom PatternLayoutEncoder that can support multiple layouts per logger.
  - `root-pattern` is a String
  - `logger-patterns` is a map of `{logger pattern}`"
  [root-pattern logger-patterns]
  (let [layouts  (atom {})
        encoders (for [kv logger-patterns
                       :let [[logger root-pattern] kv]]
                   (make-encoder logger root-pattern false))]
    (proxy [PatternLayoutEncoder] []
      (start []
        (.setPattern ^PatternLayoutEncoder this root-pattern)
        (run! #(do
                 (.setContext ^PatternLayoutEncoder (:encoder %) (.getContext ^PatternLayoutEncoder this))
                 (.start ^PatternLayoutEncoder (:encoder %)))
               encoders)
        (proxy-super start))
      (encode [^ILoggingEvent event]
        (let [logger-name    (.getLoggerName event)
              root-layout    (.getLayout ^PatternLayoutEncoder this)
              ;; cache the closest layout for a given logger name
              ^Layout layout (get @layouts logger-name
                                  (let [match (closest-match logger-name encoders root-layout)]
                                    (swap! layouts assoc logger-name match)
                                    match))
              ^String txt    (.doLayout layout event)
              charset        (.getCharset ^PatternLayoutEncoder this)]
          (if (nil? charset)
            (.getBytes txt)
            (.getBytes txt charset)))))))
