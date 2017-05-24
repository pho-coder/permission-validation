(ns permission-validation.core
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]))

(def cli-options
  [["-f" "--ips-file file" "ip list file"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: program-name [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  migrate-jobs  get kill and submit scribes"
        "  query-jobs    query jobs"
        ""
        "Please refer to the manual page for more information."]
       (clojure.string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn check [ips-file]
  (log/info ips-file)
  (loop [ips (clojure.string/split-lines (slurp ips-file))
         i 1]
    (when-let [one (first ips)]
      (log/info i one)
      (let [[ip name] (clojure.string/split one #",")
            ip (.substring ip 1 (dec (.length ip)))
            name (.substring name 1 (dec (.length name)))
            mysql-db {:dbtype "mysql"
                      :host ip
                      :port 3358
                      :dbname "information_schema"
                      :user name
                      :password "FdHTbSjheGVNQwEDNSXXOO_D2efdsdF"}]
        (log/info ip name)
        (try
          (let [re (j/query mysql-db
                            ["show grants;"])]
            (log/info re)
            (let [grants (first (vals (first re)))]
              (if (and (.contains grants "SELECT")
                       (.contains grants "REPLICATION SLAVE")
                       (.contains grants "REPLICATION CLIENT"))
                (spit "success" (str one "," grants "\n") :append true)
                (spit "wrong" (str one "," grants "\n") :append true))))
          (catch Exception e
            (log/error e)
            (spit "error" (str one "," (:cause (Throwable->map e)) "\n") :append true)))
        (recur (rest ips)
               (inc i))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (log/info options)
    (log/info arguments)
    (cond
      (:help options) (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 2 (usage summary))
      errors (exit 3 (error-msg errors)))

    (let [file (:ips-file options)]
      (case (first arguments)
        "check" (check file)
        (exit 4 (usage summary))))))
