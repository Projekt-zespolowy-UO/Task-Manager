document.addEventListener("DOMContentLoaded", () => {
  const CATEGORIES_API_PATH = "/categories";
  const TASKS_API_PATH = "/tasks";

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

  // -------------------------
  // HELPERS
  // -------------------------

  function escapeHtml(value) {
    return String(value || "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function normalizeSearchValue(value) {
    return value.trim().toLowerCase();
  }

  function getPanelIdFromElement(panelElement) {
    return panelElement?.dataset.categoryId || panelElement?.dataset.panelId;
  }

  function getTaskIdFromElement(taskElement) {
    return taskElement?.dataset.taskId;
  }

  function applyTaskSearch() {
    const query = normalizeSearchValue(searchInput?.value || "");
    const panels = container.querySelectorAll(".task-panel");

    panels.forEach((panel) => {
      const tasks = panel.querySelectorAll(".task-item");
      let hasVisibleTask = query === "";

      tasks.forEach((task) => {
        const title =
          task.querySelector(".task-item-title")?.textContent.toLowerCase() ||
          "";
        const description = (task.dataset.description || "").toLowerCase();

        const matches =
          query === "" || title.includes(query) || description.includes(query);

        task.classList.toggle("task-item-hidden", !matches);

        if (matches) {
          hasVisibleTask = true;
        }
      });

      panel.classList.toggle("task-panel-hidden", !hasVisibleTask);
    });
  }

  // -------------------------
  // MODALS
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
  // TASK ELEMENT
  // -------------------------

  function createTaskElement(task) {
    const taskElement = document.createElement("div");
    taskElement.className = "task-item";

    const taskId = task.id;
    const taskName = task.title || task.name || "Task";
    const taskDescription = task.description || "";

    if (taskId !== undefined && taskId !== null) {
      taskElement.dataset.taskId = taskId;
    }

    taskElement.dataset.description = taskDescription;

    taskElement.innerHTML = `
      <div class="task-item-content">
        <span class="task-item-title">${escapeHtml(taskName)}</span>
        <div class="task-actions">
          <button class="task-options-btn" type="button">⋮</button>
          <div class="task-options-menu" style="display:none;">
            <button class="task-expand-btn" type="button">Rozwiń</button>
            <button class="task-move-btn" type="button">Przerzuć do kategorii</button>
            <button class="task-delete-btn" type="button">Usuń</button>
          </div>
        </div>
      </div>
    `;

    const taskOptionsBtn = taskElement.querySelector(".task-options-btn");
    const taskOptionsMenu = taskElement.querySelector(".task-options-menu");
    const taskDeleteBtn = taskElement.querySelector(".task-delete-btn");
    const taskExpandBtn = taskElement.querySelector(".task-expand-btn");
    const taskMoveBtn = taskElement.querySelector(".task-move-btn");

    taskOptionsBtn.addEventListener("click", (e) => {
      e.stopPropagation();

      document
        .querySelectorAll(".task-options-menu")
        .forEach((menu) => (menu.style.display = "none"));

      taskOptionsMenu.style.display =
        taskOptionsMenu.style.display === "block" ? "none" : "block";
    });

    taskDeleteBtn.addEventListener("click", async () => {
      // Frontend-only delete for now.
      // If backend adds DELETE /tasks/{id}, connect it here later.
      taskElement.remove();
      applyTaskSearch();
    });

    taskExpandBtn.addEventListener("click", () => {
      const title = taskElement.querySelector(".task-item-title").textContent;
      const description = taskElement.dataset.description;

      openTaskDetailsModal(title, description);
      taskOptionsMenu.style.display = "none";
    });

    taskMoveBtn.addEventListener("click", async () => {
      // Frontend-only move for now.
      // If backend adds PUT /tasks/{id}/category or PATCH /tasks/{id}, connect it here later.

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

      if (
        targetTasksContainer &&
        taskElement.parentElement !== targetTasksContainer
      ) {
        targetTasksContainer.appendChild(taskElement);
      }

      taskOptionsMenu.style.display = "none";
      applyTaskSearch();
    });

    return taskElement;
  }

  // -------------------------
  // PANEL ELEMENT
  // -------------------------

  function createPanelElement(panel) {
    const newPanel = document.createElement("div");
    newPanel.className = "task-panel";

    const panelId = panel.id;
    const panelName = panel.name || panel.title || "Panel";

    if (panelId !== undefined && panelId !== null) {
      newPanel.dataset.panelId = panelId;
      newPanel.dataset.categoryId = panelId;
    }

    newPanel.innerHTML = `
      <div class="panel-header">
        <button class="options-btn" type="button">⋮</button>
        <h3 class="panel-title">${escapeHtml(panelName)}</h3>
        <div class="options-menu" style="display:none;">
          <button class="rename-btn" type="button">Zmień nazwę</button>
          <button class="delete-btn" type="button">Usuń</button>
          <button class="add-task-btn" type="button">Dodaj zadanie</button>
        </div>
      </div>
      <div class="panel-tasks"></div>
    `;

    const optionsBtn = newPanel.querySelector(".options-btn");
    const optionsMenu = newPanel.querySelector(".options-menu");
    const renameBtn = newPanel.querySelector(".rename-btn");
    const deleteBtn = newPanel.querySelector(".delete-btn");
    const addTaskBtn = newPanel.querySelector(".add-task-btn");
    const tasksContainer = newPanel.querySelector(".panel-tasks");
    const panelTitle = newPanel.querySelector(".panel-title");

    optionsBtn.addEventListener("click", (e) => {
      e.stopPropagation();

      document
        .querySelectorAll(".options-menu")
        .forEach((menu) => (menu.style.display = "none"));

      optionsMenu.style.display =
        optionsMenu.style.display === "block" ? "none" : "block";
    });

    renameBtn.addEventListener("click", () => {
      // Frontend-only rename for now.
      // If backend adds PUT /categories/{id}, connect it here later.

      const currentName = panelTitle.textContent;
      const newName = prompt("Nowa nazwa panelu:", currentName);

      if (!newName || !newName.trim()) {
        return;
      }

      panelTitle.textContent = newName.trim();
      optionsMenu.style.display = "none";
      applyTaskSearch();
    });

    deleteBtn.addEventListener("click", async () => {
      // Frontend-only delete for now.
      // If backend adds DELETE /categories/{id}, connect it here later.

      newPanel.remove();
      applyTaskSearch();
    });

    addTaskBtn.addEventListener("click", () => {
      openTaskModal(tasksContainer, optionsMenu);
    });

    return newPanel;
  }

  // -------------------------
  // LOAD DATA FROM BACKEND
  // -------------------------

  async function loadPanelsAndTasks() {
    try {
      const panels = await apiRequest(CATEGORIES_API_PATH, {
        method: "GET",
      });

      console.log("Loaded panels:", panels);

      if (Array.isArray(panels)) {
        panels.forEach((panel) => {
          const panelElement = createPanelElement(panel);
          container.insertBefore(panelElement, addBtn);
        });
      }
    } catch (error) {
      console.warn("Could not load panels. Endpoint may not exist yet:", error);
    }

    try {
      const tasks = await apiRequest(TASKS_API_PATH, {
        method: "GET",
      });

      console.log("Loaded tasks:", tasks);

      if (Array.isArray(tasks)) {
        tasks.forEach((task) => {
          const categoryId =
            task.categoryId ||
            task.category?.id ||
            task.panelId ||
            task.panel?.id;

          if (!categoryId) {
            console.warn("Task has no categoryId, skipping:", task);
            return;
          }

          const panelElement = document.querySelector(
            `.task-panel[data-category-id="${categoryId}"]`,
          );

          if (!panelElement) {
            console.warn("No panel found for task:", task);
            return;
          }

          const tasksContainer = panelElement.querySelector(".panel-tasks");

          if (!tasksContainer) {
            return;
          }

          const taskElement = createTaskElement(task);
          tasksContainer.appendChild(taskElement);
        });
      }
    } catch (error) {
      console.warn("Could not load tasks. Endpoint may not exist yet:", error);
    }

    applyTaskSearch();
  }

  // -------------------------
  // EVENTS
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

  // Create panel in backend
  if (saveBtn) {
    saveBtn.addEventListener("click", async () => {
      const name = panelNameInput.value.trim();

      if (!name) {
        alert("Wpisz nazwę panelu!");
        return;
      }

      saveBtn.disabled = true;

      try {
        const savedPanel = await apiRequest(CATEGORIES_API_PATH, {
          method: "POST",
          body: JSON.stringify({
            name,
          }),
        });

        console.log("Saved panel:", savedPanel);

        const newPanel = createPanelElement(savedPanel);
        container.insertBefore(newPanel, addBtn);

        closeAllModals();
        applyTaskSearch();
      } catch (error) {
        console.error("Failed to save panel:", error);
        alert(
          "Nie udało się zapisać panelu w bazie. Sprawdź, czy backend ma endpoint POST /categories.",
        );
      } finally {
        saveBtn.disabled = false;
      }
    });
  }

  // Create task in backend
  if (taskSaveBtn) {
    taskSaveBtn.addEventListener("click", async () => {
      const taskName = taskNameInput.value.trim();
      const taskDescription = taskDescriptionInput.value.trim();

      if (!taskName || !activeTasksContainer) {
        return;
      }

      const panelElement = activeTasksContainer.closest(".task-panel");
      const categoryId = getPanelIdFromElement(panelElement);

      if (!categoryId) {
        alert("Nie znaleziono ID panelu. Panel musi być zapisany w bazie.");
        return;
      }

      taskSaveBtn.disabled = true;

      try {
        const savedTask = await apiRequest(TASKS_API_PATH, {
          method: "POST",
          body: JSON.stringify({
            title: taskName,
            description: taskDescription,
            categoryId: Number(categoryId),
          }),
        });

        console.log("Saved task:", savedTask);

        const taskElement = createTaskElement(savedTask);
        activeTasksContainer.appendChild(taskElement);

        if (activeOptionsMenu) {
          activeOptionsMenu.style.display = "none";
        }

        closeAllModals();
        applyTaskSearch();
      } catch (error) {
        console.error("Failed to save task:", error);
        alert(
          "Nie udało się zapisać zadania w bazie. Sprawdź, czy backend przyjmuje title, description i categoryId.",
        );
      } finally {
        taskSaveBtn.disabled = false;
      }
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

  loadPanelsAndTasks();
});
