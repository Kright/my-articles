# Бриф для переводчика (RU -> EN)

Инструкция для агента, который переводит одну статью. Словарь терминов и устойчивых решений - в [glossary.md](glossary.md):
его нужно прочитать до перевода и придерживаться; новые удачные решения стоит предлагать в отчёте.

Одобренные переводы - любые `*.en.md` в этом репозитории, они служат образцом регистра.

---


You are translating a blog note from Russian into English for its author, Igor (nickname Kright), a programmer (Scala/Kotlin/Java, physics, gamedev, programming language design; works at JetBrains). The author's explicit wish: "the text is a reflection of how I think, and I want to preserve that nuance". So this is NOT a job for polished, neutral, corporate-blog English. The English text should read as if the same person wrote it in English: same voice, same rhythm, same level of informality, same dry humour, same way of structuring a thought.

## Step 1 - study the author's style before translating

The author's articles live in `/home/lgor/projects/2025/my-articles/` (Markdown, grouped by year folders; ignore `.git`, `.idea`, `.obsidian`, and code subprojects). Read your target article first, completely. Then read 3-4 other Russian articles of his to get a feel for the voice (pick ones close in topic to yours plus one opinionated one, e.g. "Хабр мёртв" in 2025). Use `ls`/`find` to locate them.

Approved translations already exist - every `*.en.md` file in the repository. Read at least two of them together with their originals as the reference for the target register, for example:
- `2026/Fused multiply-add/Fused multiply-add.md` + `.en.md`
- `2026/Делаю свой язык программирования/Делаю свой язык программирования.md` + `.en.md`
- `2024/О дизайне языков программирования/О дизайне языков программирования.md` + `.en.md` (long; an excerpt is enough)

Also read `_translation/glossary.md` in the repository root and stick to it.

Style observations from previous translators (verify against your own reading):
- Notes open straight with the point, no warm-up intro. One thought per paragraph.
- Sentences are short or medium; long ones are chained from simple pieces with commas and "и", not with subordinate clauses.
- Examples are introduced casually: "Например, ...", "Ещё пример: ...", "Из минусов - ...", "В общем и целом - ...". The spaced hyphen " - " works as a pause instead of a linking verb.
- First person without embarrassment ("я нашёл", "на мой взгляд"), conversational "мы" in examples, sometimes impersonal "можно".
- Colloquial words with attitude sit right inside technical text: "фишка", "аж", "штуки", "реально", "офигенно", "казалось бы", "как ни странно", "вдруг", "нейронка". Irony is dry, one phrase, no emoji.
- States his observations bluntly, hedges ("может", "на мой взгляд") only where genuinely unsure. No officialese.
- The reader is assumed to be a programmer; terms are not explained.
- English terms stay as they are, mixed into the Russian.

Resulting conventions for the English (already used in the approved translations - stay consistent with them):
- contractions (it's, there's, you'd), "we"/"you" as in the original, short sentences;
- " - " (spaced hyphen) instead of em dashes; American spelling;
- "нейронка/нейронки" -> "the LLM"/"LLMs" (not "neural net", not "AI"); "модель" -> "model";
- sentences start with a capital letter even where the original is sloppy about it; missing full stops at the end of list items/paragraphs are kept as in the original;
- fragments stay fragments, notes stay notes - do not turn them into essay prose.

## Step 2 - translate

Save the translation as a NEW file next to the original, same name with `.en.md` instead of `.md`.

- Front matter of the new file: copy the original's front matter, replace only `title` with your English title. Keep `author` and `date` exactly. Do NOT add `lang` or `ref` - the site pipeline adds them from the `.en.md` suffix. If the title contains a colon or quotes, keep it in double quotes (valid YAML).
- Title: in the author's voice - plain, not a headline or clickbait.
- Translate the WHOLE article to the very end. No summarizing, no placeholders, no skipped or merged parts.
- Keep the structure 1:1: same paragraphs in the same order, same headings at the same levels, same lists and nesting, same tables, same code blocks, same images, same links. Do not add, remove, "improve", reorder or fact-correct content; no intro, conclusion or translator's notes.
- Code blocks stay byte-identical (including language tags), except Russian comments/strings inside them, which should be translated. Do not fix or reformat code, even if it looks wrong - report it instead.
- Images: keep `![...](img/...)` / `![...](imgs/...)` paths exactly as in the original (they are shared between the two language versions). Translate alt text if there is any. If an image contains Russian text that matters for understanding, mention it in the report.
- Links: external links stay as they are, except links to Russian Wikipedia (ru.wikipedia.org) - replace with the corresponding English article when one clearly exists. Links to habr.com etc. stay, without "(in Russian)" notes. Relative links to the author's other `.md` articles: if a `.en.md` version of the target exists on disk, point to it, otherwise keep the link unchanged; list them in the report.
- Obvious typos in the Russian prose: just write the correct English, and list the typo in the report. Anything technically questionable, outdated or ambiguous: translate faithfully as written and mention it in the report.
- Preserve the voice. Keep the small words that carry attitude; find natural English equivalents, not literal calques, but don't sanitize. Keep opinions as blunt or as hedged as in the original. Don't inflate short sentences into long formal ones. Avoid LLM-English tells: no "delve", "it's worth noting", "leverage", "moreover", "furthermore", "crucial", no marketing tone, no em dashes.
- Use standard English terminology of the field - what a practising programmer would naturally write.
- Natural, correct English comes first: where a literal rendering of a Russian construction would sound off, restructure the sentence, but keep the thought's shape and the attitude.

Do NOT modify the original Russian file or any other existing file; do not run git commands that change anything. Only create your one new `.en.md` file. Other translators are working concurrently on other articles in the same repository - don't touch their folders.

After writing, self-check with grep/wc and fix any discrepancy:
- number of headings (lines starting with `#` outside code blocks) equals the original, same order and levels;
- number of fenced code block markers (```) equals the original, and code blocks are identical to the original apart from translated comments (verify with diff);
- number of list items, links `](` and images `![` equals the original;
- no Cyrillic left in the English file (grep for `[а-яА-ЯёЁ]`), except inside link URLs/paths that must stay unchanged;
- word count is plausible relative to the original (English usually comes out 10-25% longer in words; a much shorter file means something was dropped).

## Step 3 - report back (in Russian, compact)

1. The English title you chose and 1-2 alternatives.
2. The places where the translation required a real choice (idioms, jokes, ambiguous or fragmentary phrases, competing terminology): original -> chosen -> why / alternatives, so the author can quickly review and override. A table is fine; up to ~15 rows.
3. Typos and technically doubtful / outdated / ambiguous places in the original (report only), with enough context to find them.
4. Relative links and images found, and what you did with them.
5. Self-check numbers (original vs translation) and the path of the created file.
Do not repeat the generic style observations from this brief; only add what is new or different.
