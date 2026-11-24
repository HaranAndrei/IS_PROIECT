import {goToLogin, request, sessionKey} from "./session.js";

const message = document.querySelector("#matches-message");
const status = document.querySelector("#matches-status");
const retry = document.querySelector("#retry-matches");
const list = document.querySelector("#match-list");
const template = document.querySelector("#match-template");
const dateTime = new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "long", timeStyle: "short", timeZone: "UTC"
});

function renderMatches(matches) {
    const cards = document.createDocumentFragment();
    for (const match of matches) {
        const card = template.content.cloneNode(true);
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
        const profile = await request("/api/me", {token});
        profileLoaded = true;
        document.querySelector("#account-name").textContent = profile.name;
        document.querySelector("#account-email").textContent = profile.email;
        document.querySelector("#signed-in-details").hidden = false;

        const matches = await request("/api/matches/overview", {token});
        renderMatches(matches);
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

document.querySelector("#sign-out").addEventListener("click", () => goToLogin());
retry.addEventListener("click", loadMatches);
loadMatches();
