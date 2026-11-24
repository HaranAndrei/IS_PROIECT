import {request, sessionKey} from "./session.js";

const form = document.querySelector("#auth-form");
const title = document.querySelector("#auth-title");
const notice = document.querySelector("#auth-notice");
const nameField = document.querySelector("#name-field");
const nameInput = document.querySelector("#name");
const email = document.querySelector("#email");
const password = document.querySelector("#password");
const showPassword = document.querySelector("#toggle-password");
const loginMode = document.querySelector("#login-mode");
const registerMode = document.querySelector("#register-mode");
const submit = document.querySelector("#auth-submit");
let mode = "login";

function setMode(nextMode) {
    mode = nextMode;
    const registering = mode === "register";
    loginMode.setAttribute("aria-pressed", String(!registering));
    registerMode.setAttribute("aria-pressed", String(registering));
    nameField.hidden = !registering;
    nameInput.disabled = !registering;
    nameInput.required = registering;
    password.autocomplete = registering ? "new-password" : "current-password";
    password.value = "";
    setPasswordVisibility(false);
    title.textContent = registering ? "Creează un cont" : "Intră în cont";
    submit.textContent = registering ? "Creează contul" : "Autentifică-te";
    notice.textContent = "";
}

function setPasswordVisibility(visible) {
    password.type = visible ? "text" : "password";
    showPassword.textContent = visible ? "Ascunde" : "Afișează";
    showPassword.setAttribute("aria-label", visible ? "Ascunde parola" : "Afișează parola");
}

function setBusy(busy) {
    form.setAttribute("aria-busy", String(busy));
    loginMode.disabled = busy;
    registerMode.disabled = busy;
    submit.disabled = busy;
}

function errorMessage(status) {
    switch (status) {
        case 400: return "Verifică datele introduse și încearcă din nou.";
        case 401: return "Emailul sau parola este incorectă.";
        case 409: return "Există deja un cont cu acest email. Intră în cont sau folosește altă adresă.";
        default: return "Nu am putut finaliza cererea. Încearcă din nou.";
    }
}

loginMode.addEventListener("click", () => setMode("login"));
registerMode.addEventListener("click", () => setMode("register"));
showPassword.addEventListener("click", () => setPasswordVisibility(password.type === "password"));

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    notice.textContent = "";
    setBusy(true);
    let accountCreated = false;

    try {
        const credentials = {email: email.value.trim(), password: password.value};
        if (mode === "register") {
            await request("/api/users", {
                method: "POST",
                body: {name: nameInput.value.trim(), ...credentials}
            });
            accountCreated = true;
        }
        const login = await request("/api/auth/login", {method: "POST", body: credentials});
        sessionStorage.setItem(sessionKey, login.accessToken);
        location.replace("/matches.html");
    } catch (error) {
        if (accountCreated) {
            setMode("login");
            notice.textContent = "Contul a fost creat. Autentifică-te cu emailul și parola alese.";
        } else {
            notice.textContent = errorMessage(error.status);
        }
    } finally {
        setBusy(false);
    }
});

setPasswordVisibility(false);
if (sessionStorage.getItem(sessionKey)) {
    location.replace("/matches.html");
} else if (new URLSearchParams(location.search).get("session") === "expired") {
    notice.textContent = "Sesiunea nu mai este activă. Autentifică-te din nou.";
}
