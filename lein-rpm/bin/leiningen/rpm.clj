(ns leiningen.rpm)

(defn rpm
  "I don't do a lot."
  [project & args]
  (let [{:keys [name version dependencies target-path]} project]
   (doseq [[d-name d-version] dependencies] (prn d-name))
  (println "Hi! " (str name version  target-path) )
  
  )
  )
