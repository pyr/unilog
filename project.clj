(defproject spootnik/unilog "0.7.17"
  :description "logging should be easy!"
  :url "https://github.com/pyr/unilog"
  :license {:name "MIT License"
            :url "https://github.com/pyr/unilog/tree/master/LICENSE"}
  :plugins [[lein-codox "0.10.2"]]
  :codox {:source-uri "https://github.com/pyr/unilog/blob/{version}/{filepath}#L{line}"
          :metadata   {:doc/format :markdown}}
  :dependencies [[org.clojure/clojure                           "1.8.0"]
                 [net.logstash.logback/logstash-logback-encoder "4.8"]
                 [org.slf4j/slf4j-api                           "1.7.22"]
                 [org.slf4j/log4j-over-slf4j                    "1.7.22"]
                 [org.slf4j/jul-to-slf4j                        "1.7.22"]
                 [ch.qos.logback/logback-classic                "1.1.8"]
                 [ch.qos.logback/logback-core                   "1.1.8"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.logging "0.3.1"]]}})
