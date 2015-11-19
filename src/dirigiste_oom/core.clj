(ns dirigiste-oom.core
  (:gen-class)
  (:import
   [io.aleph.dirigiste Pool IPool IPool$AcquireCallback IPool$Generator Pools]))

(defn field [obj field-name]
  (-> obj
      (.getClass)
      (.getDeclaredField field-name)
      (doto (.setAccessible true))
      (.get obj)))

(defn run-checks [max-iterations dispose-times new-pool default-key]
  (let [^IPool pool (new-pool)
        ^java.util.Set destroyed-objects (field pool "_destroyedObjects")]
    (println "_destroyedObjects size before check" (.size destroyed-objects))
    (dotimes [i max-iterations]
      (let [obj (.acquire pool default-key)]
        (dotimes [_ dispose-times]
          (.dispose pool default-key obj))))
    (println "_destroyedObjects size after check" (.size destroyed-objects))))

(defn -main [& args]
  (let [utilization 0.9
        max-queue-size 65536
        max-per-key 8
        max-total 1024
        max-rand 10000000
        new-generator #(reify IPool$Generator
                         (generate [_ k] (str k (rand-int max-rand)))
                         (destroy [_ k v]))
        new-pool #(Pools/utilizationPool (new-generator)
                                         max-queue-size
                                         utilization
                                         max-per-key
                                         max-total)
        max-iterations 1000
        default-key "test"]
    (println "Dispose each object only once")
    (run-checks max-iterations 1 new-pool default-key)
    (println)
    (println "Dispose each object twice")
    (run-checks max-iterations 2 new-pool default-key)))
