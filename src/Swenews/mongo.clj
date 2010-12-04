(ns Swenews.mongo
  (:use somnium.congomongo))  
(mongo!  
  :db "mydb") 



(defn insert-news [news]
  (insert! :news
           news))

(defn get-news []
  (fetch :news :limit 10))
