import {goToLogin, request, sessionKey} from "./session.js";

const message = document.querySelector("#matches-message");
const status = document.querySelector("#matches-status");
const retry = document.querySelector("#retry-matches");
const list = document.querySelector("#match-list");
const template = document.querySelector("#match-template");
const matchForm = document.querySelector("#match-form");
const formFields = document.querySelector("#match-form-fields");
const formHeading = document.querySelector("#create-match-heading");
const sportSelect = document.querySelector("#match-sport");
const fieldOptions = document.querySelector("#match-field-options");
const fieldHelp = document.querySelector("#field-help");
const dateInput = document.querySelector("#match-date");
const durationSelect = document.querySelector("#match-duration");
const maxPlayersInput = document.querySelector("#match-max-players");
const slotOptions = document.querySelector("#match-slot-options");
const slotHelp = document.querySelector("#slot-help");
const cancelEdit = document.querySelector("#cancel-match-edit");
const createButton = document.querySelector("#create-match");
const createStatus = document.querySelector("#create-status");
const actionStatus = document.querySelector("#match-action-status");
const deleteDialog = document.querySelector("#match-delete-dialog");
const deleteForm = document.querySelector("#delete-match-form");
const deleteStatus = document.querySelector("#delete-match-status");
const cancelDelete = document.querySelector("#cancel-match-delete");
const confirmDelete = document.querySelector("#confirm-match-delete");
const dateTime = new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "long", timeStyle: "short", timeZone: "UTC"
});
const time = new Intl.DateTimeFormat("ro-RO", {timeStyle: "short", timeZone: "UTC"});
const bookingDate = new Intl.DateTimeFormat("sv-SE", {dateStyle: "short", timeZone: "Europe/Bucharest"});
const bookingDateTime = new Intl.DateTimeFormat("sv-SE", {
    dateStyle: "short", timeStyle: "medium", timeZone: "Europe/Bucharest"
});
const browseAvailable = new URLSearchParams(location.search).get("view") === "available";

let currentUser;
let sportsFields = [];
let fieldsLoaded = false;
let fieldsLoadFailed = false;
let matchesById = new Map();
let editingMatch = null;
let deletingMatchId = null;
let activeSlotRequest = 0;
let canKeepSavedTime = false;

function showRequestError(target, error, fallback, conflict) {
    if (error.status === 401) {
        goToLogin(true);
        return;
    }

    if (error.status === 409 && conflict) {
        target.textContent = conflict;
    } else if (error.status === 400) {
        target.textContent = "Verifică datele introduse și încearcă din nou.";
    } else {
        target.textContent = fallback;
    }
}

function updateSaveButton() {
    createButton.disabled = !canKeepSavedTime && !slotOptions.querySelector("input:checked");
}

function normalizedFieldName(name) {
    return name.normalize("NFD").replace(/\p{M}+/gu, "").trim().replace(/\s+/g, " ").toLocaleLowerCase("ro-RO");
}

function updateFieldOptions({name = "", id = null} = {}) {
    const selectedSport = sportSelect.value.toLocaleLowerCase("ro-RO");
    const matchingFields = sportsFields.filter(field =>
        String(field.sport).toLocaleLowerCase("ro-RO") === selectedSport
    ).sort((left, right) => left.name.localeCompare(right.name, "ro"));
    const normalizedName = normalizedFieldName(name);
    const preferredField = matchingFields.find(field =>
        id === null ? normalizedFieldName(field.name) === normalizedName : field.id === id
    );
    const choices = matchingFields.map(field => {
        const label = document.createElement("label");
        label.className = "field-option";
        const radio = document.createElement("input");
        radio.type = "radio";
        radio.name = "field";
        radio.value = String(field.id);
        radio.required = true;
        radio.checked = field === preferredField;

        const text = document.createElement("span");
        const name = document.createElement("strong");
        const address = document.createElement("span");
        name.textContent = field.name;
        address.textContent = field.address;
        text.append(name, address);
        label.append(radio, text);
        return label;
    });
    fieldOptions.replaceChildren(...choices);
    if (fieldsLoadFailed) {
        fieldHelp.textContent = "Nu am putut încărca terenurile. Reîncarcă pagina și încearcă din nou.";
    } else if (!selectedSport) {
        fieldHelp.textContent = "Alege sportul ca să vezi terenurile.";
    } else if (matchingFields.length === 0) {
        fieldHelp.textContent = "Nu există terenuri înregistrate pentru acest sport.";
    } else {
        fieldHelp.textContent = "";
    }
}

