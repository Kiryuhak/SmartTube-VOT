<div align="center">
  <img src="smarttubetv/src/stvot/res/mipmap-nodpi/ic_stvot.png" width="150" alt="SmartTube Vox Icon">
  <h1>SmartTube Vox</h1>
  <h3>SmartTube для Android TV с VOT-переводом и Живым голосом</h3>
  <p>Неофициальная сборка (форк) SmartTube с интеграцией голосового перевода Яндекс VOT, авторизацией через Яндекс ID и поддержкой автоматических обновлений.</p>
</div>

<p align="center">
  <a href="https://github.com/Kiryuhak/SmartTube-Vox/releases/latest"><img src="https://img.shields.io/github/v/release/Kiryuhak/SmartTube-Vox?color=blue&label=%D0%A0%D0%B5%D0%BB%D0%B8%D0%B7" alt="Release"></a>
  <a href="https://github.com/Kiryuhak/SmartTube-Vox/releases"><img src="https://img.shields.io/github/downloads/Kiryuhak/SmartTube-Vox/total?color=brightgreen&label=%D0%A1%D0%BA%D0%B0%D1%87%D0%B8%D0%B2%D0%B0%D0%BD%D0%B8%D0%B9" alt="Downloads"></a>
  <a href="https://android.com/tv/"><img src="https://img.shields.io/badge/%D0%9F%D0%BB%D0%B0%D1%82%D1%84%D0%BE%D1%80%D0%BC%D0%B0-Android%20TV%20%7C%20Google%20TV-blueviolet" alt="Platform"></a>
  <a href="https://github.com/Kiryuhak/SmartTube-Vox/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/Kiryuhak/SmartTube-Vox/ci.yml?branch=main&label=CI" alt="CI"></a>
  <a href="https://github.com/Kiryuhak/SmartTube-Vox/releases/latest"><img src="https://img.shields.io/badge/%D0%9E%D0%B1%D0%BD%D0%BE%D0%B2%D0%BB%D0%B5%D0%BD%D0%B8%D1%8F-In--App%20Updater-success" alt="Updates"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/%D0%9B%D0%B8%D1%86%D0%B5%D0%BD%D0%B7%D0%B8%D1%8F-GPLv3-orange" alt="License"></a>
</p>

---

## 📥 Скачать SmartTube Vox

