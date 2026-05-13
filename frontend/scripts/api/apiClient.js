const API_BASE_URL = window.AUTH_API_BASE_URL || "http://localhost:8080";

const TOKEN_KEY = "planix_token";
const REFRESH_TOKEN_KEY = "planix_refresh_token";

function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
}

function getStoredRefreshToken() {
  return (
    localStorage.getItem(REFRESH_TOKEN_KEY) ||
    sessionStorage.getItem(REFRESH_TOKEN_KEY)
  );
}

function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem("planix_user_email");

  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem("planix_user_email");
}

function saveNewTokens(authDto) {
  const targetStorage = localStorage.getItem(TOKEN_KEY)
    ? localStorage
    : sessionStorage;

  if (authDto?.token) {
    targetStorage.setItem(TOKEN_KEY, authDto.token);
  }

  if (authDto?.refreshToken) {
    targetStorage.setItem(REFRESH_TOKEN_KEY, authDto.refreshToken);
  }
}

async function refreshAccessToken() {
  const refreshToken = getStoredRefreshToken();

  if (!refreshToken) {
    throw new Error("No refresh token");
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      refreshToken,
    }),
  });

  if (!response.ok) {
    throw new Error("Refresh token failed");
  }

  const authDto = await response.json();
  saveNewTokens(authDto);

  return authDto.token;
}

async function apiRequest(path, options = {}) {
  let token = getStoredToken();

  if (!token) {
    clearStoredAuth();
    window.location.href = "./login.html";
    throw new Error("No token");
  }

  let response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    },
  });

  if (response.status === 401 || response.status === 403) {
    try {
      token = await refreshAccessToken();

      response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
          ...(options.headers || {}),
        },
      });
    } catch (error) {
      clearStoredAuth();
      window.location.href = "./login.html";
      throw error;
    }
  }

  const raw = await response.text();

  let data = null;

  try {
    data = raw ? JSON.parse(raw) : null;
  } catch {
    data = raw;
  }

  if (!response.ok) {
    const message =
      typeof data === "object" && data?.message
        ? data.message
        : typeof data === "string" && data
          ? data
          : `Request failed with status ${response.status}`;

    throw new Error(message);
  }

  return data;
}
