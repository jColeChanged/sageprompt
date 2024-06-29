(ns tacktech.llm.schemas-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [malli.core :as m]
            [clojure.string]
            [malli.generator :as mg]
            [tacktech.llm.schemas :as sut]))

;; Custom generator for valid external models
(def gen-valid-external-model
  (gen/fmap
   (fn [[provider model]]
     (str provider "/" model))
   (gen/tuple
    (gen/elements (vec sut/supported-providers))
    (gen/not-empty gen/string-alphanumeric))))

;; Test that valid external models pass validation
(defspec invalid-external-models-fail-validation 100
  (prop/for-all [invalid-model (gen/one-of
                                [(gen/return "invalid/model")
                                 (gen/return "openai/")
                                 (gen/return "/gpt-4")
                                 (gen/fmap
                                  (fn [s] (str "unsupported/" s))
                                  gen/string-alphanumeric)])]
                (not (m/validate sut/external-model-schema invalid-model))))

;; Test that split-external-model correctly splits valid models
(defspec split-external-model-works-correctly 100
  (prop/for-all [model gen-valid-external-model]
                (let [{:keys [provider model]} (sut/split-external-model model)]
                  (and (contains? sut/supported-providers provider)
                       (string? model)
                       (not (clojure.string/blank? model))))))

;; Test that our custom generator produces valid models
(defspec custom-generator-produces-valid-models 100
  (prop/for-all [model gen-valid-external-model]
                (m/validate sut/external-model-schema model)))

;; Traditional unit test for a specific case
(deftest specific-model-test
  (testing "Known good model passes validation"
    (is (m/validate sut/external-model-schema "openai/gpt-4")))
  (testing "Known bad model fails validation"
    (is (not (m/validate sut/external-model-schema "unsupported/model"))))
  (testing "split-external-model works correctly"
    (is (= {:provider "openai" :model "gpt-4"}
           (sut/split-external-model "openai/gpt-4")))))

(deftest split-external-model-test
  (testing "split-external-model function"
    (testing "with valid input"
      (is (= {:provider "openai" :model "gpt-4"}
             (sut/split-external-model "openai/gpt-4")))
      (is (= {:provider "anthropic" :model "claude-2"}
             (sut/split-external-model "anthropic/claude-2")))
      (is (= {:provider "openai" :model "gpt-3.5-turbo"}
             (sut/split-external-model "openai/gpt-3.5-turbo"))))

    (testing "with model name containing slashes"
      (is (= {:provider "openai" :model "gpt-4/test/model"}
             (sut/split-external-model "openai/gpt-4/test/model"))))))

(deftest advanced-split-external-model-tests

  (testing "Extremely long model names"
    (let [long-model-name (apply str (repeat 10000 "a"))]
      (is (= {:provider "openai" :model long-model-name}
             (sut/split-external-model (str "openai/" long-model-name))))))

  (testing "Adversarial inputs"
    (is (= {:provider "openai" :model "gpt-4\u0000hidden"}
           (sut/split-external-model "openai/gpt-4\u0000hidden")))
    (is (= {:provider "openai" :model "gpt-4\n\rnewescape"}
           (sut/split-external-model "openai/gpt-4\n\rnewescape"))))

  (testing "Circular reference in model name"
    (is (= {:provider "openai" :model "openai/gpt-4"}
           (sut/split-external-model "openai/openai/gpt-4"))))

  (testing "Internationalization support"
    (is (= {:provider "openai" :model "智能模型-1"}
           (sut/split-external-model "openai/智能模型-1")))
    (is (= {:provider "yandex" :model "Русская-Модель-1"}
           (sut/split-external-model "yandex/Русская-Модель-1")))))

(defspec fuzz-test-split-external-model 1000
  (prop/for-all [s gen/string-ascii]
                (try
                  (let [result (sut/split-external-model s)]
                    (and (map? result)
                         (contains? result :provider)
                         (contains? result :model)))
                  (catch Exception e
                    false))))

(deftest external-query-schema-test
  (testing "Valid external queries"
    (is (m/validate sut/external-query-schema
                    {:model "openai/gpt-4"
                     :messages [{:role "user" :content "Hello, world!"}]}))

    (is (m/validate sut/external-query-schema
                    {:model "anthropic/claude-2"
                     :messages [{:role "system" :content "You are a helpful assistant."}
                                {:role "user" :content "Tell me a joke."}]
                     :temperature 0.7
                     :max-tokens 100})))

  (testing "Invalid external queries"
    (is (not (m/validate sut/external-query-schema
                         {:model "unsupported/gpt-4"
                          :messages [{:role "user" :content "Hello"}]})))

    (is (not (m/validate sut/external-query-schema
                         {:model "openai/gpt-4"
                          :messages "Not a vector"})))

    (is (not (m/validate sut/external-query-schema
                         {:model "openai/gpt-4"
                          :messages [{:role "invalid-role" :content "Hello"}]})))))

(deftest internal-query-schema-test
  (testing "Valid internal queries"
    (is (m/validate sut/internal-query-schema
                    {:provider "openai"
                     :model "gpt-4"
                     :messages [{:role "user" :content "Hello, world!"}]}))

    (is (m/validate sut/internal-query-schema
                    {:provider "anthropic"
                     :model "claude-2"
                     :messages [{:role "system" :content "You are a helpful assistant."}
                                {:role "user" :content "Tell me a joke."}]})))

  (testing "Invalid internal queries"
    (is (not (m/validate sut/internal-query-schema
                         {:provider "unsupported-provider"
                          :model "gpt-4"
                          :messages [{:role "user" :content "Hello"}]})))

    (is (not (m/validate sut/internal-query-schema
                         {:provider "openai"
                          :model ""
                          :messages [{:role "user" :content "Hello"}]})))

    (is (not (m/validate sut/internal-query-schema
                         {:provider "openai"
                          :model "gpt-4"
                          :messages "Not a vector"})))

    (is (not (m/validate sut/internal-query-schema
                         {:provider "openai"
                          :model "gpt-4"
                          :messages [{:role "invalid-role" :content "Hello"}]})))))


(deftest external->internal-query-test
  (testing "Conversion from external to internal query"
    (is (= {:provider "openai"
            :model "gpt-4"
            :messages [{:role "user" :content "Hello"}]}
           (sut/external->internal-query
            {:model "openai/gpt-4"
             :messages [{:role "user" :content "Hello"}]})))

    (is (= {:provider "anthropic"
            :model "claude-2"
            :messages [{:role "user" :content "Hello"}]
            :max-tokens 100
            :temperature 0.7}
           (sut/external->internal-query
            {:model "anthropic/claude-2"
             :messages [{:role "user" :content "Hello"}]
             :max-tokens 100
             :temperature 0.7})))

    (is (= {:provider "lmstudio"
            :model "llama-7b"
            :messages [{:role "user" :content "Hello"}]
            :temperature 0.5
            :top-p 0.9
            :n 3
            :stream true
            :stop ["." "!"]
            :presence-penalty 0.6
            :frequency-penalty -0.2
            :logit-bias {:20392 1 :43929 -1}
            :user "user123"}
           (sut/external->internal-query
            {:model "lmstudio/llama-7b"
             :messages [{:role "user" :content "Hello"}]
             :temperature 0.5
             :top-p 0.9
             :n 3
             :stream true
             :stop ["." "!"]
             :presence-penalty 0.6
             :frequency-penalty -0.2
             :logit-bias {:20392 1 :43929 -1}
             :user "user123"})))))