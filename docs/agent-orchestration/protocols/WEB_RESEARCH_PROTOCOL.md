# Web Research Protocol

Каждый phase и каждый технический пункт начинается с web research.

## Почему

Знания модели могут быть устаревшими. Нельзя полагаться на внутренние знания по текущим Android/KMP, ML Kit, VLM runtime, graph digitization, OpenCV/BoofCV, Compose UI и scientific reporting practices.

## Источники приоритета

1. Official docs: Android, Jetpack Compose, Kotlin Multiplatform, ML Kit, OpenCV/BoofCV.
2. Maintained open-source projects.
3. Peer-reviewed / recent technical papers.
4. Vendor docs для Gemma/Qwen/VLM runtimes.
5. Проверенные engineering blogs только как вторичные источники.

## Required output

Каждый агент сохраняет:

`docs/research/YYYY-MM-DD_<topic>.md`

Содержание:

- topic;
- sources checked;
- what changed vs assumptions;
- selected approach;
- rejected approach;
- implementation impact;
- test impact;
- open risks.

## Anti-patterns

- “Я знаю, как это делается” без research note.
- Использование устаревших APIs без проверки.
- Добавление библиотеки без проверки Android/KMP совместимости.
