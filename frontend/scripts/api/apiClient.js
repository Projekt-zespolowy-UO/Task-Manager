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
    console.error("❌ apiRequest: No token found, redirecting to login");
    clearStoredAuth();
    window.location.href = "./login.html";
    throw new Error("No token");
  }

  console.log("🔵 apiRequest: Token found, sending request to:", path);

  let response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    },
  });

  console.log("🔵 apiRequest: First response status:", response.status);

  if (response.status === 401 || response.status === 403) {
    console.log("🟡 apiRequest: Got 401/403, attempting token refresh...");
    try {
      token = await refreshAccessToken();
      console.log("🟢 apiRequest: Token refreshed successfully");

      response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
          ...(options.headers || {}),
        },
      });

      console.log(
        "🔵 apiRequest: Second response status after refresh:",
        response.status,
      );

      // Check if refresh+retry also failed with auth error
      if (response.status === 401 || response.status === 403) {
        console.log(
          "🔴 apiRequest: Still 401/403 after refresh! Session actually expired.",
        );
        clearStoredAuth();
        window.location.href = "./login.html";
        throw new Error("Session expired. Please login again.");
      }
    } catch (error) {
      console.error("🔴 apiRequest: Error during refresh/retry:", error);
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

    console.error("🔴 apiRequest: Response not OK:", message);
    throw new Error(message);
  }

  console.log("🟢 apiRequest: Success!");
  return data;
}

async function apiDownload(path, options = {}) {
  let token = getStoredToken();

  if (!token) {
    console.error("❌ apiDownload: No token found");
    clearStoredAuth();
    window.location.href = "./login.html";
    throw new Error("No token");
  }

  console.log(
    "🔵 apiDownload: First attempt with token:",
    token.substring(0, 10) + "...",
  );

  let response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "text/csv",
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    },
  });

  console.log("🔵 apiDownload: First attempt status:", response.status);

  // If we get 401/403, try to refresh token and retry ONCE
  if (response.status === 401 || response.status === 403) {
    console.log("🟡 apiDownload: Got 401/403, trying to refresh token...");
    try {
      token = await refreshAccessToken();
      console.log(
        "🟢 apiDownload: Token refreshed, retrying with new token...",
      );

      response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
          Accept: "text/csv",
          Authorization: `Bearer ${token}`,
          ...(options.headers || {}),
        },
      });

      console.log("🔵 apiDownload: Second attempt status:", response.status);
    } catch (refreshError) {
      console.error(
        "🔴 apiDownload: Token refresh failed:",
        refreshError.message,
      );
      clearStoredAuth();
      throw new Error(`Authentication failed: ${refreshError.message}`);
    }
  }

  if (!response.ok) {
    const errorText = await response.text();
    console.error(
      "🔴 apiDownload: Response not OK. Status:",
      response.status,
      "Body:",
      errorText,
    );
    throw new Error(
      `Download failed with status ${response.status}: ${errorText || "Unknown error"}`,
    );
  }

  console.log("🟢 apiDownload: Success! Preparing blob...");
  return {
    blob: await response.blob(),
    filename: getDownloadFilename(response.headers.get("Content-Disposition")),
  };
}

function getDownloadFilename(contentDisposition) {
  if (!contentDisposition) {
    return null;
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match) {
    return decodeURIComponent(utf8Match[1].replaceAll('"', ""));
  }

  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return filenameMatch ? filenameMatch[1] : null;
}
