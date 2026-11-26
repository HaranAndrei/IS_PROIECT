import {goToLogin, request, sessionKey} from "./session.js";

const message = document.querySelector("#matches-message");
const status = document.querySelector("#matches-status");
const retry = document.querySelector("#retry-matches");
const list = document.querySelector("#match-list");
const template = document.querySelector("#match-template");
const matchForm = document.querySelector("#match-form");
const sportSelect = document.querySelector("#match-sport");
const fieldSelect = document.querySelector("#match-field");
const fieldHelp = document.querySelector("#field-help");
const createButton = document.querySelector("#create-match");
const createStatus = document.querySelector("#create-status");
const actionStatus = document.querySelector("#match-action-status");
const dateTime = new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "long", timeStyle: "short", timeZone: "UTC"
});

let currentUser;
let sportsFields = [];
let fieldsLoadFailed = false;
let isCreatingMatch = false;

function setMinimumStartTime() {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    document.querySelector("#match-start").min = now.toISOString().slice(0, 16);
}

function showRequestError(target, error, fallback) {
    if (error.status === 401) {
        goToLogin(true);
        return;
    }

    if (error.status === 409) {
        target.textContent = "Nu s-a putut efectua acțiunea: meciul poate fi complet sau ești deja înscris.";
    } else if (error.status === 400) {
        target.textContent = "Verifică datele introduse și încearcă din nou.";
    } else {
        target.textContent = fallback;
    }
}

function updateFieldOptions() {
    const selectedSport = sportSelect.value.toLocaleLowerCase("ro-RO");
    const matchingFields = sportsFields.filter(field =>
        String(field.sport).toLocaleLowerCase("ro-RO") === selectedSport
    );

    fieldSelect.replaceChildren(new Option("Alege terenul", ""));
    for (const field of matchingFields) {
        const label = [field.name, field.address].filter(Boolean).join(" — ");
        fieldSelect.add(new Option(label, String(field.id)));
    }
    fieldSelect.disabled = matchingFields.length === 0;
    createButton.disabled = isCreatingMatch || matchingFields.length === 0;

    if (fieldsLoadFailed) {
        fieldHelp.textContent = "Lista terenurilor nu a putut fi încărcată. Reîncarcă pagina și încearcă din nou.";
    } else if (matchingFields.length === 0 && selectedSport) {
        fieldHelp.textContent = "Nu există terenuri înregistrate pentru acest sport.";
    } else {
        fieldHelp.textContent = "";
    }
}

function renderSports() {
    const knownSports = new Map([
        ["baschet", "Baschet"],
        ["fotbal", "Fotbal"],
        ["tenis", "Tenis"]
    ]);
    for (const field of sportsFields) {
        if (field.sport && !knownSports.has(String(field.sport).toLocaleLowerCase("ro-RO"))) {
            knownSports.set(String(field.sport).toLocaleLowerCase("ro-RO"), field.sport);
        }
    }

    sportSelect.replaceChildren(new Option("Alege sportul", ""));
    for (const [value, label] of knownSports) {
        sportSelect.add(new Option(label, value));
    }
    updateFieldOptions();
}

function renderMatches(matches, participantsByMatch) {
    const cards = document.createDocumentFragment();
    for (const match of matches) {
        const card = template.content.cloneNode(true);
        const item = card.querySelector(".match-card");
        const participants = participantsByMatch.get(String(match.id));
        const attendeeList = card.querySelector(".participant-list");
        const joinButton = card.querySelector(".match-join");
        const leaveButton = card.querySelector(".match-leave");
        const fullMessage = card.querySelector(".match-full");
        const isParticipant = Array.isArray(participants) && participants.some(participant =>
            String(participant.user?.id) === String(currentUser.id)
        );

        item.dataset.matchId = match.id;
        card.querySelector(".match-sport").textContent = match.sport;
        card.querySelector(".match-field").textContent = match.fieldName;

        // The API sends a wall-clock time without a time zone.
        const start = card.querySelector(".match-start");
        const clockTime = new Date(match.startTime + "Z");
        start.dateTime = match.startTime;
        start.textContent = Number.isNaN(clockTime.getTime())
            ? match.startTime.replace("T", " ")
            : dateTime.format(clockTime);

        card.querySelector(".match-spots").textContent = match.availableSpots;
        card.querySelector(".match-capacity").textContent = "din " + match.maxPlayers;
        card.querySelector(".match-availability").dataset.full = String(match.availableSpots === 0);
        card.querySelector(".participant-count").textContent = "(" + (participants?.length ?? "?") + ")";

        if (Array.isArray(participants) && participants.length > 0) {
            for (const participant of participants) {
                const entry = document.createElement("li");
                entry.textContent = participant.user?.name || participant.user?.email || "Jucător";
                attendeeList.append(entry);
            }
        } else {
            const empty = document.createElement("li");
            empty.className = "participant-empty";
            empty.textContent = Array.isArray(participants)
                ? "Niciun participant înscris încă."
                : "Lista participanților nu este disponibilă momentan.";
            attendeeList.append(empty);
        }

        joinButton.dataset.action = "join";
        leaveButton.dataset.action = "leave";
        joinButton.hidden = isParticipant || match.availableSpots === 0;
        leaveButton.hidden = !isParticipant;
        fullMessage.hidden = isParticipant || match.availableSpots !== 0;
        cards.append(card);
    }
    list.replaceChildren(cards);
}

