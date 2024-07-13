(ns tacktech.finetune.core
  (:require [tacktech.llm.api :refer [internal-create-chat-completion]]
            [charred.api :as charred]
            [tacktech.prompts :refer [load-prompt-content-from-disk-caching]]))

(def generation-prompt (load-prompt-content-from-disk-caching "prompts/generate.txt"))
(def rating-prompt     (load-prompt-content-from-disk-caching "prompts/discriminate.txt"))

(defn generate
  [prompt temperature provider model]
  (internal-create-chat-completion provider model {:messages [{:role "system" :content prompt}]
                                                   :temperature temperature}))

(def generate-completion
  (partial generate generation-prompt 0.5 "openai" "gpt-3.5-turbo"))


(defn parse-rating-response
  [rating-response completion]
  (let [tool-output (-> rating-response
                        :choices
                        first
                        :message
                        :tool_calls
                        first
                        :function
                        :arguments)
        {correctness-score :correctness_score
         logic-score :logic_score
         analysis :analysis} (charred/read-json tool-output :key-fn keyword)]
    {:completion completion
     :correctness-score correctness-score
     :logic-score logic-score
     :analysis analysis}))

(defn rate-completion
  [completion]
  (let [rating-response (internal-create-chat-completion
                         "openai" "gpt-3.5-turbo"
                         {:messages [{:role "system" :content rating-prompt}
                                     {:role "user" :content (str "Please analyze the following example using the provided framework and return your response in the specified JSON format:\n\n" completion)}]
                          :temperature 0.2
                          :tools [{:type "function"
                                   :function {:name "analyze_example"
                                              :description "Analyze the example using the provided framework and return the correctness score and logic score."
                                              :parameters {:type "object"
                                                           :properties {:correctness_score {:type "number"}
                                                                        :logic_score {:type "number"}
                                                                        :analysis {:type "string"}}
                                                           :required ["correctness_score" "logic_score" "analysis"]}}}]
                          :tool_choice "auto"})
        parsed-response (parse-rating-response rating-response completion)]
    parsed-response))

;; Testing the updated functions
(comment
  (rate-completion "I'm a chicken. On the other side of the road I see chicken feed. It looks so yummy.
Avatar

Let's work through this using a structured decision-making process to help you decide whether to cross the road to get the feed.
1. Articulate the Problem

The problem is whether or not you should cross the road to get the chicken feed. We'll use the 5 Whys technique to break it down.

    Why do you want to cross the road? To get the chicken feed.
    Why do you want the chicken feed? Because it looks nutritious and tasty.
    Why is it important to get the nutritious feed? Because it will provide energy and improve health.
    Why is improving health important? To ensure longevity and better quality of life.
    Why do you need to ensure longevity and quality of life? Because it allows you to continue being happy and productive.

2. Determine Success Criteria

List the criteria for evaluating potential alternatives:

    Safety: The road must be safe to cross.
    Nutritional Value: The chicken feed should be nutritious.
    Time: The time required to cross the road.
    Effort: The energy required to cross the road.
    Risk: The risk involved in crossing the road (e.g., predators, vehicles).

3. Rank Criteria

Prioritize the criteria using a ranking chart:

    Safety
    Risk
    Nutritional Value
    Time
    Effort

4. Simplify Outcomes

Define possible outcomes for each criterion:

    Safety: Safe, Unsafe
    Risk: High Risk, Low Risk
    Nutritional Value: High Nutrition, Low Nutrition
    Time: Quick, Slow
    Effort: Low Effort, High Effort"))

;;response
;;(parse-rating-response response)

(defn prepare-trl-data
  [completions]
  (->> completions
       (map rate-completion)
       (map #(str (:prompt %) "\n" (:completion %) "\n" (:score %)))))

(defn generate-and-prepare-trl-data
  []
  (let [completion (generate-completion)]
    (prepare-trl-data [completion])))

;; (def trl-data (generate-and-prepare-trl-data))

;; (println trl-data)