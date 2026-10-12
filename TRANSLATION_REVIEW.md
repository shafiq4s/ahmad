# Interface translation review

All seven interface resource sets are implemented. These are editorial drafts, not translations certified by native speakers. Quran text, surah names, source assets and original legal notices are not translated or changed. The translated About summary accompanies the original source notices; those notices remain authoritative.

## Afghan Hazaragi — haz-AF

**The entire Hazaragi set needs an Afghan Hazara native-speaker review before public release.** Written Hazaragi has regional spelling and register variations. This draft uses understandable Dari-derived vocabulary together with provisional colloquial Hazaragi forms; it does not claim to be an established orthographic standard. Review whether a professional Quran app should retain colloquial commands or use a more formal written register. Do not relabel this locale as Dari or Persian.

| Draft expression | Intended meaning | Review needed |
| --- | --- | --- |
| زبون | Language | Confirm spelling/register against زبان. |
| مصحف ره باز کو | Open Mushaf | Confirm imperative/register and ره/را spelling. |
| جستجو د قرآن | Search the Quran | Confirm د/در orthography. |
| شریک کدن | Share | Confirm the natural digital-interface verb. |
| اینجه / یکجه | Here / together | Confirm regional spelling and broader comprehensibility. |
| موجود استه / می‌شه / شوه | Is available / becomes | Confirm written forms and agreement across singular/plural messages. |
| صفحهٔ پیشتر | Previous page | Confirm navigation wording. |
| بی‌انترنت | Offline | Confirm preferred digital term. |
| نوترین / کهنه‌ترین | Newest / oldest | Confirm natural chronology labels. |
| خواندن‌های آخری | Recent readings | Confirm the best history-screen title. |

## Afghan Dari — fa-AF

| Draft expression | Intended meaning | Review needed |
| --- | --- | --- |
| قرائت‌های اخیر / سابقهٔ قرائت | Recent readings / reading history | Check whether قرائت sounds like audio rather than reading in this UI. |
| نشان‌های محفوظ / نشان کردن | Bookmarks / bookmark | Confirm a natural and consistent Afghan digital term. |
| کاپی / شریک ساختن | Copy / share | Confirm local preferred interface wording. |
| معلومات / پالیسی / انترنت | Information / policy / internet | Afghan-oriented vocabulary; review spelling and register. |
| اطلاعیهٔ جواز استفاده | License notice | Legal/editorial terminology review. |

## Afghan Pashto — ps-AF

| Draft expression | Intended meaning | Review needed |
| --- | --- | --- |
| امستنې | Settings | Confirm preferred Afghan interface term vs تنظیمات. |
| نښې / ساتل شوې نښې | Bookmarks | Check that the short label is unambiguous in a Mushaf reader. |
| ښکته کول / ښکته شوي مخونه | Download / downloaded pages | Confirm natural digital meaning. |
| د لوستلو مخینه / وروستي لوستونه | Reading history / recent readings | Check clarity and inflections. |
| د کارونې د جواز خبرتیا | License notice | Legal/editorial terminology review. |

## Modern Uzbek Latin — uz-UZ

Review `Xatcho‘plar` (bookmarks), `Mushaf`/`mushaf`, `juz`, and `Tungi rejim` (dark mode). Confirm punctuation for O‘/G‘ and Qur’on throughout the Latin-script UI. No Cyrillic Uzbek strings are included.

## Tajik Cyrillic — tg-TJ

Review `Нишонаҳо` (bookmarks), `Хонданҳои охирин` (recent readings), `мусҳаф`, `Ҷузъ`, `Ҳафс`, and `Огоҳиномаи иҷозатнома` (license notice), including noun agreement and legal summary terminology. The Tajik UI uses Cyrillic.

## Shared editorial checks

- Native speakers should review all long accessibility descriptions, errors and license/provenance summaries for natural phrasing and semantic accuracy.
- Technical names, publisher references, URLs and license identifiers stay intact. Quran.ws is described as independent; KFQC is identified only through the documented upstream provenance.
- Language autonyms are kept in resources and always displayed in their own scripts. Dates follow the chosen UI locale. Hazaragi uses `fa-AF` only for calendar presentation because Android/Java CLDR lacks reliable Hazaragi date data; the application locale, language name, resource qualifier and Hazaragi UI text remain `haz-AF`.
- The navigation descriptions intentionally retain the established Mushaf swipe direction in all languages: right advances, left goes back. UI controls change direction; page artwork and coordinates never mirror.
