(ns tacktech.finetune.core 
  (:require [tacktech.llm.api :refer [internal-create-chat-completion]]
            [tacktech.prompts :refer [load-prompt-content-from-disk-caching]]))

(def generation-prompt (load-prompt-content-from-disk-caching "prompts/generate.txt"))
(def rating-prompt     (load-prompt-content-from-disk-caching "prompts/discriminate.txt"))


(defn generate
  [prompt temperature provider model]
  (internal-create-chat-completion provider model {:messages [{:role "system" :content prompt}]
                                                   :temperature temperature}))





(def generate-completion
  (partial generate generation-prompt 0.5 "openai" "gpt-3.5-turbo"))
