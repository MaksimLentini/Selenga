// Firebase и логика клиента
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-app.js";
import { getAnalytics } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-analytics.js";
import { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword, onAuthStateChanged, signOut, updateProfile } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-auth.js";
import { getFirestore, collection, addDoc, serverTimestamp, query, orderBy, onSnapshot, doc, setDoc, getDoc } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-firestore.js";

// Вставляем конфиг Firebase (как прислали)
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
try{ getAnalytics(app); }catch(e){ /* analytics silently fails in some envs */ }
const auth = getAuth(app);
const db = getFirestore(app);

// DOM
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const showRegister = document.getElementById('show-register');
const showLogin = document.getElementById('show-login');
const authSection = document.getElementById('auth');
const chatSection = document.getElementById('chat');
const messagesDiv = document.getElementById('messages');
const messageForm = document.getElementById('message-form');
const messageInput = document.getElementById('message-input');
const userInfo = document.getElementById('user-info');
const displayNameSpan = document.getElementById('display-name');
const logoutBtn = document.getElementById('logout');

// Toggle forms
showRegister.addEventListener('click', (e)=>{ e.preventDefault(); loginForm.classList.add('hidden'); registerForm.classList.remove('hidden'); });
showLogin.addEventListener('click', (e)=>{ e.preventDefault(); registerForm.classList.add('hidden'); loginForm.classList.remove('hidden'); });

// Register
registerForm.addEventListener('submit', async (e)=>{
  e.preventDefault();
  const username = document.getElementById('reg-username').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  if(!username) return alert('Введите логин');
  try{
    const cred = await createUserWithEmailAndPassword(auth, email, password);
    await updateProfile(cred.user, { displayName: username });
    // Сохраняем профиль в Firestore
    await setDoc(doc(db, 'users', cred.user.uid), { username, email, createdAt: serverTimestamp() });
  }catch(err){ alert(err.message); }
});

// Login
loginForm.addEventListener('submit', async (e)=>{
  e.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  try{ await signInWithEmailAndPassword(auth, email, password); }catch(err){ alert(err.message); }
});

// Logout
logoutBtn.addEventListener('click', async ()=>{ await signOut(auth); });

// Messages
async function sendMessage(text, user){
  if(!text) return;
  await addDoc(collection(db, 'messages'), { text, uid: user.uid, username: user.displayName || 'Unknown', createdAt: serverTimestamp() });
}

messageForm.addEventListener('submit', async (e)=>{
  e.preventDefault();
  const text = messageInput.value.trim();
  const user = auth.currentUser;
  if(!user){ alert('Сначала войдите'); return; }
  await sendMessage(text, user);
  messageInput.value = '';
});

// Render
function renderMessage(docData, myUid){
  const div = document.createElement('div');
  div.className = 'message ' + (docData.uid === myUid ? 'me' : 'other');
  div.innerHTML = `<div><strong>${escapeHtml(docData.username)}</strong><div>${escapeHtml(docData.text)}</div></div>`;
  return div;
}

function escapeHtml(s){ return s ? s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;') : s; }

let unsubscribeMessages = null;

function startListeningMessages(uid){
  const q = query(collection(db,'messages'), orderBy('createdAt'));
  unsubscribeMessages = onSnapshot(q, (snap)=>{
    messagesDiv.innerHTML = '';
    snap.docs.forEach(d=>{
      const data = d.data();
      const el = renderMessage(data, uid);
      messagesDiv.appendChild(el);
    });
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
  });
}

// Auth state
onAuthStateChanged(auth, async (user)=>{
  if(user){
    displayNameSpan.textContent = user.displayName || user.email;
    userInfo.classList.remove('hidden');
    chatSection.classList.remove('hidden');
    auth.querySelector('#forms').classList.add('hidden');
    // ensure user doc exists
    try{
      const udoc = await getDoc(doc(db,'users',user.uid));
      if(!udoc.exists()){
        await setDoc(doc(db,'users',user.uid), { username: user.displayName || '', email: user.email, createdAt: serverTimestamp() });
      }
    }catch(e){ console.warn(e); }
    if(unsubscribeMessages) unsubscribeMessages();
    startListeningMessages(user.uid);
  }else{
    displayNameSpan.textContent = '';
    userInfo.classList.add('hidden');
    chatSection.classList.add('hidden');
    auth.querySelector('#forms').classList.remove('hidden');
    if(unsubscribeMessages) { unsubscribeMessages(); unsubscribeMessages = null; }
    messagesDiv.innerHTML = '';
  }
});

// helper export for admin page (optional)
export { firebaseConfig, app, auth, db };