> **Основная ссылка для скачивания:**
>
> 📦 **[Скачать smarttube_vox.apk (Universal APK)](https://github.com/Kiryuhak/SmartTube-Vox/releases/latest/download/smarttube_vox.apk)**
>
> 🔄 *Совместимый alias для существующих установок и скриптов:* **[smarttube_vot.apk](https://github.com/Kiryuhak/SmartTube-Vox/releases/latest/download/smarttube_vot.apk)**
>
> Все актуальные сборки и архивы версий доступны в разделе **[GitHub Releases](https://github.com/Kiryuhak/SmartTube-Vox/releases/latest)**.

Рекомендуется скачивать SmartTube Vox **только из официальных Releases этого репозитория**. Не используйте сторонние непроверенные сайты и каталоги APK во избежание подмены установочных файлов.

---

## 📖 О проекте

> SmartTube Vox — независимая неофициальная сборка (форк) SmartTube для Android TV и Android TV Box с интеграцией голосового перевода Яндекс VOT, авторизацией через Яндекс ID и поддержкой «Живого голоса».

Приложение использует собственный отдельный идентификатор пакета:
```text
io.github.kiryuhak.smarttubevot.stable
```
Благодаря отдельному package SmartTube Vox можно устанавливать параллельно с оригинальным SmartTube (как Stable, так и Beta). Они не перезаписывают друг друга, не затирают пользовательские данные и могут работать одновременно на одном устройстве.

---

## ✨ Ключевые возможности

### 🎙 VOT и Живой голос
- **Закадровый перевод видео**: мгновенная русская голосовая озвучка иностранных роликов с помощью нейросетей.
- **Авторизация Яндекс ID прямо на TV**: быстрое и удобное подключение аккаунта без ввода логина и пароля с пульта.
- **«Живой голос»**: многоголосые реалистичные нейросетевые модели автоматически активируются после успешного входа.
- **Раздельный микшер звука**: независимая регулировка громкости оригинальной дорожки видео и громкости перевода.

### 🌍 Умный автоперевод
- **Русская аудиодорожка** ➔ автоперевод не запускается.
- **Подтверждённая иностранная дорожка** ➔ перевод может стартовать автоматически.
- **Неизвестный язык или дорожка без тега** ➔ автоматического запуска нет (исключены ложные срабатывания).
- **Ручной запуск** ➔ в любой момент доступен по кнопке на панели управления плеера.

### 📺 Возможности SmartTube
SmartTube Vox унаследовал все проверенные временем возможности базового плеера:
- Полноценный интерфейс, адаптированный под экраны телевизоров и управление пультом ДУ;
- Встроенный SponsorBlock (автоматический пропуск спонсорских вставок и интеграций);
- Поддержка высоких разрешений до 4K, 60 кадров/сек (FPS) и расширенного динамического диапазона (HDR);
- Автоподстройка частоты обновления экрана (AFR);
- Гибкое управление скоростью воспроизведения;
- Отсутствие обязательной зависимости от Google Services.

> Полное описание базового плеера доступно в репозитории [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube) и на сайте [smarttubeapp.github.io](https://smarttubeapp.github.io/).

### 🔄 Автообновление
- Автоматическая проверка новых релизов SmartTube Vox;
- Список изменений (changelog) на русском языке прямо на экране ТВ;
- Фоновое скачивание нового APK;
- Бесшовное обновление установленной сборки без потери настроек.

---

## ⚖️ SmartTube vs SmartTube Vox

| Возможность | SmartTube | SmartTube Vox |
|---|:---:|:---:|
| Android TV интерфейс | ✅ | ✅ |
| SponsorBlock | ✅ | ✅ |
| VOT-перевод | — | ✅ |
| Яндекс ID | — | ✅ |
| Живой голос | — | ✅ |
| Умный автозапуск VOT | — | ✅ |
| Отдельный package | — | ✅ |
| Собственный канал обновлений | — | ✅ |

---

## 🚀 Установка

SmartTube Vox рассчитан прежде всего на телевизоры и приставки под управлением Android TV и Google TV.

1. **Скачать** файл `smarttube_vox.apk` из раздела [Releases](https://github.com/Kiryuhak/SmartTube-Vox/releases/latest).
2. **Разрешить** установку приложений из неизвестных источников в настройках безопасности Android TV.
3. **Установить** APK (через приложение Downloader, с USB-флешки или через ADB).
4. **Запустить** SmartTube Vox на телевизоре.
5. При необходимости **выполнить вход** через Яндекс ID для активации «Живого голоса».

---

## 🔐 Яндекс ID и Живой голос

Авторизация в Яндекс ID устроена просто и безопасно:

1. Пользователь запускает авторизацию из настроек плеера или меню перевода.
2. На экране ТВ отображается QR-код и короткий код устройства для перехода на страницу Яндекс ID (`ya.ru/device`).
3. После подтверждения входа на смартфоне или компьютере приложение получает OAuth-доступ.
4. Токен авторизации сохраняется локально в защищённом хранилище приложения на ТВ.
5. Режим «Живой голос» с нейросетевой озвучкой становится доступен автоматически.

> [!NOTE]
> **Безопасность данных**:
> - Авторизация работает по стандарту OAuth 2.0 Device Authorization Grant — пароли на ТВ не вводятся.
> - OAuth-токен никогда не выводится в системные логи.
> - Секретные ключи не вшиваются в открытый код APK.
> - Пользователь может в любой момент выйти из аккаунта и удалить токен в настройках приложения.

---

## 🔄 Обновления

Обновление SmartTube Vox работает по прямой и обратно-совместимой схеме:

```text
SmartTube Vox
     ↓
smarttube_vox.json / smarttube_vot.json  (манифесты обновления)
     ↓
GitHub Release
     ↓
smarttube_vox.apk / smarttube_vot.apk   (готовые бинарники)
```

Каждый GitHub Release содержит новые APK-файлы (`smarttube_vox.apk`) и метаданные (`smarttube_vox.json`), а также compatibility aliases (`smarttube_vot.apk`, `smarttube_vot.json`) одновременно. Благодаря этому как новые установки SmartTube Vox, так и ранее установленные версии Echo продолжают бесшовно получать обновления.

### Формат версий
В проекте используется двухуровневая схема версий:
- `32.45-vot.1`
- `32.45-vot.2`
- `32.46-vot.1`

Первая часть (`32.45`, `32.46`) строго соответствует базовой версии оригинального SmartTube, а суффикс `-vot.N` обозначает порядковый номер ревизии SmartTube Vox с исправлением или улучшением функционала перевода.

---

## 🖼 Скриншоты

<!--
| Главный экран | Авторизация Яндекс | VOT |
|---|---|---|
| ![](docs/screenshots/home.png) | ![](docs/screenshots/oauth.png) | ![](docs/screenshots/vot.png) |
-->

*Скриншоты интерфейса будут добавлены позже.*

---

## 📝 История изменений

Подробный список изменений для каждой выпущенной версии доступен в файле:
- 📋 **[CHANGELOG.md](CHANGELOG.md)**

Информация о самом свежем релизе и прикреплённых файлах доступна на странице **[Latest Release](https://github.com/Kiryuhak/SmartTube-Vox/releases/latest)**.

---

## 💻 Для разработчиков

<details>
<summary><b>🛠 Сборка из исходников</b></summary>

### Требования
- **JDK 17** (Eclipse Temurin, OpenJDK 17 или Azul Zulu)
- **Android SDK** (Build-Tools `34.0.0`, SDK Platform `34`)
- **Git**

### Сборка Debug APK
```bash
./gradlew assembleStvotDebug
```
Собранный файл появится в `smarttubetv/build/outputs/apk/stvot/debug/`.

### Сборка Release APK
Сборка подписанного релизного варианта (при наличии файла `keystore.properties`):
```bash
./gradlew assembleStvotRelease
```

### Параметры сборки
- **Package ID**: `io.github.kiryuhak.smarttubevot.stable`
- **Формула VersionCode**: `baseVersionCode * 1000 + votPatch` (строго в диапазоне `1..999` для сохранения монотонности Android).
</details>

<details>
<summary><b>⚙️ CI/CD</b></summary>

В проекте настроен автоматизированный конвейер на базе GitHub Actions:
```text
Pull Request
    ↓
CI (проверка форматирования, изоляции зависимостей)
    ↓
Сборка stvotDebug и ststableDebug
    ↓
Code Review
    ↓
Merge в main
    ↓
Публикация тега v<base>-vot.<patch>
    ↓
Signed Release (8 этапов валидации)
    ↓
GitHub Release
    ↓
Встроенный Updater на ТВ
```

> **Важно**: Изменения из апстрима никогда не вливаются в ветку `main` автоматически без проверки и подтверждения мейнтейнером.
</details>

---

## ❤️ Благодарности

SmartTube Vox существует благодаря работе авторов и сообществ исходных open-source проектов:

- **[SmartTube](https://github.com/yuliskov/SmartTube)** — **Юрий Лисков ([@yuliskov](https://github.com/yuliskov))** и всё сообщество разработчиков оригинального плеера SmartTube ([официальный сайт](https://smarttubeapp.github.io/)) за великолепный и независимый YouTube-клиент для Android TV.
- **[vsvoice/SmartTube](https://github.com/vsvoice/SmartTube)** — за исследовательскую работу, первоначальную концепцию и адаптацию интеграции голосового перевода в плеер SmartTube.
- **[voice-over-translation](https://github.com/ilyhalight/voice-over-translation)** — **Илья Лайт ([@ilyhalight](https://github.com/ilyhalight))** за открытую библиотеку закадрового перевода и анализ протоколов голосового сервиса.
- **[vot-cli](https://github.com/FOSWLY/vot-cli)** и **[vot.js](https://github.com/FOSWLY/vot.js)** — команда **FOSWLY ([@FOSWLY](https://github.com/FOSWLY))** за активное развитие открытых инструментов голосового перевода.

*Указанные проекты и их авторы не несут ответственности за данную сборку и не осуществляют её официальную поддержку. Мы выражаем искреннюю признательность за их вклад в развитие открытого программного обеспечения.*

---

> [!IMPORTANT]
> **SmartTube Vox** является независимой неофициальной производной сборкой (форком). Проект не является официальной версией SmartTube и не является продуктом компании Яндекс или Google LLC. Все названия брендов, сервисов и товарные знаки принадлежат их законным правообладателям.

---

## 📄 Лицензия

Исходный код проекта распространяется в соответствии с условиями свободной лицензии **GNU General Public License v3.0 (GPLv3)** и лицензиями используемых upstream-компонентов.

Полный текст лицензии доступен в файле [LICENSE](LICENSE).

---

## 🐛 Нашли проблему?

Если у вас возникли сложности в работе приложения или есть идеи по его улучшению:
- 🐞 **[Создать Issue](https://github.com/Kiryuhak/SmartTube-Vox/issues)** — сообщить об ошибке или сбое.
- 📦 **[Посмотреть Releases](https://github.com/Kiryuhak/SmartTube-Vox/releases)** — проверить наличие новых сборок.
- 📝 **[Посмотреть CHANGELOG](CHANGELOG.md)** — ознакомиться с историей изменений.
