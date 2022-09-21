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

(defn- make-comparator
  "Makes a `LoggerEncoder` comparator that puts the best match for a given `logger-name` first.
  Uses the `logger` field to compare against `logger-name`."
  [^String logger-name]
  (reify Comparator
    (compare [this encoder1 encoder2]
      ;; remove the dots
      ;; compare longest prefix
      ;; return best match
      (let [logger   (.replaceAll logger-name "\\." "")
            e1logger (.replaceAll ^String (-> encoder1 :logger name) "\\." "")
            e2logger (.replaceAll ^String (-> encoder2 :logger name) "\\." "")
            o2match  (.length (StringUtils/getCommonPrefix (into-array String [e2logger logger])))
            o1match  (.length (StringUtils/getCommonPrefix (into-array String [e1logger logger])))
            ;; match is hierarchical, so we want the shortest one
            ;; if some encoders have the same prefix (eg: logger-name is 'com.exoscale'
            ;; and we have 'com.exoscale.blah' and 'com.exoscale.blah.c' we want the shortest one
            o2Rem    (- (.length e2logger) o2match)
            o1Rem    (- (.length e1logger) o1match)
            diff     (- o2match o1match)]

        (if (zero? diff)
          (- o1Rem o2Rem)
          diff)))))

(defn- closest-match
  "Find the closest encoder for the give `logger-name` and coll of `encoder`s.
  Fallback to `root-layout` if none found"
  ^Layout [logger-name encoders root-layout]
  (if (or (.equalsIgnoreCase Logger/ROOT_LOGGER_NAME logger-name)
          (empty? encoders))
    root-layout
    (let [encoder (->> encoders
                       (sort (make-comparator logger-name))
                       (first))]
      (.getLayout ^PatternLayoutEncoder (:encoder encoder)))))

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
