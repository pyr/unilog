(ns unilog.context
  "Provide a way to interact with Mapped Diagnostic Context"
  (import org.slf4j.MDC))

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

(defn mdc-bound-fn*
  [f]
  (let [mdc (MDC/getCopyOfContextMap)]
    (bound-fn*
     (fn []
       (MDC/setContextMap mdc)
       (f)))))

(defmacro mdc-bound-fn
  [& body]
  (mdc-bound-fn* (fn [] ~@body)))
