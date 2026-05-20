Две разные картинки — два файла:

1) Фото 1 (галочка + таблетка, синий градиент) — иконка приложения на телефоне:
   Сохраните как: tools\launcher_icon_source.png

2) Фото 2 (коробка с лекарствами, календарь, капсула) — круг на экране «Добро пожаловать»:
   Сохраните как: tools\welcome_icon_source.png

Затем в PowerShell из папки Medtest1:

   powershell -ExecutionPolicy Bypass -File .\tools\setup-all-icons.ps1

Пересоберите APK и переустановите приложение.
