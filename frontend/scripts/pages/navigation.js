const menuToggle = document.getElementById("menu-toggle");
const mobileMenu = document.getElementById("mobile-menu");
const overlay = document.getElementById("overlay");

const menuView = document.getElementById("menu-view");
const notificationsView = document.getElementById("notifications-view");

const notificationsBtn = document.getElementById("notifications-btn");
const backToMenuBtn = document.getElementById("back-to-menu");

/* ---------------------------
   HAMBURGER MENU OPEN / CLOSE
---------------------------- */
if (menuToggle && mobileMenu && overlay) {
  menuToggle.addEventListener("click", () => {
    mobileMenu.classList.toggle("active");
    overlay.classList.toggle("active");

    // zawsze wracamy do menu view
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
  notificationsBtn.addEventListener("click", () => {
    showNotificationsView();
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
