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
 ("\\ee" ?ɛ)
 ("\\aa" ?ə)
 ("\\oo" ?ɔ)
 ;; Not exactly sure what this is, but used occasionally:
 ("\\?" ?ʔ)
 ;; Bare tone marks:
 ("\\-" ?̄)
 ("\\`" ?̀)
 ("\\v" ?̌)
 ("\\'" ?́)
 ("\\^" ?̂)
 ;; Better combinations for tones on 'i' (combining w/ dotted and dotless 'i' is bad):
 ("\\i-" ?ī)
 ("\\i`" ?ì)
 ("\\iv" ?ĭ) ; actually a breve - my Java fonts have no glyph for small letter i with caron
 ("\\i'" ?í)
 ("\\i^" ?î)
 ;; Other combos:
 ("\\a`" ?à)
 ("\\a'" ?á)
 ("\\a^" ?â)
 ;; No A-macron, A-caron
 ("\\e`" ?è)
 ("\\e'" ?é)
 ("\\e^" ?ê)
 ("\\ev" ?ě)
 ;; No E-macron
 ("\\o`" ?ò)
 ("\\o'" ?ó)
 ("\\o^" ?ô)
 ;; No O-macron, O-caron
 ("\\u`" ?ù)
 ("\\u'" ?ú)
 ("\\u^" ?û)
 ("\\u-" ?ū)
 ;; No U-caron
 ("\\y'" ?ý)
 ("\\y^" ?ŷ)
 ;; No Y-grave, Y-macron, Y-caron
 ;; No preaccented schwas, epsilons, or "turned C's"
 )

(define-derived-mode lao-ergoflashcard-mode ergoflashcard-mode "Lao ErgoFlashCard"
  "Major mode for editing Lao ergoflashcard data files."
  ; XXX modify-syntax-entry
  (quail-use-package "lao-latin")
)

(provide 'lao-ergoflashcard-mode)
