(ns tacktech.llm.api
  (:require [tacktech.llm.provider.core :refer [get-provider]]
            [wkok.openai-clojure.api :as api]))


(defn internal-create-chat-completion
  [provider model query]
  (println provider model query)
  (let [provider (get-provider provider)]
    (api/create-chat-completion 
     (assoc query :model model)
     provider)))


;;    (api/create-chat-completion {:model model :messages messages})))


;; (defn external-create-chat-completion
;;   [external-query]
;;   (if (m/validate external-query-schema external-query)
;;     (let [{:keys [provider model messages]} (external->internal-query external-query)]
;;       (internal-create-chat-completion provider model messages))
;;     (throw (ex-info "Invalid external query format" {:external-query external-query}))))




;; (comment

;;   (create-chat-completion nil {:model "gpt-3.5-turbo"
;;                                :messages [{:role "system" :content "You are a helpful assistant."}
;;                                           {:role "user" :content "Who won the world series in 2020?"}
;;                                           {:role "assistant" :content "The Los Angeles Dodgers won the World Series in 2020."}
;;                                           {:role "user" :content "Where was it played?"}]}))