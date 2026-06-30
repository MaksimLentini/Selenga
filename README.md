# Selenga

Мессенджер на чистом HTML/CSS/JS с бэкендом на Firebase (Authentication + Firestore).
Никакого собственного сервера для данных не нужно — вся логика чата работает прямо в браузере пользователя.

## Структура проекта

```
selenga/
├── index.html          # всё приложение: разметка, стили, логика, подключение Firebase
├── firestore.rules      # правила безопасности базы данных Firestore
├── package.json          # нужен только для деплоя как Node Web Service (см. ниже)
├── server.js            # минимальный сервер, который отдаёт index.html (тоже только для Node-варианта)
└── README.md
```

## Настройка Firebase (один раз)

1. console.firebase.google.com → создать проект.
2. Authentication → Sign-in method → включить **Email/Password**.
3. Firestore Database → создать базу.
4. Firestore Database → Rules → вставить содержимое `firestore.rules` → Publish.
5. Project settings → добавить веб-приложение → скопировать `firebaseConfig`.
6. Открыть `index.html`, найти в самом низу `const firebaseConfig = {...}` и вставить свои значения.

## Деплой на Render

Файл — статичный сайт, серверный код (Node/npm) ему не нужен. Есть два способа.

### Вариант A — Static Site (рекомендуется)

1. Render → New → **Static Site**.
2. Подключить репозиторий.
3. Build Command — оставить пустым.
4. Publish Directory — `.` (если `index.html` лежит в корне репозитория).
5. Deploy.

### Вариант B — Web Service на Node

Использует `package.json` и `server.js` из этого проекта.

1. Render → New → **Web Service**.
2. Build Command: `npm install`
3. Start Command: `npm start`
4. Deploy.

Сервер ничего не хранит — он лишь отдаёт `index.html` браузеру; все данные пользователей идут напрямую в Firebase.

## Локальный запуск

Можно просто открыть `index.html` в браузере двойным кликом, либо поднять сервер:

```bash
npm install
npm start
```

и зайти на `http://localhost:3000`.
