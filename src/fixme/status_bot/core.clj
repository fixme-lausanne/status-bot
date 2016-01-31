(ns fixme.status-bot.core
  (:require [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [dgellow.bottle.bot :refer [defaction make-bot close-all! run-all!]]
            [clojure.java.shell :refer [sh]])
  (:import [dgellow.bottle.bot SlackAdapter])
  (:gen-class))

;; Actions
(defaction hello-action
  "Greets the user"
  #"hello"
  (fn [_ _] {:message "Hello, I'm the status bot."}))

(defaction ping-foo-action
  "Ping foo.fixme.ch"
  #"ping"
  (fn [_ _]
    (let [{:keys [exit out err]} (sh "ping" "-c" "3" "foo.fixme.ch")]
      {:message
       (format "\n[Exit code] %s\n[Output]\n%s\n[Error]\n%s"
               exit out err)})))

(defaction help-action
  "Display help"
  #"help"
  (fn [_ {:keys [bot]}]
    {:message
     (format "\n%s\n"
             (clojure.string/join
              "\n"
              (map #(format "- \"%s\": %s" (:pattern %)
                            (:doc (meta %))) (:actions bot))))}))

;; Bot definition
(def bot-spec
  (let [slack-token (env :slack-api-token)]
    (assert slack-token "Missing environment variable SLACK_API_TOKEN")
    (make-bot {:name "status_bot"
               :actions [hello-action
                         help-action
                         ping-foo-action]
               :adapters [(SlackAdapter. slack-token)]})))

;; Entry points
(def bot
  (atom nil))
(defn stop-bot! []
  (timbre/info "Stopping bot...")
  (close-all! @bot)
  (reset! bot nil))
(defn start-bot! []
  (when @bot
    (stop-bot!))
  (timbre/info "Starting bot...")
  (reset! bot bot-spec)
  (when @bot
    (run-all! @bot)))

(defn -main [& args]
  (start-bot!))
