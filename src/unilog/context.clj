(ns unilog.context
  "Provide a way to interact with Mapped Diagnostic Context")

(defn push-context
  "Add a key to the current Mapped Diagnostic Context"
  [k v]
  (org.slf4j.MDC/put (name k) (str v)))

(defn pull-context
  "Remove a key to the current Mapped Diagnostic Context"
  [k]
  (org.slf4j.MDC/remove (name k)))

(defmacro with-context
  "Execute body with the Mapped Diagnostic Context updated from
   keys found in the ctx map."
  [ctx & body]
  (if-not (map? ctx)
    (throw (ex-info "with-context expects a map" {}))
    `(try
       (doseq [[k# v#] ~ctx ]
         (push-context k# v#))
       ~@body
       (finally
         (doseq [[k# _#] ~ctx]
           (pull-context k#))))))

(defn mdc-fn*
  [f]
  (let [mdc (org.slf4j.MDC/getCopyOfContextMap)]
    (fn [& args]
      (when (some? mdc)
        (org.slf4j.MDC/setContextMap mdc))
      (apply f args))))

(defmacro mdc-fn
  [& fntail]
  `(mdc-fn* (fn ~@fntail)))


(comment

  ;; Example usage, you'll need a pattern that shows MDC values

  (require '[clojure.tools.logging :refer [info]])

  (let [f (mdc-fn [] (with-context {:mdcval "bar"} (info "something")))]
    @(future
       (f))))
