(ns fixme.status-bot.core
  (:require [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [dgellow.bottle.bot
             :refer [defaction
                     make-bot
                     make-envelope
                     stop-all! start-all!]]
            [clojure.java.shell :refer [sh]])
  (:import [dgellow.bottle.bot SlackAdapter])
  (:gen-class))

;; Actions
(defaction hello-action
  "Greets the user"
  #"hello"
  (fn [_ _]
    (make-envelope
     {:message "Hello, I'm the status bot."})))

(defaction ping-foo-action
  "Ping foo.fixme.ch"
  #"ping foo"
  (fn [_ _]
    (let [{:keys [exit out err]} (sh "ping" "-c" "3" "foo.fixme.ch")]
      (make-envelope
       {:message
        (format "\n[Exit code] %s\n[Output]\n%s\n[Error]\n%s"
                exit out err)}))))

(defaction ping-router-action
  "Ping router (62.220.131.170)"
  #"ping router"
  (fn [_ _]
    (let [{:keys [exit out err]} (sh "ping" "-c" "3" "62.220.131.170")]
      (make-envelope
       {:message
        (format "\n[Exit code] %s\n[Output]\n%s\n[Error]\n%s"
                exit out err)}))))

(defrecord Service [service ok?])

(defn make-pingservice [label domain-or-ip]
  (Service. label (= 0 (:exit (sh "ping" "-c" "3" domain-or-ip)))))

(defn make-webservice [url]
  (Service. url (= 0 (:exit (sh "curl" url)))))

(defaction status-foo-action
  "Gives status of services on foo.fixme.ch"
  #"status"
  (fn [_ _]
    (let [pingservices (map (fn [x] (apply make-pingservice x))
                         (partition 2 ["ping router (62.220.131.170)" "62.220.131.170"
                                       "ping foo.fixme.ch" "foo.fixme.ch"]))
          webservices (map make-webservice
                        ["http://foo.fixme.ch"
                         "https://foo.fixme.ch"
                         "https://git.fixme.ch"
                         "https://pad.fixme.ch"
                         "https://wiki.fixme.ch"
                         "https://trigger.fixme.ch"
                         "https://mpd.fixme.ch"])
          services (concat pingservices webservices)]
      (make-envelope
       {:message
        (apply str "Services status:\n"
          (map #(format "%s\t%s\n"
                        (if (:ok? %) ":white_check_mark:" ":x:")
                        (:service %))
            services))}))))

(defaction help-action
  "Display help"
  #"help"
  (fn [_ {:keys [bot]}]
    (make-envelope
     {:message
      (format "\n%s\n"
              (clojure.string/join
               "\n"
               (map #(format "- \"%s\": %s" (:pattern %)
                             (:doc (meta %))) (:actions bot))))})))

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
  (stop-all! @bot)
  (reset! bot nil))
(defn start-bot! []
  (when @bot
    (stop-bot!))
  (timbre/info "Starting bot...")
  (reset! bot bot-spec)
  (when @bot
    (start-all! @bot)))

(defn -main [& args]
  (start-bot!))
