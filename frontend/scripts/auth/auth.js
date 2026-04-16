const AUTH_API_BASE_URL = window.AUTH_API_BASE_URL || "http://localhost:8080";
const TOKEN_KEY = "planix_token";
const REFRESH_TOKEN_KEY = "planix_refresh_token";
const USER_EMAIL_KEY = "planix_user_email";

function setFeedback(element, message, type = "") {
  if (!element) {
    return;
  }

  element.textContent = message;
  element.classList.remove("is-error", "is-success", "is-loading");

  if (type) {
    element.classList.add(type);
  }
}

function setButtonState(button, isLoading, loadingText, defaultText) {
  if (!button) {
    return;
  }

  button.disabled = isLoading;
  button.textContent = isLoading ? loadingText : defaultText;
}

function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_EMAIL_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(USER_EMAIL_KEY);
}

function storeAuth(authDto, email, rememberMe = true) {
  const targetStorage = rememberMe ? localStorage : sessionStorage;

  clearStoredAuth();
  if (authDto?.token) {
    targetStorage.setItem(TOKEN_KEY, authDto.token);
  }

  if (authDto?.refreshToken) {
    targetStorage.setItem(REFRESH_TOKEN_KEY, authDto.refreshToken);
  }

  targetStorage.setItem(USER_EMAIL_KEY, email);
}

async function postJson(path, payload) {
  const response = await fetch(`${AUTH_API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

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

function normalizeEmail(value) {
  return value.trim().toLowerCase();
}

function redirectToHome() {
  window.location.href = "./userPage.html";
}

function setupLogin() {
  const form = document.getElementById("login-form");

  if (!form) {
    return;
  }

  const emailInput = document.getElementById("login-email");
  const passwordInput = document.getElementById("login-password");
  const rememberMeInput = document.getElementById("remember-me");
  const feedback = document.getElementById("login-feedback");
  const submitButton = document.getElementById("login-submit");

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const email = normalizeEmail(emailInput.value);
    const password = passwordInput.value;
    const rememberMe = rememberMeInput.checked;

    if (!email || !password) {
      setFeedback(feedback, "Wpisz adres e-mail i hasło.", "is-error");
      return;
    }

    setFeedback(feedback, "Trwa logowanie...", "is-loading");
    setButtonState(submitButton, true, "Logowanie...", "Zaloguj się");

    try {
      const authDto = await postJson("/auth/login", { email, password });
      storeAuth(authDto, email, rememberMe);
      setFeedback(feedback, "Logowanie zakończone sukcesem.", "is-success");
      window.setTimeout(redirectToHome, 500);
    } catch (error) {
      const message =
        error instanceof TypeError
          ? "Nie udało się połączyć z backendem. Sprawdź, czy serwer działa na localhost:8080."
          : error.message;

      setFeedback(feedback, message, "is-error");
    } finally {
      setButtonState(submitButton, false, "Logowanie...", "Zaloguj się");
    }
  });
}

function setupRegistration() {
  const form = document.getElementById("registration-form");

  if (!form) {
    return;
  }

  const userNameInput = document.getElementById("registration-name");
  const emailInput = document.getElementById("registration-email");
  const passwordInput = document.getElementById("registration-password");
  const confirmPasswordInput = document.getElementById(
    "registration-password-confirm",
  );
  const termsInput = document.getElementById("registration-terms");
  const feedback = document.getElementById("registration-feedback");
  const submitButton = document.getElementById("registration-submit");

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const userName = userNameInput.value.trim();
    const email = normalizeEmail(emailInput.value);
    const password = passwordInput.value;
    const confirmPassword = confirmPasswordInput.value;

    if (!userName || !email || !password || !confirmPassword) {
      setFeedback(feedback, "Uzupełnij wszystkie pola formularza.", "is-error");
      return;
    }

    if (password.length < 6) {
      setFeedback(
        feedback,
        "Hasło musi mieć co najmniej 6 znaków.",
        "is-error",
      );
      return;
    }

    if (password !== confirmPassword) {
      setFeedback(feedback, "Hasła muszą być identyczne.", "is-error");
      return;
    }

    if (!termsInput.checked) {
      setFeedback(
        feedback,
        "Zaakceptuj regulamin i politykę prywatności.",
        "is-error",
      );
      return;
    }

    setFeedback(feedback, "Tworzenie konta...", "is-loading");
    setButtonState(submitButton, true, "Tworzenie...", "Utwórz konto");

    try {
      const authDto = await postJson("/user/registration", {
        userName,
        email,
        password,
      });

      storeAuth(authDto, email, true);
      setFeedback(feedback, "Konto zostało utworzone.", "is-success");
      window.setTimeout(redirectToHome, 500);
    } catch (error) {
      const message =
        error instanceof TypeError
          ? "Nie udało się połączyć z backendem. Sprawdź, czy serwer działa na localhost:8080."
          : error.message;

      setFeedback(feedback, message, "is-error");
    } finally {
      setButtonState(submitButton, false, "Tworzenie...", "Utwórz konto");
    }
  });
}

setupLogin();
setupRegistration();