function savedStart(fieldId, date, duration) {
    if (editingMatch && Number(fieldId) === editingMatch.fieldId
            && date === editingMatch.startTime.slice(0, 10) && Number(duration) === editingMatch.durationHours) {
        return editingMatch.startTime;
    }
    return null;
}

async function loadSlots() {
    const requestId = ++activeSlotRequest;
    const fieldId = fieldOptions.querySelector("input:checked")?.value;
    const date = dateInput.value;
    const durationHours = durationSelect.value;
    const currentTime = slotOptions.querySelector("input:checked")?.value;
    const savedTime = savedStart(fieldId, date, durationHours)?.slice(11, 16);
    slotOptions.replaceChildren();
    canKeepSavedTime = false;
    updateSaveButton();
    if (!fieldId || !date || !dateInput.validity.valid) {
        slotHelp.textContent = "Alege terenul și data pentru a vedea orele libere.";
        return;
    }

    slotHelp.textContent = "Se încarcă orele libere…";
    const params = new URLSearchParams({fieldId, date, durationHours});
    if (editingMatch) params.set("matchId", editingMatch.id);

    try {
        const token = sessionStorage.getItem(sessionKey);
        const slots = await request("/api/matches/available-slots?" + params, {token});
        if (requestId !== activeSlotRequest) return;
        const hours = slots.map(slot => slot.slice(0, 5));
        canKeepSavedTime = Boolean(savedTime);
        const showSavedTime = savedTime && !hours.includes(savedTime);
        const selectedTime = hours.includes(currentTime) ? currentTime : savedTime;

        for (const hour of hours) {
            const label = document.createElement("label");
            label.className = "slot-option";
            const radio = document.createElement("input");
            radio.name = "hour";
            radio.type = "radio";
            radio.value = hour;
            radio.required = !canKeepSavedTime;
            radio.checked = hour === selectedTime;
            label.append(radio, hour);
            slotOptions.append(label);
        }
        slotHelp.textContent = showSavedTime
            ? `Ora salvată (${savedTime}) se păstrează dacă lași necompletată ora de începere.`
            : hours.length === 0 ? "Nu sunt ore libere în programul terenului pentru data și durata alese." : "";
        updateSaveButton();
    } catch (error) {
        if (requestId !== activeSlotRequest) return;
        showRequestError(slotHelp, error, "Nu am putut încărca orele. Schimbă data sau durata și încearcă din nou.");
    }
}

function renderSports(selectedSport = "", preferredField) {
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
    sportSelect.value = knownSports.has(selectedSport) ? selectedSport : "";
    updateFieldOptions(preferredField);
    loadSlots();
}

async function loadSportsFields(token) {
    if (fieldsLoaded) return;

    try {
        const fields = await request("/api/fields", {token});
        if (!Array.isArray(fields)) throw new Error("Lista terenurilor este invalidă");
        sportsFields = fields;
        fieldsLoadFailed = false;
        const selection = new URLSearchParams(location.search);
        renderSports(selection.get("sport") ?? "", {name: selection.get("court") ?? ""});
        fieldsLoaded = true;
    } catch {
        fieldsLoadFailed = true;
        renderSports();
    }
}

