document.addEventListener("DOMContentLoaded", () => {
  const addBtn = document.querySelector(".add-panel-btn");
  const modal = document.getElementById("panel-modal");
  const overlay = document.getElementById("overlay");
  const saveBtn = document.getElementById("save-btn");
  const cancelBtn = document.getElementById("cancel-btn");
  const container = document.querySelector(".task-container");

  // --- FUNKCJE MODALA ---

  // Otwieranie modala
  if (addBtn) {
    addBtn.addEventListener("click", () => {
      modal.style.display = "flex";
      overlay.classList.add("active");
    });
  }

  // Zamykanie modala
  const closeModal = () => {
    modal.style.display = "none";
    overlay.classList.remove("active");
    document.getElementById("panel-name-input").value = "";
  };

  if (cancelBtn) cancelBtn.addEventListener("click", closeModal);
  if (overlay) overlay.addEventListener("click", closeModal);

  // --- LOGIKA DODAWANIA PANELU ---

  if (saveBtn) {
    saveBtn.addEventListener("click", () => {
      const input = document.getElementById("panel-name-input");
      const name = input.value;

      if (name.trim() !== "") {
        const newPanel = document.createElement("div");
        newPanel.className = "task-panel";
        newPanel.innerHTML = `
          <div class="panel-header">
            <button class="options-btn">⋮</button>
            <h3 class="panel-title">${name}</h3>
            <div class="options-menu" style="display:none;">
              <button class="rename-btn">Zmień nazwę</button>
              <button class="delete-btn">Usuń</button>
              <button class="add-task-btn">Dodaj zadanie</button>
            </div>
          </div>
        `;

        const optionsBtn = newPanel.querySelector(".options-btn");
        const optionsMenu = newPanel.querySelector(".options-menu");

        optionsBtn.addEventListener("click", (e) => {
          e.stopPropagation();
          document
            .querySelectorAll(".options-menu")
            .forEach((m) => (m.style.display = "none"));
          optionsMenu.style.display =
            optionsMenu.style.display === "block" ? "none" : "block";
        });

        newPanel.querySelector(".delete-btn").addEventListener("click", () => {
          newPanel.remove();
        });

        container.insertBefore(newPanel, addBtn);

        closeModal();
      } else {
        alert("Wpisz nazwę panelu!");
      }
    });
  }

  document.addEventListener("click", () => {
    document
      .querySelectorAll(".options-menu")
      .forEach((m) => (m.style.display = "none"));
  });
});
