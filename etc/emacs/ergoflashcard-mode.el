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

(require 'cl)

; (makunbound 'ergoflashcard-font-lock-keywords)
(defconst ergoflashcard-font-lock-keywords
  ;; Still a few things that should match that don't - e.g. "foo- -bar", "t-shirt", "pre- and post-" - but oh well.
  (let ((segment "\\(\\([^#\n-]\\|-[^\n ]\\)\\([^\n-]\\|[^\n ]-[^\n ]\\| -[^\n ]\\|[^\n ]- \\)*\\([^\n ]-\\)?\\)")) ; 4 parens
  `(
    (,(concat "^" segment "\\( - \\)" segment "\\(\\( - \\)" segment "\\)?$")
     (1 font-lock-function-name-face)
     (5 font-lock-comment-face)
     (6 'default)
     (11 font-lock-comment-face nil t)
     (12 font-lock-type-face nil t)
     )
    (,(concat "^" segment "\n") (0 ,(if (boundp 'font-lock-builtin-face) 'font-lock-builtin-face 'font-lock-reference-face)))
    (" / " (0 ,(if (boundp 'font-lock-warning-face) 'font-lock-warning-face 'ergoflashcard-bogus-face) t))
    ("^#.*$" (0 font-lock-comment-face t))
    ("^[^#\n].*\n" (0 ergoflashcard-bogus-face nil))
    )))

;; Cribbed from make-mode.el:
(defface ergoflashcard-bogus-face
   '((((class color)) (:background  "hotpink"))
     (t (:reverse-video t)))
  "*Face to use for bogus text in Ergoflashcard mode."
  :group 'faces)
(if (boundp 'facemenu-unlisted-faces)
    (add-to-list 'facemenu-unlisted-faces 'ergoflashcard-bogus-face))
(defvar ergoflashcard-bogus-face 'ergoflashcard-bogus-face
  "Face to use for bogus text in Ergoflashcard mode.")

(define-derived-mode ergoflashcard-mode text-mode "Ergoflashcard"
  "Major mode for editing ergoflashcard data files."
  ;; XXX need to set encoding to UTF-8
  (setq case-fold-search t)
  (auto-fill-mode -1)
  (set (make-local-variable 'font-lock-defaults)
       '(ergoflashcard-font-lock-keywords t t))
  (modify-syntax-entry ?/ "."))

(provide 'ergoflashcard-mode)
