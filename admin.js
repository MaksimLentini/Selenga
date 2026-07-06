// Admin panel script — показывает и удаляет сообщения (доступно только для пользователя с логином Lentini)
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-app.js";
import { getAnalytics } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-analytics.js";
import { getAuth, onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-auth.js";
import { getFirestore, collection, query, orderBy, onSnapshot, deleteDoc, doc } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyDQQapEceixlN9GdcoivtVNt6OMmJBQZgQ",
  authDomain: "selenga-67940.firebaseapp.com",
  projectId: "selenga-67940",
  storageBucket: "selenga-67940.firebasestorage.app",
  messagingSenderId: "899774964729",
  appId: "1:899774964729:web:359c2dfcf9fe990c8d5328",
  measurementId: "G-TCW6RDCF37"
};
const app = initializeApp(firebaseConfig);
try{ getAnalytics(app); }catch(e){}
const auth = getAuth(app);
const db = getFirestore(app);

const adminStatus = document.getElementById('admin-status');
const adminControls = document.getElementById('admin-controls');
const adminMessages = document.getElementById('admin-messages');

let unsub = null;

onAuthStateChanged(auth, (user)=>{
  if(!user){
    adminStatus.textContent = 'Войдите в систему под админом (Lentini)';
    adminControls.classList.add('hidden');
    return;
  }
  const name = user.displayName || user.email;
  if(name !== 'Lentini'){
    adminStatus.textContent = `Доступ запрещён — вы вошли как ${name}`;
    adminControls.classList.add('hidden');
    return;
  }
  adminStatus.textContent = `Вошли как админ: ${name}`;
  adminControls.classList.remove('hidden');
  // start listening
  const q = query(collection(db,'messages'), orderBy('createdAt'));
  unsub = onSnapshot(q, (snap)=>{
    adminMessages.innerHTML = '';
    snap.docs.forEach(d=>{
      const data = d.data();
      const row = document.createElement('div');
      row.className = 'message';
      const left = document.createElement('div');
      left.innerHTML = `<div><strong>${escapeHtml(data.username)}</strong></div><div>${escapeHtml(data.text)}</div>`;
      const del = document.createElement('button');
      del.textContent = 'Удалить';
      del.addEventListener('click', async ()=>{
        if(confirm('Удалить сообщение?')){
          await deleteDoc(doc(db,'messages',d.id));
        }
      });
      row.appendChild(left);
      row.appendChild(del);
      adminMessages.appendChild(row);
    });
  });
});

function escapeHtml(s){ return s ? s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;') : s; }

