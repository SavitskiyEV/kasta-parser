(ns kasta.core
  (:require [clj-http.client :as client])
  (:require [clj-time.core :as t])
  (:require [clj-time.local :as l])
  (:gen-class))

(def menu-url "https://modnakasta.ua/api/v2/market/menu/")

(def page-url "https://modnakasta.ua/market/")

(def category-products-url "https://modnakasta.ua/api/v2/product-list")

(def max-expected-node-products-count 10)

(def nodes-count 0)
(def small-categories-count 0)

(defn get-node-products-count [node] 
  (def node-product-url ( clojure.string/join [category-products-url ( :q node)]))
  (def node-products ( :product-ids ( :body (client/get node-product-url {:as :json}))))

  ( count node-products)
)

(defn process-leaf-node [node breadcrumbs]
         
  (def products-count (get-node-products-count node) )

  (if ( < products-count max-expected-node-products-count) 
    (do 
      (println (clojure.string/join (clojure.string/join " > " 
      (conj (vec (map :name breadcrumbs )) ( :name node) ))) "|" 
        (clojure.string/replace (clojure.string/join [page-url ( :url node)]) #" " ""))
        (def small-categories-count (+ small-categories-count 1))
    )
  )
)

(defn parse-menu [response-body]
;(println nodes-count)
  (def breadcrumbs ( :breadcrumbs response-body)) 
  (def menu-nodes ( :nodes response-body))
  (def grouped-nodes ((juxt filter remove) #(= ( :has-children %) true) menu-nodes))
  (doseq [child-node (first grouped-nodes) ] 
     @(parse-menu ( :body (client/get menu-url {:as :json, 
                                                :query-params{"v" "1", 
                                                              "parent-uuid" 
                                                                ( :uuid child-node)}}))))

  (doseq [leaf (second grouped-nodes)] (process-leaf-node leaf breadcrumbs) )
  (def nodes-count (+ nodes-count (count ( :nodes response-body))))
)
      

(defn -main
  "Processing kasta market and printing nodes route depending on their content"
  [& args]  
  (println (l/local-now))
  (def root-menu ( :body (client/get menu-url {:as :json, :query-params {"v" "1"}})))
  (parse-menu root-menu)
  (print (* 100 (float ( / small-categories-count nodes-count))))
  (print " % из ")
  (print nodes-count)
  (print " категорий")
  )