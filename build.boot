(set-env!
 :source-paths #{"src"}
 :dependencies '[[dgellow/bottle "0.0.1-SNAPSHOT"]
                 [environ "1.0.1"]
                 [com.taoensso/timbre "4.1.4"]
                 [org.clojure/clojure "1.7.0"]])

(task-options! pom {:project 'fixme/status-bot
                    :version "0.1.0-SNAPSHOT"
                    :description "The FIXME Hackerspace's status bot"}
               aot {:namespace #{'fixme.status-bot.core}}
               jar {:main 'fixme.status-bot.core})

(deftask deps [])

(deftask run-tests []
  (set-env! :source-paths #{"src" "test"})
  (comp (test)))

(deftask auto-test []
  (set-env! :source-paths #{"src" "test"})
  (comp (watch)
        (speak)
        (test)))

(deftask build
  "Build and package the project"
  []
  (comp (aot)
        (pom)
        (uber)
        (jar)))
