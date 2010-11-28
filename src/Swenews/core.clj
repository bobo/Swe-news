(ns Swenews.core
  (:use ring.util.response
        net.cgrand.moustache
        net.cgrand.enlive-html
        [ring.middleware.params]
        ring.middleware.session
        Swenews.util)
  (:require [appengine-magic.services.datastore :as ds]
            [appengine-magic.core :as ae]
            [clojure.contrib.json :as json]
            [clojure.contrib.string :as s]
            [appengine-magic.services.user :as user])
  (:import java.util.Date
           [ java.util.concurrent TimeUnit]
           [org.joda.time.format ISODateTimeFormat DateTimeFormat]
           [org.joda.time DateTime]
           ))


(ds/defentity NewsItem [^:key title,url,points,submited-at])
;(ds/defentity User [^:key user-name,,points,submited-at])

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
        news (NewsItem. (params "title"),(params "url"),0,(Date.))]
    (println "saving")
    (ds/save! news)
    (println "updating")
    (update news)
    (println "done")
    {:status 200
     :body (str "stored:" (params "title") (params "url"))}))

(defn get-news []
  (ds/query :kind NewsItem
;            :limit 10
            :sort [[:submited-at :dsc]]))

(defn give-point [req]
  (let [item (first (ds/query :kind NewsItem
                              :filter (= :title ((:params req) "title"))))
        after (assoc  item :points (+ 1 (:points item)))
        c  (:n (:session req)) 
        session (assoc (:session req) :n (if c (inc c) 1))]
    (println (str "c:" (:n session) " points: " (:points  after)))
    (if (and (number? (:n session)) (> 5 (:n session)))
      (do (ds/save! after)
          {:status 200
           :session session
           :body (json/json-str {:count (:points after)})})  
        {:status 200
         :session session
         :body (json/json-str {:count -1})})))


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
      ["test"] (-> (str user/current-user) response constantly)
;      ["promise"] #(get-new-news %1)
      ["promise"] #(get-new-news %1)
      ["deliver"] #(update %1)
      [""] (-> ( news-template (get-news)) response constantly)))


(ae/def-appengine-app Swenews-app #'Swenews-app-handler)

