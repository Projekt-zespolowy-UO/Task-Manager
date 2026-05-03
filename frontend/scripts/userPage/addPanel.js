document.addEventListener("DOMContentLoaded", () => {
  const addBtn = document.querySelector(".add-panel-btn");
  const panelModal = document.getElementById("panel-modal");
  const taskModal = document.getElementById("task-modal");
  const taskDetailsModal = document.getElementById("task-details-modal");
  const overlay = document.getElementById("overlay");

  const saveBtn = document.getElementById("save-btn");
  const cancelBtn = document.getElementById("cancel-btn");

  const taskSaveBtn = document.getElementById("task-save-btn");
  const taskCancelBtn = document.getElementById("task-cancel-btn");

  const taskDetailsCloseBtn = document.getElementById("task-details-close-btn");

  const container = document.querySelector(".task-container");
  const searchInput = document.querySelector(".search-input");
  const clearSearchButton = document.querySelector(".clear-search");

  const panelNameInput = document.getElementById("panel-name-input");
  const taskNameInput = document.getElementById("task-name-input");
  const taskDescriptionInput = document.getElementById(
    "task-description-input",
  );

  const taskDetailsTitle = document.getElementById("task-details-title");
  const taskDetailsDescription = document.getElementById(
    "task-details-description",
  );

  let activeTasksContainer = null;
  let activeOptionsMenu = null;
  let activeTaskElement = null;

  function normalizeSearchValue(value) {
    return value.trim().toLowerCase();
  }

  function applyTaskSearch() {
    const query = normalizeSearchValue(searchInput?.value || "");
    const panels = container.querySelectorAll(".task-panel");

    panels.forEach((panel) => {
      const tasks = panel.querySelectorAll(".task-item");
      let hasVisibleTask = query === "";

      tasks.forEach((task) => {
        const title =
          task.querySelector(".task-item-title")?.textContent.toLowerCase() || "";
        const description = (task.dataset.description || "").toLowerCase();
        const matches = query === "" || title.includes(query) || description.includes(query);

        task.classList.toggle("task-item-hidden", !matches);

        if (matches) {
          hasVisibleTask = true;
        }
      });

      panel.classList.toggle("task-panel-hidden", !hasVisibleTask);
    });
  }

  // -------------------------
  // MODALE
  // -------------------------

  const openPanelModal = () => {
    panelModal.style.display = "flex";
    overlay.classList.add("active");
    panelNameInput.value = "";
    panelNameInput.focus();
  };

  const closePanelModal = () => {
    panelModal.style.display = "none";
    panelNameInput.value = "";
  };

  const openTaskModal = (tasksContainer, optionsMenu) => {
    activeTasksContainer = tasksContainer;
    activeOptionsMenu = optionsMenu;

    taskModal.style.display = "flex";
    overlay.classList.add("active");
    taskNameInput.value = "";
    taskDescriptionInput.value = "";
    taskNameInput.focus();
  };

  const closeTaskModal = () => {
    taskModal.style.display = "none";
    taskNameInput.value = "";
    taskDescriptionInput.value = "";
    activeTasksContainer = null;
    activeOptionsMenu = null;
  };

  const openTaskDetailsModal = (title, description) => {
    taskDetailsTitle.textContent = title;
    taskDetailsDescription.textContent = description || "Brak opisu.";
    taskDetailsModal.style.display = "flex";
    overlay.classList.add("active");
  };

  const closeTaskDetailsModal = () => {
    taskDetailsModal.style.display = "none";
  };

  const closeAllModals = () => {
    closePanelModal();
    closeTaskModal();
    closeTaskDetailsModal();
    overlay.classList.remove("active");
  };

  // -------------------------
  // TASK
  // -------------------------

  function createTaskElement(taskName, taskDescription) {
    const task = document.createElement("div");
    task.className = "task-item";
    task.dataset.description = taskDescription || "";

    task.innerHTML = `
      <div class="task-item-content">
        <span class="task-item-title">${taskName}</span>
        <div class="task-actions">
          <button class="task-options-btn">⋮</button>
          <div class="task-options-menu" style="display:none;">
            <button class="task-expand-btn">Rozwiń</button>
            <button class="task-move-btn">Przerzuć do kategorii</button>
            <button class="task-delete-btn">Usuń</button>
          </div>
        </div>
      </div>
    `;

    const taskOptionsBtn = task.querySelector(".task-options-btn");
    const taskOptionsMenu = task.querySelector(".task-options-menu");
    const taskDeleteBtn = task.querySelector(".task-delete-btn");
    const taskExpandBtn = task.querySelector(".task-expand-btn");
    const taskMoveBtn = task.querySelector(".task-move-btn");

    taskOptionsBtn.addEventListener("click", (e) => {
      e.stopPropagation();

      document
        .querySelectorAll(".task-options-menu")
        .forEach((menu) => (menu.style.display = "none"));

      taskOptionsMenu.style.display =
        taskOptionsMenu.style.display === "block" ? "none" : "block";
    });

    taskDeleteBtn.addEventListener("click", () => {
      task.remove();
      applyTaskSearch();
    });

    taskExpandBtn.addEventListener("click", () => {
      const title = task.querySelector(".task-item-title").textContent;
      const description = task.dataset.description;
      openTaskDetailsModal(title, description);
      taskOptionsMenu.style.display = "none";
    });

    taskMoveBtn.addEventListener("click", () => {
      const panels = [...document.querySelectorAll(".task-panel")];
      const panelNames = panels.map((panel, index) => {
        const title =
          panel.querySelector(".panel-title")?.textContent ||
          `Panel ${index + 1}`;
        return `${index + 1}. ${title}`;
      });

      if (panels.length <= 1) {
        alert("Brak innej kategorii do przeniesienia.");
        return;
      }

      const choice = prompt(
        `Wybierz numer kategorii:\n${panelNames.join("\n")}`,
      );

      const selectedIndex = Number(choice) - 1;

      if (
        Number.isNaN(selectedIndex) ||
        selectedIndex < 0 ||
        selectedIndex >= panels.length
      ) {
        return;
      }

      const targetPanel = panels[selectedIndex];
      const targetTasksContainer = targetPanel.querySelector(".panel-tasks");

      if (targetTasksContainer && task.parentElement !== targetTasksContainer) {
        targetTasksContainer.appendChild(task);
      }

      taskOptionsMenu.style.display = "none";
      applyTaskSearch();
    });

    return task;
  }

  // -------------------------
  // PANEL
  // -------------------------

  function createPanelElement(name) {
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
      <div class="panel-tasks"></div>
    `;

    const optionsBtn = newPanel.querySelector(".options-btn");
    const optionsMenu = newPanel.querySelector(".options-menu");
    const deleteBtn = newPanel.querySelector(".delete-btn");
    const addTaskBtn = newPanel.querySelector(".add-task-btn");
    const tasksContainer = newPanel.querySelector(".panel-tasks");

    optionsBtn.addEventListener("click", (e) => {
      e.stopPropagation();

      document
        .querySelectorAll(".options-menu")
        .forEach((menu) => (menu.style.display = "none"));

      optionsMenu.style.display =
        optionsMenu.style.display === "block" ? "none" : "block";
    });

    deleteBtn.addEventListener("click", () => {
      newPanel.remove();
      applyTaskSearch();
    });

    addTaskBtn.addEventListener("click", () => {
      openTaskModal(tasksContainer, optionsMenu);
    });

    return newPanel;
  }

  // -------------------------
  // EVENTY
  // -------------------------

  if (addBtn) {
    addBtn.addEventListener("click", openPanelModal);
  }

  if (cancelBtn) {
    cancelBtn.addEventListener("click", closeAllModals);
  }

  if (taskCancelBtn) {
    taskCancelBtn.addEventListener("click", closeAllModals);
  }

  if (taskDetailsCloseBtn) {
    taskDetailsCloseBtn.addEventListener("click", closeAllModals);
  }

  if (overlay) {
    overlay.addEventListener("click", closeAllModals);
  }

  if (saveBtn) {
    saveBtn.addEventListener("click", () => {
      const name = panelNameInput.value.trim();

      if (!name) {
        alert("Wpisz nazwę panelu!");
        return;
      }

      const newPanel = createPanelElement(name);
      container.insertBefore(newPanel, addBtn);

      closeAllModals();
    });
  }

  if (taskSaveBtn) {
    taskSaveBtn.addEventListener("click", () => {
      const taskName = taskNameInput.value.trim();
      const taskDescription = taskDescriptionInput.value.trim();

      if (!taskName || !activeTasksContainer) {
        return;
      }

      const taskElement = createTaskElement(taskName, taskDescription);
      activeTasksContainer.appendChild(taskElement);

      if (activeOptionsMenu) {
        activeOptionsMenu.style.display = "none";
      }

      closeAllModals();
      applyTaskSearch();
    });
  }

  if (searchInput) {
    searchInput.addEventListener("input", applyTaskSearch);
  }

  if (clearSearchButton) {
    clearSearchButton.addEventListener("click", () => {
      if (!searchInput) {
        return;
      }

      searchInput.value = "";
      searchInput.focus();
      applyTaskSearch();
    });
  }

  document.addEventListener("click", () => {
    document
      .querySelectorAll(".options-menu")
      .forEach((menu) => (menu.style.display = "none"));

    document
      .querySelectorAll(".task-options-menu")
      .forEach((menu) => (menu.style.display = "none"));
  });

  if (panelNameInput) {
    panelNameInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        saveBtn.click();
      }
    });
  }

  if (taskNameInput) {
    taskNameInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        taskSaveBtn.click();
      }
    });
  }

  applyTaskSearch();
});
