(ns kasta.core
  (:require [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.local :as l])
  (:gen-class))

(def menu-url "https://modnakasta.ua/api/v2/market/menu/")
(def page-url "https://modnakasta.ua/market/")
(def category-products-url "https://modnakasta.ua/api/v2/product-list")

(def max-expected-node-products-count 10)
(def *nodes-count* 0)
(def *matching-nodes-count* 0)

(defn get-node-products-count [node]
  (-> (clojure.string/join [category-products-url ( :q node)])
    (client/get {:as :json})
    :body
    :product-ids
    (count)))

(defn process-leaf-node [node breadcrumbs]         
  (when ( < (get-node-products-count node) max-expected-node-products-count) 
     (println (clojure.string/join (clojure.string/join " > " 
      (conj (vec (map :name breadcrumbs)) ( :name node)))) "|" 
        (clojure.string/replace (clojure.string/join [page-url 
                                                      ( :url node)]) 
                                  #" " ""))
        (alter-var-root #'*matching-nodes-count* (constantly (inc *matching-nodes-count*)))))

(defn parse-menu [response-body]
  (def grouped-nodes 
    ((juxt filter remove) #(= ( :has-children %) true) ( :nodes response-body)))
  ;I have no idea how to get rid off that def and keep code simple. I mean, let 
  ; with lambda that does all code below is a solution but I am unsure. Answer is welcome!
  (doseq [child-node (first grouped-nodes) ] 
          (parse-menu 
           ( :body (client/get menu-url {:as :json, 
                                        :query-params{"v" "1", 
                                                      "parent-uuid" 
                                                        ( :uuid child-node)}}))))
  (doseq [leaf (second grouped-nodes)] 
    (process-leaf-node leaf 
      ( :breadcrumbs response-body)))
  (alter-var-root #'*nodes-count* (constantly (inc *nodes-count*))))
      

(defn -main
  "Processing kasta market and printing nodes route depending on their content"
  [& args]  
  (parse-menu ( :body (client/get menu-url {:as :json, :query-params {"v" "1"}})))
  (println (clojure.string/join [(* 100 (float (/ *matching-nodes-count* *nodes-count*))) 
                                " % из " 
                                *nodes-count* 
                                " категорий"])))