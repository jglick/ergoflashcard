(require 'cl)

; (makunbound 'quiz-font-lock-keywords)
(defconst quiz-font-lock-keywords
  `(
    ("^\\([^#:\n][^:\n]*\\)\\(:\\)\\([^:\n]+\\)\\(\\(:\\)\\([^:\n]+\\)\\)?$"
     (1 font-lock-function-name-face)
     (2 font-lock-comment-face)
     (3 'default)
     (5 font-lock-comment-face nil t)
     (6 font-lock-type-face nil t)
     )
    ("^\\([^#:\n \t][^#:\n]*\\)\n" (0 ,(if (boundp 'font-lock-builtin-face) 'font-lock-builtin-face 'font-lock-reference-face)))
    ("/" (0 ,(if (boundp 'font-lock-warning-face) 'font-lock-warning-face 'quiz-bogus-face) t))
    ("|" (0 font-lock-string-face t))
    ("^#.*$" (0 font-lock-comment-face t))
    ("^[^#\n].*\n" (0 quiz-bogus-face nil))
    ))

;; Cribbed from make-mode.el:
(defface quiz-bogus-face
   '((((class color)) (:background  "hotpink"))
     (t (:reverse-video t)))
  "*Face to use for bogus text in Quiz mode."
  :group 'faces)
(if (boundp 'facemenu-unlisted-faces)
    (add-to-list 'facemenu-unlisted-faces 'quiz-bogus-face))
(defvar quiz-bogus-face 'quiz-bogus-face
  "Face to use for bogus text in Quiz mode.")

(define-derived-mode quiz-mode text-mode "Quiz"
  "Major mode for editing quiz data files."
  (setq case-fold-search t)
  (auto-fill-mode -1)
  (set (make-local-variable 'font-lock-defaults)
       '(quiz-font-lock-keywords t t))
  (modify-syntax-entry ?/ "."))

(provide 'quiz-mode)
