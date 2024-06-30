(ns tacktech.llm.api 
  (:require [clojure.tools.analyzer.ast.query :as query]
            [wkok.openai-clojure.api :as api]))



;; (defn internal-create-chat-completion
;;   [provider model messages]

;;   (case provider
;;     "openai" (openai-api/create-chat-completion {:model model :messages messages})
;;     "anthropic" (anthropic-api/create-chat-completion {:model model :messages messages})
;;     "lmstudio" (lmstudio-api/create-chat-completion {:model model :messages messages})
;;     (throw (ex-info "Unsupported provider" {:provider provider}))))


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