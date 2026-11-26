import {goToLogin, request, sessionKey} from "./session.js";

const cityCenter = [46.7712, 23.5893];
const sportNames = {baschet: "Baschet", fotbal: "Fotbal", tenis: "Tenis"};
const sidebar = document.querySelector("#court-sidebar");
const directory = document.querySelector("#court-directory");
const tileStatus = document.querySelector("#tile-status");
const filters = document.querySelector("#sport-filters");
const courtList = document.querySelector("#court-list");
const courtStatus = document.querySelector("#court-status");
const courtCount = document.querySelector("#court-count");
const retry = document.querySelector("#retry-map");
const compact = matchMedia("(max-width: 640px)");

function sportLabels(court) {
    return court.sports.map(sport => sportNames[sport]).join(" · ");
}

function buildPopup(court, activeSport) {
    const popup = document.createElement("div");
    popup.className = "court-popup";
    const title = document.createElement("h2");
    const address = document.createElement("p");
    const sports = document.createElement("p");
    const details = document.createElement("p");
    const website = document.createElement("a");
    const createMatch = document.createElement("a");
    title.textContent = court.name;
    address.textContent = court.address;
    sports.textContent = sportLabels(court);
    details.textContent = court.details;
    website.href = court.website;
    website.textContent = "Informații despre teren";
    website.target = "_blank";
    website.rel = "noopener noreferrer";
    const sport = activeSport === "all" ? court.sports[0] : activeSport;
    createMatch.href = "/matches.html?" + new URLSearchParams({sport, court: court.name}) + "#match-form";
    createMatch.textContent = "Creează un meci aici";
    createMatch.className = "court-create-link";
    popup.append(title, address, sports, details);
    if (court.hoursSource) {
        const opening = document.createElement("a");
        opening.href = court.hoursSource;
        opening.target = "_blank";
        opening.rel = "noopener noreferrer";
        opening.textContent = "Program oficial";
        const links = document.createElement("p");
        links.className = "court-popup-links";
        links.append(website, opening);
        popup.append(links, createMatch);
    } else {
        const opening = document.createElement("p");
        opening.textContent = "Program de rezervare indisponibil.";
        popup.append(opening, website);
    }
    return popup;
}

function addCourt(leaflet, map, markers, icon, court, activeSport) {
    const marker = leaflet.marker([court.latitude, court.longitude], {
        icon, title: court.name, alt: court.name
    }).addTo(markers);
    const popup = leaflet.popup({autoPanPadding: [16, 16]}).setContent(buildPopup(court, activeSport));

    function keepPopupClearOfFilters() {
        popup.options.autoPanPaddingTopLeft = null;
        if (compact.matches) {
            directory.open = false;
            popup.options.autoPanPaddingTopLeft = [16, sidebar.offsetTop + sidebar.offsetHeight + 12];
        }
    }

    marker.on("click", keepPopupClearOfFilters);
    marker.bindPopup(popup);

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
        keepPopupClearOfFilters();
        map.setView(marker.getLatLng(), Math.max(14, map.getZoom()), {animate: false});
        marker.openPopup();
    });
    marker.on("popupopen", () => button.setAttribute("aria-expanded", "true"));
    marker.on("popupclose", () => button.setAttribute("aria-expanded", "false"));
    return item;
}

function loadScript(url) {
    return new Promise((resolve, reject) => {
        const script = document.createElement("script");
        script.src = url;
        script.onload = resolve;
        script.onerror = reject;
        document.head.append(script);
    });
}

async function addBasemap(leaflet, map) {
    const maplibre = "https://unpkg.com/maplibre-gl@5.24.0/dist/";
    const openStreetMap = '<a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';
    let vectorLayer;
    let vectorMap;
    let fallbackUsed = false;
    let rendered = false;
    const timeout = setTimeout(useRasterMap, 12000);

    function useRasterMap() {
        if (fallbackUsed || rendered) return;
        fallbackUsed = true;
        clearTimeout(timeout);
        if (vectorMap) map.removeLayer(vectorLayer);
        const tiles = leaflet.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            className: "map-raster",
            attribution: "&copy; " + openStreetMap
        });
        tiles.on("tileerror", () => {
            tileStatus.textContent = "Fundalul hărții nu s-a încărcat complet. Poți selecta terenurile din listă.";
            tileStatus.hidden = false;
        });
        tiles.addTo(map);
    }

    try {
        await loadScript(maplibre + "maplibre-gl.js");
        await loadScript("https://unpkg.com/@maplibre/maplibre-gl-leaflet@0.1.3/leaflet-maplibre-gl.js");
        if (fallbackUsed) return;
        const stylesheet = document.createElement("link");
        stylesheet.rel = "stylesheet";
        stylesheet.href = maplibre + "maplibre-gl.css";
        document.head.append(stylesheet);

        vectorLayer = leaflet.maplibreGL({
            style: "https://tiles.openfreemap.org/styles/fiord",
            attribution: '<a href="https://openfreemap.org/">OpenFreeMap</a> · '
                + '&copy; <a href="https://www.openmaptiles.org/">OpenMapTiles</a> · ' + openStreetMap
        });
        vectorLayer.addTo(map);
        vectorMap = vectorLayer.getMaplibreMap();
        vectorMap.once("load", () => {
            rendered = true;
            clearTimeout(timeout);
        });
        vectorMap.once("error", useRasterMap);
    } catch {
        useRasterMap();
    }
}

function displayMap(leaflet, courts) {
    filters.hidden = false;
    directory.open = !compact.matches;
    const map = leaflet.map("courts-map", {scrollWheelZoom: false, zoomControl: false, maxZoom: 19})
        .setView(cityCenter, 13);
    leaflet.control.zoom({position: "bottomright", zoomInTitle: "Apropie", zoomOutTitle: "Depărtează"}).addTo(map);
    const markers = leaflet.layerGroup().addTo(map);
    const icon = leaflet.divIcon({
        className: "court-pin", html: "", iconSize: [30, 30], iconAnchor: [15, 31], popupAnchor: [0, -30]
    });

    function applyFilter(sport) {
        const visible = sport === "all" ? courts : courts.filter(court => court.sports.includes(sport));
        for (const button of filters.querySelectorAll("button")) {
            button.setAttribute("aria-pressed", String(button.dataset.sport === sport));
        }
        markers.clearLayers();
        courtList.replaceChildren(...visible.map(court => addCourt(leaflet, map, markers, icon, court, sport)));
        courtCount.textContent = String(visible.length);
        courtStatus.textContent = visible.length === 0 ? "Nu sunt terenuri pentru acest sport." : "";
        if (visible.length > 0) {
            const sidebarSize = sidebar.getBoundingClientRect();
            map.fitBounds(visible.map(court => [court.latitude, court.longitude]), {
                paddingTopLeft: compact.matches ? [24, sidebarSize.height + 24] : [sidebarSize.width + 38, 28],
                paddingBottomRight: [32, 42], maxZoom: 14, animate: false
            });
        }
    }

    applyFilter("all");
    addBasemap(leaflet, map);
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
retry.addEventListener("click", () => location.reload());
loadMap();
