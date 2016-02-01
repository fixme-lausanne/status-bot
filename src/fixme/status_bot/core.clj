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
  #"ping foo"
  (fn [_ _]
    (let [{:keys [exit out err]} (sh "ping" "-c" "3" "foo.fixme.ch")]
      {:message
       (format "\n[Exit code] %s\n[Output]\n%s\n[Error]\n%s"
               exit out err)})))

(defaction ping-router-action
  "Ping router (62.220.131.170)"
  #"ping router"
  (fn [_ _]
    (let [{:keys [exit out err]} (sh "ping" "-c" "3" "62.220.131.170")]
      {:message
       (format "\n[Exit code] %s\n[Output]\n%s\n[Error]\n%s"
               exit out err)})))

(defaction status-foo-action
  "Gives status of services on foo.fixme.ch"
  #"status"
  (fn [_ _]
    (let [ping (= 0 (:exit (sh "ping" "-c" "3" "foo.fixme.ch")))
          ping-router (= 0 (:exit (sh "ping" "-c" "3" "62.220.131.170")))
          curl-http (= 0 (:exit (sh "curl" "http://foo.fixme.ch")))
          curl-https (= 0 (:exit (sh "curl" "https://foo.fixme.ch")))
          curl-gogs (= 0 (:exit (sh "curl" "https://git.fixme.ch")))
          curl-pad (= 0 (:exit (sh "curl" "https://pad.fixme.ch")))
          curl-wiki (= 0 (:exit (sh "curl" "https://wiki.fixme.ch")))
          curl-trigger (= 0 (:exit (sh "curl" "https://trigger.fixme.ch")))
          curl-mpd (= 0 (:exit (sh "curl" "https://mpd.fixme.ch")))
          services [{:service "ping router (62.220.131.170)"
                     :ok? ping-router}
                    {:service "ping foo.fixme.ch" :ok? ping}
                    {:service "http://foo.fixme.ch" :ok? curl-http}
                    {:service "https://foo.fixme.ch" :ok? curl-https}
                    {:service "https://git.fixme.ch" :ok? curl-gogs}
                    {:service "https://pad.fixme.ch" :ok? curl-pad}
                    {:service "https://wiki.fixme.ch" :ok? curl-wiki}
                    {:service "https://trigger.fixme.ch" :ok? curl-trigger}
                    {:service "https://mpd.fixme.ch" :ok? curl-mpd}]]
      {:message
       (apply str "Services status:\n"
         (map #(format "%s\t%s\n"
                       (if (:ok? %) ":white_check_mark:" ":x:")
                       (:service %))
           services))})))

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
                         ping-foo-action
                         ping-router-action
                         status-foo-action]
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
