const TOKEN_KEY = "planix_token";
const REFRESH_TOKEN_KEY = "planix_refresh_token";
const USER_EMAIL_KEY = "planix_user_email";

function clearAuthStorage(storage) {
  storage.removeItem(TOKEN_KEY);
  storage.removeItem(REFRESH_TOKEN_KEY);
  storage.removeItem(USER_EMAIL_KEY);
}

function closeMobileMenu() {
  document.getElementById("mobile-menu")?.classList.remove("active");
  document.getElementById("overlay")?.classList.remove("active");
}

function logout() {
  clearAuthStorage(localStorage);
  clearAuthStorage(sessionStorage);
  closeMobileMenu();
  window.location.href = "./login.html";
}

document.querySelectorAll(".logout-btn").forEach((button) => {
  button.addEventListener("click", logout);
});

document.querySelectorAll(".mobile-menu-disabled").forEach((link) => {
  link.addEventListener("click", (event) => {
    event.preventDefault();
  });
});
