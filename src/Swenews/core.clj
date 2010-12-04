(ns Swenews.core
  (:use ring.util.response
        net.cgrand.moustache
        net.cgrand.enlive-html
        [ring.middleware.params]
        [ring.adapter.jetty :only [run-jetty]]
        ring.middleware.session
        Swenews.util)
  (:require [clojure.contrib.json :as json]
            [clojure.contrib.string :as s]
            [ Swenews.mongo :as mongo]
            [ Swenews.redis :as redis]
            )
  (:import java.util.Date
           [ java.util.concurrent TimeUnit]
           [org.joda.time.format ISODateTimeFormat DateTimeFormat]
           [org.joda.time DateTime]
           ))



(defn item-to-json [newsItem]
  (json/json-str (dissoc newsItem :submited-at)))

(defn- do-print [d]
  (. (DateTimeFormat/forPattern "dd MMM HH:mm") print d))

(defmulti print-time class)
(defmethod print-time java.util.Date [d]
           (print-time (.getTime d)))
(defmethod print-time Long [d]
           (do-print d))
(defmethod print-time org.joda.time.DateTime [d]
           (do-print d))
(defmethod print-time :default [d]
           "")


(def p (atom (promise-t 20 TimeUnit/SECONDS)))

(defn update [newsItem]
  (let [prom @p]
    (println "reseting")
    (reset! p (promise-t 20 TimeUnit/SECONDS))
    (println "delivering")
    (deliver prom newsItem)
    (println (str "done, returning: " @prom))
    {:status 200
     :body (str "returned: " @prom)}))

(defsnippet feedsnippet "news.html"
  [:tr] [news]
  [:td.vote :a] (set-attr :href (str "point?title=" (:title news)))
  [:td.title] (do-> (content (s/take 50 (:title news)))
                    (set-attr :title (:title news)))
  [:td.link :a] (do-> (set-attr :href (:url news))
                      (set-attr :title (:url news))
                      (content (s/take 20  (:url news))))
  [:td.time] (content (print-time (:submited-at news)))
  [:td.points] (content (str (:points news))))

(defn get-new-news [req]
  (println "getting")
  (let [result @@p]
    (if (= "0"  result)
      {:status 200
       :body result}
      {:status 200
       :body (emit* (feedsnippet  result))})
  ))

(defn store-news [req]
  (let [params (:params req)
        news {:title (params "title") :href (params "url"),:points 0, :submited-at (Date.)}]
    (mongo/insert-news news)
    (update news)
    {:status 200
     :body (str "stored:" (params "title") (params "url"))}))

(defn get-new []
   (mongo/get-news))

(defn give-point [req]
  (let [item ((:params req) "title")
        session ((assoc (:session req) :n (redis/use-point (:user (:session  req)))))]
    (if (> 5 (:n session (redis/used-points (:user session))))
          {:status 200
           :session session
           :body (json/json-str {:count (redis/give-post-points item)})}))))


(deftemplate news-template "news.html"
  [ctxt]
  [:table#news :tr] (clone-for [news ctxt]
                               [:td.vote :a] (set-attr :href (str "point?title=" (:title news)))
                               [:td.title] (do-> (content (s/take 50 (:title news)))
                                                 (set-attr :title (:title news)))
                               [:td.link :a] (do-> (set-attr :href (:url news))
                                                   (set-attr :title (:url news))
                                                   (content (s/take 20  (:url news))))
                               [:td.time] (content (print-time (:submited-at news)))
                               [:td.points] (content (str (:points news)))
                            ))

(def Swenews-app-handler
     (app
      wrap-params
      wrap-session
      ["store"]  #(store-news %1)
      ["point"]  #(give-point %1)
;      ["promise"] #(get-new-news %1)
      ["promise"] #(get-new-news %1)
      ["deliver"] #(update %1)
      [""] (-> ( news-template (get-news)) response constantly)))


(run-jetty #'Swenews-app-handler {:port 80
                                  :join? false})

