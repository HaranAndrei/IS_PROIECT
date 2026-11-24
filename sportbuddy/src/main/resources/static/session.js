export const sessionKey = "sportbuddy.accessToken";

export function goToLogin(expired = false) {
    sessionStorage.removeItem(sessionKey);
    location.replace(expired ? "/?session=expired" : "/");
}

export async function request(path, {method = "GET", body, token} = {}) {
    const headers = {Accept: "application/json"};
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (token) headers.Authorization = "Bearer " + token;

    const response = await fetch(path, {method, headers, body: JSON.stringify(body)});
    if (!response.ok) {
        const error = new Error("Cererea a eșuat");
        error.status = response.status;
        throw error;
    }
    return response.json();
}