async function loadMatches() {
    const token = sessionStorage.getItem(sessionKey);
    if (!token) {
        goToLogin();
        return;
    }

    message.hidden = false;
    status.textContent = "Se încarcă meciurile…";
    retry.hidden = true;
    let profileLoaded = false;
    try {
        currentUser = await request("/api/me", {token});
        profileLoaded = true;
        document.querySelector("#account-name").textContent = currentUser.name;
        document.querySelector("#account-email").textContent = currentUser.email;
        document.querySelector("#signed-in-details").hidden = false;

        const [matches, fields] = await Promise.all([
            request("/api/matches/overview", {token}),
            request("/api/fields", {token}).catch(() => null)
        ]);
        fieldsLoadFailed = fields === null;
        sportsFields = Array.isArray(fields) ? fields : [];
        renderSports();

        const participantEntries = await Promise.all(matches.map(async match => {
            try {
                const participants = await request(`/api/matches/${match.id}/participants`, {token});
                return [String(match.id), participants];
            } catch {
                return [String(match.id), null];
            }
        }));
        renderMatches(matches, new Map(participantEntries));
        list.hidden = matches.length === 0;
        status.textContent = matches.length === 0 ? "Nu sunt meciuri momentan." : "";
        message.hidden = matches.length > 0;
    } catch (error) {
        if (error.status === 401 || (!profileLoaded && error.status === 404)) {
            goToLogin(true);
        } else {
            status.textContent = "Nu am putut încărca meciurile.";
            retry.hidden = false;
        }
    }
}

async function leaveMatch(matchId, token) {
    const response = await fetch(`/api/matches/${matchId}/participants/me`, {
        method: "DELETE",
        headers: {Accept: "application/json", Authorization: "Bearer " + token}
    });
    if (!response.ok) {
        const error = new Error("Retragerea de la meci a eșuat");
        error.status = response.status;
        throw error;
    }
}

matchForm.addEventListener("submit", async event => {
    event.preventDefault();
    createStatus.textContent = "";
    if (!matchForm.reportValidity()) return;
    if (!fieldSelect.value) {
        createStatus.textContent = "Alege un teren disponibil pentru sportul selectat.";
        return;
    }

    const token = sessionStorage.getItem(sessionKey);
    if (!token) {
        goToLogin();
        return;
    }

    const formData = new FormData(matchForm);
    isCreatingMatch = true;
    createButton.disabled = true;
    try {
        await request("/api/matches", {
            method: "POST",
            token,
            body: {
                sport: formData.get("sport"),
                startTime: formData.get("startTime"),
                maxPlayers: Number(formData.get("maxPlayers")),
                field: {id: Number(formData.get("field"))}
            }
        });
        matchForm.reset();
        document.querySelector("#match-max-players").value = "10";
        updateFieldOptions();
        createStatus.textContent = "Meciul a fost creat.";
        await loadMatches();
    } catch (error) {
        showRequestError(createStatus, error, "Nu am putut crea meciul. Încearcă din nou.");
    } finally {
        isCreatingMatch = false;
        updateFieldOptions();
    }
});

sportSelect.addEventListener("change", updateFieldOptions);
list.addEventListener("click", async event => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    const matchId = button.closest(".match-card").dataset.matchId;
    const token = sessionStorage.getItem(sessionKey);
    if (!token) {
        goToLogin();
        return;
    }

    actionStatus.textContent = "";
    button.disabled = true;
    try {
        if (button.dataset.action === "join") {
            await request(`/api/matches/${matchId}/participants/me`, {method: "POST", token});
            actionStatus.textContent = "Te-ai înscris la meci.";
        } else {
            await leaveMatch(matchId, token);
            actionStatus.textContent = "Te-ai retras de la meci.";
        }
        await loadMatches();
    } catch (error) {
        showRequestError(actionStatus, error, "Nu am putut actualiza înscrierea. Încearcă din nou.");
    } finally {
        button.disabled = false;
    }
});

document.querySelector("#sign-out").addEventListener("click", () => goToLogin());
retry.addEventListener("click", loadMatches);
setMinimumStartTime();
loadMatches();
