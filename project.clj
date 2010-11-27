(defproject Swenews "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  :namespaces [Swenews.app_servlet]
    :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [enlive  "1.0.0-SNAPSHOT"]
                 [net.cgrand/moustache "1.0.0-SNAPSHOT"]
                 [ring/ring-core "0.2.5"]
                 [ring/ring-devel "0.2.5"]
                 [joda-time "1.6"]
                 [ring/ring-jetty-adapter "0.2.5"]]
    :dev-dependencies [[appengine-magic "0.3.0-SNAPSHOT"]])
