;;; Copyright 2004 Jesse N. Glick
;;;
;;; Licensed under the Apache License, Version 2.0 (the "License");
;;; you may not use this file except in compliance with the License.
;;; You may obtain a copy of the License at
;;;
;;;     http://www.apache.org/licenses/LICENSE-2.0
;;;
;;; Unless required by applicable law or agreed to in writing, software
;;; distributed under the License is distributed on an "AS IS" BASIS,
;;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;;; See the License for the specific language governing permissions and
;;; limitations under the License.

(require 'ergoflashcard-mode)

(define-derived-mode irish-ergoflashcard-mode ergoflashcard-mode "Irish ErgoFlashCard"
  "Major mode for editing Irish ergoflashcard data files."
  (set (make-local-variable 'iso-language) "irish")
  (iso-accents-mode 1)
  (do ((char 128 (+ char 1)))
      ((= char 256) nil)
    (modify-syntax-entry char "w")))

(provide 'irish-ergoflashcard-mode)
