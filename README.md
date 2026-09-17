# TranslateOCR

## License
(c) 2026 - 2027 Joan Jerez, ToBeIT
GPL 3

(c) 2023 - 2024 Joan Jerez
GPL 3

Portions of code:
(c) 2014 - 2016 Mikel Artetxe
GPL 3

Application icon:
(c) 2008 - 2014 The Oxygen Team
LGPL 3

This project includes third party libraries; please see libraries/ for copyright details pertaining to them.

## Language pairs (Apertium)

Main page: https://wiki.apertium.org/wiki/Main_Page
List of language pairs: https://wiki.apertium.org/wiki/List_of_language_pairs

### English

1. [ ] English <-\> Catalan
2. [ ] English <-\> Spanish
3. [ ] Welsh -> English
4. [ ] Serbo-Croatian -> English
5. [ ] Icelandic -> English

### Romanic

1. [ ] Spanish <-\> Catalan
2. [ ] French <-\> Catalan
3. [ ] Spanish <-\> Portuguese
4. [ ] Catalan <-\> Italian
5. [x] French <-\> Spanish
6. [ ] Romanian <-\> Catalan
7. [ ] Portuguese <-\> Catalan

### Nordic

1. [ ] Swedish <-\> Danish
2. [ ] Icelandic <-\> Swedish
3. [ ] Danish <-\> Norwegian
4. [ ] Swedish <-\> Norwegian

### Slavic

1. [ ] Serbocroatian <-\> Slovenian
2. [ ] Macedonian <-\> Bulgarian
3. [ ] Belarusian <-\> Russian
4. [ ] Russian <-\> Ukrainian

## Language support (Firefox Translation)

Main page: https://mozilla.github.io/translations/
List of models trained: https://mozilla.github.io/translations/model-registry/

### English 
1. [ ] Afrikaans <-\> English
2. [ ] Albanian -\> English
3. [ ] Arabic <-\> English
4. [ ] Azerbaijani <- English
5. [ ] Bangla <-\> English
6. [ ] Basque <-\> English
7. [ ] Bosnian <-\> English
8. [ ] Bulgarian <-\> English
9. [ ] Catalan <-\> English
10. [ ] Chinese <-\> English
11. [ ] Croatian <- English
12. [ ] Czech <-\> English
13. [ ] Danish <-\> English
14. [ ] Dutch <-\> English
15. [ ] Estonian <-\> English
16. [ ] Finnish <-\> English
17. [ ] French <-\> English
18. [ ] Galician <-\> English
19. [ ] German <-\> English
20. [ ] Greek <-\> English
21. [ ] Gujarati <-\> English
22. [ ] Hebrew <-\> English
23. [ ] Hindi <-\> English
24. [ ] Hungarian <-\> English
25. [ ] Iceland <-\> English
26. [ ] Indonesian <-\> English
27. [ ] Italian <-\> English
28. [ ] Japanese <-\> English
29. [ ] Kannada <-\> English
30. [ ] Korean <-\> English
31. [ ] Latvian <-\> English
32. [ ] Lithuanian <-\> English
33. [ ] Malay <-\> English
34. [ ] Malayalam <-\> English
35. [ ] Marathi <-\> English
36. [ ] Norwegian <-\> English
37. [ ] Norwegian Bökmal <-\> English
38. [ ] Persian <-\> English
39. [ ] Polish <-\> English
40. [ ] Portuguese <-\> English
41. [ ] Romanian <-\> English
42. [ ] Russian <-\> English
43. [ ] Serbian <-\> English
44. [ ] Slovak <-\> English
45. [ ] Slovenian <-\> English
46. [ ] Spanish <-\> English
47. [ ] Swedish <-\> English
48. [ ] Tamil <-\> English
49. [ ] Telugu <-\> English
50. [ ] Thai <-\> English
51. [ ] Traditional Chinese <-\> English
52. [ ] Turkish <-\> English
53. [ ] Ukrainian <-\> English
54. [ ] Urdu <-\> English
55. [ ] Vietnamese <-\> English

## Language support (Tesseract OCR)

Several languages are supported in different versions of Tesseract: https://tesseract-ocr.github.io/tessdoc/Data-Files-in-different-versions.html

In TranslateOCR, we will support only English [eng].

## Language support (eSpeak NG)

`$ espeak-ng --voices`

1. [x] Belarusian
2. [x] Bulgarian
3. [x] Catalan
4. [x] Danish
5. [x] English
6. [x] French
7. [x] Icelandic
8. [x] Italian
9. [x] Macedonian
10. [x] Norwegian
11. [x] Portuguese
12. [x] Romanian
13. [x] Russian
14. [x] Serbo-Croatian
15. [x] Slovenian
16. [x] Spanish
17. [x] Ukrainian
18. [x] Welsh

## Language support (MMS)

All languages supported: https://dl.fbaipublicfiles.com/mms/asr/mms1b_all_langs.html

## Planning

### Phase 1: Update dependencies & test

Latest SDK

### Phase 2: Dictionaries compatible with lttoolbox-java

Compile last version of Appium dictionaries compatible with lttoolbox-java.

### Phase 3: Add eSpeak library

espeak-ng: https://github.com/espeak-ng/espeak-ng/tree/master/android

### Phase 4: Add Bergamot Translator

Firefox Translations: https://firefox-source-docs.mozilla.org/toolkit/components/translations/resources/03_bergamot.html

