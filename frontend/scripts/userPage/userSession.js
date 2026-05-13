document.addEventListener("DOMContentLoaded", () => {
  loadCurrentUser();
  setupLogout();
  setupDisabledMobileLinks();
});

async function loadCurrentUser() {
  const userNameElement = document.getElementById("user-name");

  try {
    const user = await apiRequest("/user/userdata", {
      method: "GET",
    });

    console.log("Loaded user:", user);

    if (userNameElement) {
      userNameElement.textContent =
        user.userName || user.username || user.name || user.email || "User";
    }
  } catch (error) {
    console.error("Could not load user:", error);

    if (userNameElement) {
      userNameElement.textContent = "User";
    }
  }
}

function closeMobileMenu() {
  document.getElementById("mobile-menu")?.classList.remove("active");
  document.getElementById("overlay")?.classList.remove("active");
}

function setupLogout() {
  document.querySelectorAll(".logout-btn").forEach((button) => {
    button.addEventListener("click", () => {
      clearStoredAuth();
      closeMobileMenu();
      window.location.href = "./login.html";
    });
  });
}

function setupDisabledMobileLinks() {
  document.querySelectorAll(".mobile-menu-disabled").forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
    });
  });
}
