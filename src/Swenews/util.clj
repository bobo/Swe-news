(ns Swenews.util
  (:import java.util.Date
           [ java.util.concurrent TimeUnit TimeoutException PriorityBlockingQueue Executors Callable]
           [org.joda.time.format ISODateTimeFormat DateTimeFormat]
           [org.joda.time DateTime]
           ))



(defn promise-t
  "Alpha - subject to change.
  Returns a promise object that can be read with deref/@, and set,
  once only, with deliver. Calls to deref/@ prior to delivery will
  block. All subsequent derefs will return the same delivered value
  without blocking."
  {:added "1.1"}
  [t u]
  (let [d (java.util.concurrent.CountDownLatch. 1)
        v (atom nil)]
    (reify 
     clojure.lang.IDeref
     (deref [_] (if (. d (await t u))
                  @v
                  "0"))
     clojure.lang.IFn
      (invoke [this x]
        (locking d
          (if (pos? (.getCount d))
            (do (reset! v x)
                (.countDown d)
                this)
            (throw (IllegalStateException. "Multiple deliver calls to a promise"))))))))

