(ns Swenews.redis
  (:require redis))


(defn use-point [user]
       (redis/with-server {:host "127.0.0.1" :port 6379 :db 0}
         (do
           (println  (redis/incr (str user ":given")))
           (Integer/parseInt (redis/get (str user ":given"))))))

(defn used-points [user]
         (redis/with-server {:host "127.0.0.1" :port 6379 :db 0}
         (redis/get (str user ":given"))))


(defn give-post-points [post]
       (redis/with-server {:host "127.0.0.1" :port 6379 :db 0}
         (redis/incr (str post ":points"))))



