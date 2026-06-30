// Минимальный сервер: просто отдаёт статичный index.html.
// Вся логика (Firebase Auth, Firestore) работает в браузере пользователя —
// сервер не хранит и не обрабатывает никаких данных.

const express = require('express');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.static(path.join(__dirname)));

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Selenga запущена на порту ${PORT}`);
});