function renderMatches(matches, participantsByMatch) {
    matchesById = new Map(matches.map(match => [String(match.id), match]));
    const cards = document.createDocumentFragment();
    const now = bookingDateTime.format(new Date()).replace(" ", "T");
    for (const match of matches) {
        const card = template.content.cloneNode(true);
        const item = card.querySelector(".match-card");
        const participants = participantsByMatch.get(String(match.id));
        const attendeeList = card.querySelector(".participant-list");
        const joinButton = card.querySelector(".match-join");
        const leaveButton = card.querySelector(".match-leave");
        const fullMessage = card.querySelector(".match-full");
        const upcoming = match.startTime > now;
        const isOrganizer = String(match.organizerId) === String(currentUser.id);
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
        if (!Number.isNaN(clockTime.getTime())) {
            const endTime = new Date(clockTime.getTime() + match.durationHours * 3600000);
            card.querySelector(".match-end").textContent = " – " + time.format(endTime);
        }

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

        joinButton.hidden = !upcoming || isParticipant || match.availableSpots === 0;
        leaveButton.hidden = !isParticipant;
        fullMessage.hidden = !upcoming || isParticipant || match.availableSpots !== 0;
        card.querySelector(".match-edit").hidden = browseAvailable || !isOrganizer;
        card.querySelector(".match-delete").hidden = browseAvailable || !isOrganizer;
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

        const [matches] = await Promise.all([
            request(browseAvailable ? "/api/matches/available" : "/api/matches/overview", {token}),
            browseAvailable ? null : loadSportsFields(token)
        ]);

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
        status.textContent = matches.length === 0
            ? (browseAvailable ? "Nu sunt meciuri cu înscrieri deschise momentan." : "Nu sunt meciuri momentan.")
            : "";
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

function resetMatchForm() {
    editingMatch = null;
    matchForm.reset();
    dateInput.min = bookingDate.format(new Date());
    maxPlayersInput.min = "2";
    formHeading.textContent = "Creează un meci";
    createButton.textContent = "Creează meciul";
    cancelEdit.hidden = true;
    updateFieldOptions();
    loadSlots();
}

function editMatch(match) {
    editingMatch = match;
    formHeading.textContent = "Modifică meciul";
    createButton.textContent = "Salvează modificările";
    cancelEdit.hidden = false;
    createStatus.textContent = "";
    dateInput.value = match.startTime.slice(0, 10);
    const today = bookingDate.format(new Date());
    dateInput.min = dateInput.value < today ? dateInput.value : today;
    durationSelect.value = String(match.durationHours);
    maxPlayersInput.min = String(Math.min(2, match.maxPlayers));
    maxPlayersInput.value = match.maxPlayers;
    slotOptions.replaceChildren();
    renderSports(match.sport.toLocaleLowerCase("ro-RO"), {id: match.fieldId});
    formHeading.focus();
}

matchForm.addEventListener("submit", async event => {
    event.preventDefault();
    createStatus.textContent = "";
    if (formFields.disabled || !matchForm.reportValidity()) return;
    const formData = new FormData(matchForm);
    if (!formData.get("field") || (!formData.get("hour") && !canKeepSavedTime)) {
        createStatus.textContent = "Alege terenul și un interval disponibil.";
        return;
    }

    const token = sessionStorage.getItem(sessionKey);
    if (!token) {
        goToLogin();
        return;
    }

    const fieldId = Number(formData.get("field"));
    const date = formData.get("date");
    const durationHours = Number(formData.get("durationHours"));
    const hour = formData.get("hour");
    const saved = savedStart(fieldId, date, durationHours);
    const match = editingMatch;
    const body = {
        sport: formData.get("sport"),
        startTime: saved && (!hour || saved.slice(11, 16) === hour) ? saved : `${date}T${hour}:00`,
        maxPlayers: Number(formData.get("maxPlayers")),
        durationHours
    };

    formFields.disabled = true;
    try {
        await request(match ? `/api/matches/${match.id}` : "/api/matches", {
            method: match ? "PUT" : "POST",
            token,
            body: match ? {...body, fieldId, playersNeeded: match.playersNeeded} : {...body, field: {id: fieldId}}
        });
        resetMatchForm();
        createStatus.textContent = match ? "Modificările au fost salvate." : "Meciul a fost creat. Ești deja înscris.";
        await loadMatches();
    } catch (error) {
        showRequestError(createStatus, error, "Nu am putut salva meciul. Încearcă din nou.",
            match ? "Intervalul a fost ocupat sau capacitatea e sub numărul de înscriși."
                : "Intervalul a fost ocupat între timp. Alege altă oră.");
        if (error.status === 409) await loadSlots();
    } finally {
        formFields.disabled = false;
    }
});

sportSelect.addEventListener("change", () => {
    updateFieldOptions();
    loadSlots();
});
fieldOptions.addEventListener("change", loadSlots);
dateInput.addEventListener("change", loadSlots);
durationSelect.addEventListener("change", loadSlots);
slotOptions.addEventListener("change", updateSaveButton);
cancelEdit.addEventListener("click", () => {
    resetMatchForm();
    createStatus.textContent = "";
});

list.addEventListener("click", async event => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    const matchId = button.closest(".match-card").dataset.matchId;
    const action = button.dataset.action;
    if (action === "edit" || action === "delete") {
        const match = matchesById.get(matchId);
        if (formFields.disabled || String(match?.organizerId) !== String(currentUser.id)) return;
        if (action === "edit") {
            editMatch(match);
        } else {
            deletingMatchId = matchId;
            deleteStatus.textContent = "";
            deleteDialog.showModal();
        }
        return;
    }

    const token = sessionStorage.getItem(sessionKey);
    if (!token) {
        goToLogin();
        return;
    }

    actionStatus.textContent = "";
    button.disabled = true;
    try {
        if (action === "join") {
            await request(`/api/matches/${matchId}/participants/me`, {method: "POST", token});
            actionStatus.textContent = "Te-ai înscris la meci.";
        } else {
            await request(`/api/matches/${matchId}/participants/me`, {method: "DELETE", token});
            actionStatus.textContent = "Te-ai retras de la meci.";
        }
        await loadMatches();
    } catch (error) {
        showRequestError(actionStatus, error, "Nu am putut actualiza înscrierea. Încearcă din nou.",
            "Meciul a început, s-a completat sau ești deja înscris.");
        if (browseAvailable && error.status === 409) await loadMatches();
    } finally {
        button.disabled = false;
    }
});

cancelDelete.addEventListener("click", () => deleteDialog.close());
deleteDialog.addEventListener("cancel", event => {
    if (confirmDelete.disabled) event.preventDefault();
});
deleteForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (confirmDelete.disabled) return;
    const matchId = deletingMatchId;
    const token = sessionStorage.getItem(sessionKey);
    confirmDelete.disabled = true;
    cancelDelete.disabled = true;
    deleteStatus.textContent = "";
    try {
        await request(`/api/matches/${matchId}`, {method: "DELETE", token});
        deleteDialog.close();
        if (String(editingMatch?.id) === matchId) {
            resetMatchForm();
            createStatus.textContent = "";
        } else {
            loadSlots();
        }
        actionStatus.textContent = "Meciul a fost șters.";
        await loadMatches();
    } catch (error) {
        showRequestError(deleteStatus, error, "Nu am putut șterge meciul. Încearcă din nou.");
    } finally {
        confirmDelete.disabled = false;
        cancelDelete.disabled = false;
    }
});

document.querySelector("#sign-out").addEventListener("click", () => goToLogin());
retry.addEventListener("click", loadMatches);
if (browseAvailable) {
    document.title = "SportBuddy | Meciuri disponibile";
    document.querySelector("#matches-heading").textContent = "Meciuri disponibile";
    document.querySelector("#matches-feed-heading").textContent = "Înscrieri deschise";
    document.querySelector("#all-matches-tab").removeAttribute("aria-current");
    document.querySelector("#available-matches-tab").setAttribute("aria-current", "page");
    document.querySelector(".matches-workspace").classList.add("matches-workspace--available");
    document.querySelector(".match-create-section").hidden = true;
}
dateInput.min = bookingDate.format(new Date());
loadMatches();
