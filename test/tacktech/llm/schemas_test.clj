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
(defspec valid-external-models-pass-validation 100
  (prop/for-all [model gen-valid-external-model]
                (m/validate sut/external-model-schema model)))

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