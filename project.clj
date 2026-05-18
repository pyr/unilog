(def slf4j-version "2.0.18")
(def logback-version "1.5.32")
(def jackson-version "2.21.2")
(def jackson-annotations-version "2.21")

(defproject spootnik/unilog "0.7.33-SNAPSHOT"
  :description "logging should be easy!"
  :url "https://github.com/pyr/unilog"
  :license {:name "MIT License"
            :url  "https://github.com/pyr/unilog/tree/master/LICENSE"}
  :plugins [[lein-ancient "1.0.0"]]
  :pedantic? :abort
  :dependencies [[org.clojure/clojure                            "1.12.5"]
                 [net.logstash.logback/logstash-logback-encoder  "8.1"]
                 [org.slf4j/slf4j-api                            ~slf4j-version]
                 [org.slf4j/log4j-over-slf4j                     ~slf4j-version]
                 [org.slf4j/jul-to-slf4j                         ~slf4j-version]
                 [org.slf4j/jcl-over-slf4j                       ~slf4j-version]
                 [ch.qos.logback/logback-classic                 ~logback-version]
                 [ch.qos.logback/logback-core                    ~logback-version]
                 [com.fasterxml.jackson.core/jackson-databind    ~jackson-version]
                 [com.fasterxml.jackson.core/jackson-annotations ~jackson-annotations-version]
                 [com.fasterxml.jackson.core/jackson-core        ~jackson-version]]
  :deploy-repositories [["releases" :clojars] ["snapshots" :clojars]]
  :profiles {:dev  {:dependencies [[org.clojure/tools.logging "1.3.1"]
                                   [metosin/jsonista          "1.0.0"
                                    :exclusions [com.fasterxml.jackson.core/*]]]
                    :pedantic?    :ignore
                    :plugins      [[lein-ancient "1.0.0"]]
                    :global-vars  {*warn-on-reflection* true}}
             :test {:dependencies [[org.clojure/tools.logging "1.3.1"]
                                   [metosin/jsonista          "1.0.0"
                                    :exclusions [com.fasterxml.jackson.core/*]]]
                    :plugins      [[lein-difftest "2.0.0"]
                                   [lein-cljfmt "0.9.0"]]
                    :pedantic?    :abort}})
