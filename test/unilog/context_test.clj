(ns unilog.context-test
  (:require [unilog.context        :refer [with-context]]
            [clojure.test          :refer [deftest is testing]]))

(deftest context-is-stacked
  (testing "Context stacking works as expected"
    (with-context {"a" "1"}
      (is (= "1" (get (org.slf4j.MDC/getCopyOfContextMap) "a")))
      (with-context {:a 2}
        (is (= "2" (get (org.slf4j.MDC/getCopyOfContextMap) "a"))))
      (is (= "1" (get (org.slf4j.MDC/getCopyOfContextMap) "a"))))))
  
  
