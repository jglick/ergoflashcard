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

(quail-define-package "lao-latin" "Lao" "LL" nil "Lao text in Latin transliteration acc. to Lao Basic Course." nil nil nil t nil nil nil nil nil nil nil)
(quail-define-rules
 ;; Special letters:
 ("\\n" ?ŋ)
 ("\\e" ?ɛ)
 ("\\a" ?ə)
 ("\\o" ?ɔ)
 ;; Tone marks:
 ("\\-" ?̅)
 ("\\`" ?̀)
 ("\\v" ?̌)
 ("\\'" ?́)
 ("\\^" ?̂)
 )

(define-derived-mode lao-ergoflashcard-mode ergoflashcard-mode "Lao ErgoFlashCard"
  "Major mode for editing Lao ergoflashcard data files."
  ; XXX modify-syntax-entry
  (quail-use-package "lao-latin")
)

(provide 'lao-ergoflashcard-mode)
