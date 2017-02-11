(ns unilog.encoder.json
  (:import ch.qos.logback.classic.spi.LoggingEvent
           ch.qos.logback.core.encoder.EncoderBase)
  (:require [cheshire.core :refer [generate-string]]
            [clj-time.coerce :refer [from-long]]
            [clj-time.format :refer [unparse formatters]
             ]))

(defn- ^"[B" str-bytes
  [^String s ^String sep]
  (.getBytes (str s sep)))

(def header-bytes (byte-array 0))
(def footer-bytes (byte-array 0))

(defn iso8601
  [date]
  (unparse (:date-time formatters) (from-long date)))

(defn format-event
  [^LoggingEvent ev]
  (merge
   (into {} (.getMDCPropertyMap ev))
   {"@version"   1
    "@timestamp" (iso8601 (.getTimeStamp ev))
    :message     (.getMessage ev)
    :logger-name (.getLoggerName ev)
    :thread-name (.getThreadName ev)
    :level       (-> ev .getLevel .levelStr)}))

(defn make-encoder
  []
  (proxy [EncoderBase] []
    (encode      [e] (str-bytes (generate-string (format-event e)) "\n"))
    (headerBytes []  header-bytes)
    (footerBytes []  footer-bytes)
    (isStarted   []  true)
    (start       []) ;; no-op
    (stop        []))) ;; no-op
