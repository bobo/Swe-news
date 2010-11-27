(ns Swenews.app_servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use Swenews.core)
  (:use [appengine-magic.servlet :only [make-servlet-service-method]]))


(defn -service [this request response]
  ((make-servlet-service-method Swenews-app) this request response))
