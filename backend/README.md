# Medtest1 backend (локальный сервер + SQLite)

## Запуск локально (Windows)

Нужен **Node.js 22+** (встроенный модуль `node:sqlite`; без `better-sqlite3` и без Visual Studio / Python для сборки нативных модулей).

Открыть терминал в папке `backend` и выполнить:

```bash
npm install
set PORT=8080
set DB_FILE=./data.sqlite
set JWT_SECRET=change-me
set ADMIN_KEY=change-admin-key
npm run dev
```

Проверка:

- `GET http://localhost:8080/health`
- `GET http://localhost:8080/api/admin/users` с заголовком `x-admin-key: <ADMIN_KEY>`

## Код сброса пароля — отправка на **email**

Без настройки почты запрос «Отправить код» вернёт **`503`** и `error: "mail_not_configured"`.

Нужно задать **один** из вариантов:

### Вариант A: Resend (проще для теста)

1. Зарегистрируйтесь на [resend.com](https://resend.com), создайте API key.
2. В PowerShell перед `npm run dev`:

```powershell
$env:RESEND_API_KEY="re_xxxxxxxx"
$env:RESEND_FROM="onboarding@resend.dev"
$env:PORT="8081"
npm.cmd run dev
```

Письма с тестового домена `resend.dev` уходят **только на ваш подтверждённый email** в аккаунте Resend. Для любых адресов подключите свой домен в Resend и задайте `RESEND_FROM`.

### Вариант B: SMTP (например Gmail)

```powershell
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_USER="your@gmail.com"
$env:SMTP_PASS="пароль-приложения-Google"
$env:MAIL_FROM="your@gmail.com"
```

Для Gmail включите двухфакторную аутентификацию и создайте **пароль приложения**.

### Только локальная отладка без почты

```powershell
$env:ALLOW_RESET_WITHOUT_MAIL="1"
```

Код будет только в консоли (`[password-reset]`). В JSON ответа будет `mailDelivered: false`.

При ошибке SMTP/Resend — **`500`**, `error: "mail_failed"`. Опционально: `set SHOW_RESET_CODE=1` — код дублируется в JSON `devCode`.

## Синхронизация пароля после сброса в Firebase

В приложении сброс пароля идёт через **Firebase** (письмо со ссылкой). Пароль в **SQLite** на backend при этом сам не меняется. После смены пароля по ссылке пользователь входит в приложение: клиент авторизуется в Firebase с **новым** паролем, отправляет **idToken** на сервер, и тот обновляет `password_hash` и выдаёт JWT.

Нужна проверка токена на сервере через **Firebase Admin SDK**:

1. В [Firebase Console](https://console.firebase.google.com/) → Project settings → Service accounts → **Generate new private key** (JSON-файл).
2. Перед запуском backend задайте **один** из вариантов:

```powershell
# путь к скачанному JSON (удобно на Windows)
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\path\to\serviceAccountKey.json"
```

или одной строкой (без переносов внутри JSON):

```powershell
$env:FIREBASE_SERVICE_ACCOUNT_JSON='{"type":"service_account",...}'
```

Без этого `POST /api/auth/sync-password-from-firebase` вернёт **`503`** и `error: "firebase_admin_not_configured"`: новый пароль будет работать только в Firebase, старый — в backend.

### Обновление backend на Render (фото и ответы в чате)

Сообщение в приложении *«Обновите backend на Render»* значит: на сервере ещё **старая** версия API без загрузки фото. Нужно задеплоить код из папки `backend` этого репозитория.

#### Проверка, что сервер уже новый

В PowerShell:

```powershell
Invoke-RestMethod -Uri "https://medtest1-backend.onrender.com/health"
```

Должно быть примерно: `ok: True`, `version: 2`, `features.supportImages: True`. Если `version` нет или `supportImages` нет — деплой ещё не обновился.

#### Вариант A: Render подключён к GitHub (рекомендуется)

1. Закоммитьте и запушьте проект в репозиторий, к которому привязан Render.
2. Откройте [Render Dashboard](https://dashboard.render.com/) → сервис **medtest1-backend**.
3. Убедитесь: **Root Directory** = `backend` (как в корневом `render.yaml`).
4. Нажмите **Manual Deploy** → **Deploy latest commit** (или дождитесь автодеплоя после push).
5. Дождитесь статуса **Live** (зелёный), снова проверьте `/health`.

#### Вариант B: Только папка backend в отдельном репозитории

Если в Render указан репозиторий только с содержимым `backend/`, используйте `backend/render.yaml` и те же шаги Manual Deploy.

#### После деплоя

- Переустановите **release**-APK (или соберите заново), если меняли только сервер — достаточно обновить backend.
- **ADMIN_KEY** после redeploy может смениться, если Render сгенерировал его заново: Dashboard → Environment → `ADMIN_KEY` → скопируйте в приложение (Настройки → админ-режим).
- На бесплатном Render файлы фото и база при **новом** деплое могут обнуляться — это ограничение платформы.

### Backend на Render (production)

В [Render Dashboard](https://dashboard.render.com/) → ваш сервис → **Environment** добавьте:

| Переменная | Значение |
|------------|----------|
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Весь JSON из Firebase Console (Service accounts → Generate new private key), **одной строкой** |

Сохраните → **Manual Deploy**. Локальный `data.sqlite` на ПК **не связан** с базой на Render.

Просмотр пользователей в облачной БД (без DB Browser на сервере):

```powershell
$adminKey = "ВАШ_ADMIN_KEY_ИЗ_RENDER"
Invoke-RestMethod -Uri "https://medtest1-backend.onrender.com/api/admin/users" -Headers @{ "x-admin-key" = $adminKey }
```

### Скачать `data.sqlite` с Render (бесплатный план)

На **Free** нет SSH/Shell. Сначала проверьте, есть ли данные на сервере:

```powershell
$adminKey = "ВАШ_ADMIN_KEY_ИЗ_RENDER"
Invoke-RestMethod -Uri "https://medtest1-backend.onrender.com/api/admin/database/stats" -Headers @{ "x-admin-key" = $adminKey }
```

Если `users: 0` — в облачной БД пока никто не зарегистрировался через **release**-APK (или база обнулилась после redeploy на Render).

Скачивание (нужен `ADMIN_KEY`):

```powershell
$uri = "https://medtest1-backend.onrender.com/api/admin/database/export"
Invoke-WebRequest -Uri $uri -Headers @{ "x-admin-key" = $adminKey } -OutFile "$env:USERPROFILE\Downloads\render-data.sqlite"
```

Откройте в DB Browser → **Данные** → `users`.

**Важно:** на бесплатном Render при каждом **новом деплое** файл `./data/data.sqlite` может **создаваться заново пустым**. Данные на телефоне (локальная SQLite в приложении) в этот файл **не попадают**.

## Эндпоинты

- `POST /api/auth/register` `{ username, email, password, deviceId, appVersion, platform }`
- `POST /api/auth/login` `{ usernameOrEmail, password, deviceId, appVersion, platform }`
- `POST /api/auth/sync-password-from-firebase` `{ idToken, password, deviceId, appVersion, platform }` — проверка Firebase idToken, обновление пароля в БД, ответ как у login (`token`, `user`)
- `POST /api/auth/forgot-password` `{ username, email }` — создаёт код, **отправляет на email** (если настроена почта). Ответ `{ ok, mailDelivered?, devCode? }`. Ошибки: `login_email_mismatch`, `mail_not_configured`, `mail_failed`.
- `POST /api/auth/verify-reset-code` `{ username, email, code }` — проверяет код **без** смены пароля (для перехода к вводу нового пароля в приложении).
- `POST /api/auth/reset-password` `{ username, email, code, newPassword }` — смена пароля
- `POST /api/events/install` `{ deviceId, appVersion, platform, username? }`
- `DELETE /api/users/me` (Bearer токен)
- `GET /api/admin/users` (x-admin-key)
- `GET /api/admin/installs` (x-admin-key)
- `GET /api/support/messages` (Bearer) → сообщения с `replyPreview*` и `imageUrl`
- `POST /api/support/messages` (Bearer) `{ text, replyToMessageId?, imageUrl? | imageBase64+imageMimeType }`
- `POST /api/support/upload-image` (Bearer) `{ imageBase64, imageMimeType }` → `{ imageUrl }`
- `GET /api/admin/support/conversations` (x-admin-key)
- `GET /api/admin/support/conversations/:id/messages` (x-admin-key)
- `POST /api/admin/support/conversations/:id/messages` (x-admin-key) — как у пользователя
- `POST /api/admin/support/conversations/:id/upload-image` (x-admin-key)
