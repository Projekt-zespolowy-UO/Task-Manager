const menuToggle = document.getElementById("menu-toggle");
const mobileMenu = document.getElementById("mobile-menu");
const overlay = document.getElementById("overlay");

const menuView = document.getElementById("menu-view");
const notificationsView = document.getElementById("notifications-view");

const notificationsBtn = document.getElementById("notifications-btn");
const backToMenuBtn = document.getElementById("back-to-menu");

const REMINDERS_API = "/api/reminders";

let reminders = [];

/* ---------------------------
   LOAD REMINDERS
---------------------------- */

async function loadReminders() {
  try {
    reminders = await apiRequest(REMINDERS_API, {
      method: "GET",
    });

    renderReminders();
  } catch (err) {
    console.error("Failed to load reminders:", err);
  }
}

/* ---------------------------
   RENDER REMINDERS
---------------------------- */

function renderReminders() {
  const list = document.getElementById("notifications-list");

  if (!list) return;

  if (!reminders.length) {
    list.innerHTML = `
      <div class="notification-item">
        Brak powiadomien
      </div>
    `;
    return;
  }

  list.innerHTML = reminders
    .map(
      (reminder) => `
        <div class="notification-item">
          <div class="notification-title">
            ${reminder.title}
          </div>

          <div class="notification-description">
            ${reminder.description || ""}
          </div>

          <div class="notification-date">
            ${new Date(reminder.reminderTime).toLocaleString()}
          </div>
        </div>
      `,
    )
    .join("");
}

/* ---------------------------
   CREATE REMINDER
---------------------------- */

async function createReminder(title, description, reminderTime) {
  try {
    await apiRequest(REMINDERS_API, {
      method: "POST",
      body: JSON.stringify({
        title,
        description,
        reminderTime,
      }),
    });

    await loadReminders();
  } catch (err) {
    console.error("Failed to create reminder:", err);
  }
}

/* ---------------------------
   HAMBURGER MENU OPEN / CLOSE
---------------------------- */

if (menuToggle && mobileMenu && overlay) {
  menuToggle.addEventListener("click", () => {
    mobileMenu.classList.toggle("active");
    overlay.classList.toggle("active");

    showMenuView();
  });

  overlay.addEventListener("click", () => {
    mobileMenu.classList.remove("active");
    overlay.classList.remove("active");

    showMenuView();
  });
}

/* ---------------------------
   OPEN NOTIFICATIONS VIEW
---------------------------- */

if (notificationsBtn) {
  notificationsBtn.addEventListener("click", async () => {
    showNotificationsView();

    await loadReminders();
  });
}

/* ---------------------------
   BACK TO MENU VIEW
---------------------------- */

if (backToMenuBtn) {
  backToMenuBtn.addEventListener("click", () => {
    showMenuView();
  });
}

/* ---------------------------
   VIEW SWITCHERS
---------------------------- */

function showMenuView() {
  if (!menuView || !notificationsView) return;

  menuView.style.display = "flex";
  notificationsView.style.display = "none";
}

function showNotificationsView() {
  if (!menuView || !notificationsView) return;

  menuView.style.display = "none";
  notificationsView.style.display = "flex";
}

/* ---------------------------
   ACTIVE LINK HIGHLIGHT
---------------------------- */

const currentPage = window.location.pathname.split("/").pop();

const navLinks = document.querySelectorAll(".navbar-link");

navLinks.forEach((link) => {
  const linkPage = link.getAttribute("href");

  if (linkPage === currentPage) {
    link.classList.add("active");
  }
});
