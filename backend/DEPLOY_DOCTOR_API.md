# Деплой API врачей на Render

Если `GET https://medtest1-backend.onrender.com/health` возвращает `"version": 2` без `"doctorApi": true`,
а `GET /api/admin/doctors` — **404**, на Render задеплоен **старый код из GitHub**.

Redeploy без push **не поможет**: Render берёт код из репозитория, а не с вашего ПК.

## Что отправить в Git

Обязательные файлы backend:

- `backend/src/doctorApi.js` (новый)
- `backend/src/index.js` (registerDoctorRoutes + health version 3)
- `backend/src/db.js` (таблицы doctor_*)
- `backend/src/firebaseAdmin.js` (push для врачей)

## Команды (PowerShell)

```powershell
cd C:\Users\musta\AndroidStudioProjects\Medtest1
git checkout main
git add backend/src/doctorApi.js backend/src/index.js backend/src/db.js backend/src/firebaseAdmin.js
git commit -m "Add doctor API for admin panel and mobile app"
git push origin main
```

После push: Render Dashboard → **medtest1-backend** → **Manual Deploy** → Deploy latest commit.

## Проверка

```powershell
Invoke-WebRequest "https://medtest1-backend.onrender.com/health" -UseBasicParsing
```

Ожидается: `"version": 3`, `"doctorApi": true`.

Или (даже на старом health): `GET /api/admin/doctors` без ключа → **401**, не 404.
