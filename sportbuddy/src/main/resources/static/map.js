import {goToLogin, request, sessionKey} from "./session.js";

const cityCenter = [46.7712, 23.5893];
const sportNames = {baschet: "Baschet", fotbal: "Fotbal", tenis: "Tenis"};
const mapPanel = document.querySelector("#map-panel");
const tileStatus = document.querySelector("#tile-status");
const filters = document.querySelector("#sport-filters");
const courtList = document.querySelector("#court-list");
const courtStatus = document.querySelector("#court-status");
const retry = document.querySelector("#retry-map");

function sportLabels(court) {
    return court.sports.map(sport => sportNames[sport]).join(" · ");
}

function buildPopup(court) {
    const popup = document.createElement("div");
    popup.className = "court-popup";
    const title = document.createElement("h2");
    const address = document.createElement("p");
    const sports = document.createElement("p");
    const details = document.createElement("p");
    const website = document.createElement("a");
    title.textContent = court.name;
    address.textContent = court.address;
    sports.textContent = sportLabels(court);
    details.textContent = court.details;
    website.href = court.website;
    website.textContent = "Informații despre teren";
    website.target = "_blank";
    website.rel = "noopener noreferrer";
    popup.append(title, address, sports, details, website);
    return popup;
}

function addCourt(leaflet, map, markers, icon, court) {
    const marker = leaflet.marker([court.latitude, court.longitude], {
        icon, title: court.name, alt: court.name
    }).bindPopup(buildPopup(court)).addTo(markers);

    const item = document.createElement("li");
    const button = document.createElement("button");
    const title = document.createElement("strong");
    const sports = document.createElement("span");
    button.type = "button";
    button.className = "court-item";
    button.setAttribute("aria-expanded", "false");
    title.textContent = court.name;
    sports.textContent = sportLabels(court);
    button.append(title, sports);
    item.append(button);

    button.addEventListener("click", () => {
        map.setView(marker.getLatLng(), Math.max(14, map.getZoom()), {animate: false});
        marker.openPopup();
        mapPanel.scrollIntoView({block: "nearest"});
    });
    marker.on("popupopen", () => button.setAttribute("aria-expanded", "true"));
    marker.on("popupclose", () => button.setAttribute("aria-expanded", "false"));
    return item;
}

function displayMap(leaflet, courts) {
    mapPanel.hidden = false;
    filters.hidden = false;
    const map = leaflet.map("courts-map", {scrollWheelZoom: false}).setView(cityCenter, 13);
    const markers = leaflet.layerGroup().addTo(map);
    const icon = leaflet.divIcon({
        className: "court-pin", html: "", iconSize: [26, 26], iconAnchor: [7, 26], popupAnchor: [6, -24]
    });

    function applyFilter(sport) {
        const visible = sport === "all" ? courts : courts.filter(court => court.sports.includes(sport));
        for (const button of filters.querySelectorAll("button")) {
            button.setAttribute("aria-pressed", String(button.dataset.sport === sport));
        }
        markers.clearLayers();
        courtList.replaceChildren(...visible.map(court => addCourt(leaflet, map, markers, icon, court)));
        courtStatus.textContent = visible.length === 0 ? "Nu sunt terenuri pentru acest sport."
            : visible.length === 1 ? "1 teren afișat" : visible.length + " terenuri afișate";
        if (visible.length > 0) {
            map.fitBounds(visible.map(court => [court.latitude, court.longitude]), {
                padding: [26, 36], maxZoom: 14, animate: false
            });
        }
    }

    applyFilter("all");
    const tiles = leaflet.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
    });
    tiles.on("loading", () => { tileStatus.hidden = true; });
    tiles.on("tileerror", () => {
        tileStatus.textContent = "O parte din fundalul OpenStreetMap nu s-a încărcat. Marcajele rămân selectabile.";
        tileStatus.hidden = false;
    });
    tiles.addTo(map);
    for (const button of filters.querySelectorAll("button")) {
        button.addEventListener("click", () => applyFilter(button.dataset.sport));
    }
}

async function loadMap() {
    const token = sessionStorage.getItem(sessionKey);
    if (!token) {
        goToLogin();
        return;
    }

    retry.hidden = true;
    courtStatus.textContent = "Se încarcă terenurile…";
    let profileLoaded = false;
    try {
        const profile = await request("/api/me", {token});
        profileLoaded = true;
        document.querySelector("#account-name").textContent = profile.name;
        document.querySelector("#account-email").textContent = profile.email;
        document.querySelector("#map-account").hidden = false;

        const courts = await request("/cluj-courts.json");
        if (!window.L) throw new Error("Leaflet nu este disponibil");
        displayMap(window.L, courts);
    } catch (error) {
        if (!profileLoaded && (error.status === 401 || error.status === 404)) {
            goToLogin(true);
        } else {
            courtStatus.textContent = "Nu am putut încărca harta terenurilor.";
            retry.hidden = false;
        }
    }
}

document.querySelector("#sign-out").addEventListener("click", () => goToLogin());
retry.addEventListener("click", loadMap);
loadMap();
