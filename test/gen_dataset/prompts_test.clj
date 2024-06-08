(ns gen-dataset.prompts-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.java.io :as io]
            [gen-dataset.prompts :refer [load-prompt-content-from-disk-non-caching load-prompt-content]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))


(def test-filename "test-prompt.txt")
(def test-content "This is a test prompt.")

(defn with-test-file [f]
  (spit test-filename test-content)
  (try
    (f)
    (finally
      (io/delete-file test-filename))))

(use-fixtures :each with-test-file)

(deftest test-load-prompt-content-from-disk-non-caching
  (testing "load-prompt-content-from-disk-non-caching function"
    (is (= test-content (load-prompt-content-from-disk-non-caching test-filename)))))

(deftest test-load-prompt
  (testing "load-prompt function with caching"
    (is (= test-content (load-prompt-content test-filename)))
    (is (= test-content (load-prompt-content test-filename)))))

(deftest test-load-prompt-file-not-found
  (testing "FileNotFoundException for non-existing file"
    (is (thrown? java.io.FileNotFoundException (load-prompt-content-from-disk-non-caching "non-existing-file.txt")))))

(deftest test-load-prompt-illegal-argument
  (testing "IllegalArgumentException for invalid argument"
    (is (thrown? IllegalArgumentException (load-prompt-content-from-disk-non-caching nil)))))

(deftest test-load-prompt-security-exception
  (testing "SecurityException or FileNotFoundException for denied read access"
    (try
      (load-prompt-content-from-disk-non-caching "/root/secret-file.txt")
      (is false "Expected exception not thrown")
      (catch java.io.FileNotFoundException e
        (is true "FileNotFoundException caught"))
      (catch java.lang.SecurityException e
        (is true "SecurityException caught")))))

(deftest test-load-empty-file
  (testing "Loading an empty file"
    (spit test-filename "")
    (is (= "" (load-prompt-content-from-disk-non-caching test-filename)))))

(deftest test-load-large-file
  (testing "Loading a large file"
    (let [large-content (apply str (repeat 100000 "x"))]
      (spit test-filename large-content)
      (is (= large-content (load-prompt-content-from-disk-non-caching test-filename))))))

(deftest test-load-file-special-characters
  (testing "Loading a file with special characters"
    (let [special-content "特殊字符"]
      (spit test-filename special-content)
      (is (= special-content (load-prompt-content-from-disk-non-caching test-filename))))))

(deftest test-load-caching-behavior
  (testing "Caching behavior"
    (is (= test-content (load-prompt-content test-filename)))
    ;; Modify the file after initial load to test if caching works
    (spit test-filename "Modified content")
    (is (= test-content (load-prompt-content test-filename)))))

(defspec test-generative-load-prompt 100
    (prop/for-all
     [content gen/string]
     (spit test-filename content)
     (= content (load-prompt-content-from-disk-non-caching test-filename))))
