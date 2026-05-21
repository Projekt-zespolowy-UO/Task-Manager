document.addEventListener("DOMContentLoaded", () => {
  const CATEGORIES_API_PATH = "/categories";
  const TASKS_API_PATH = "/tasks";
  const TASKS_EXPORT_API_PATH = "/tasks/export.csv";

  const DEFAULT_STATUS_PANELS = [
    { id: "BACKLOG", name: "Backlog" },
    { id: "IN_PROGRESS", name: "In progress" },
    { id: "DONE", name: "Done" },
  ];

  const addCategoryBtn = document.getElementById("add-category-btn");
  const exportCsvBtn = document.getElementById("export-csv-btn");
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
  const categoryTabs = document.getElementById("category-tabs");
  const searchInput = document.querySelector(".search-input");
  const clearSearchButton = document.querySelector(".clear-search");

  const panelNameInput = document.getElementById("panel-name-input");
  const taskNameInput = document.getElementById("task-name-input");
  const taskDescriptionInput = document.getElementById(
    "task-description-input"
  );
  const taskModalTitle = document.getElementById("task-modal-title");
  const taskPriorityInput = document.getElementById("task-priority-input");
  const taskDeadlineInput = document.getElementById("task-deadline-input");
  const taskCategoryInput = document.getElementById("task-category-input");

  const taskDetailsTitle = document.getElementById("task-details-title");
  const taskDetailsDescription = document.getElementById(
    "task-details-description"
  );

  let statusPanels = [...DEFAULT_STATUS_PANELS];
  let activeCategoryId = null;
  let activeStatusId = statusPanels[0].id;
  let openCategoryMenu = null;
  let activeTaskMenuStatusId = null;
  let activePanelMenuStatusId = null;
  let activeTaskMenuPosition = null;
  let activePanelMenuPosition = null;
  let editingTaskId = null;
  let taskDetailsOpen = null;
  let categories = [];
  let tasks = [];

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

  function normalizeStatus(value) {
    if (!statusPanels.length) {
      return "";
    }

    const normalizedValue = String(value || "")
      .trim()
      .toUpperCase();
    const aliases = {
      TODO: "BACKLOG",
      "TO DO": "BACKLOG",
      INPROGRESS: "IN_PROGRESS",
      "IN PROGRESS": "IN_PROGRESS",
    };
    const statusId = aliases[normalizedValue] || normalizedValue;

    return statusPanels.some((panel) => panel.id === statusId)
      ? statusId
      : statusPanels[0].id;
  }

  function toBackendStatus(statusId) {
    const normalizedStatus = normalizeStatus(statusId);

    if (normalizedStatus === "IN_PROGRESS" || normalizedStatus === "DONE") {
      return normalizedStatus;
    }

    return "TODO";
  }

  function createTaskPayload() {
    const payload = {
      title: taskNameInput.value.trim(),
      description: taskDescriptionInput.value.trim(),
      priority: taskPriorityInput.value,
      categoryId: Number(taskCategoryInput.value || activeCategoryId),
    };

    if (taskDeadlineInput.value) {
      payload.deadline = taskDeadlineInput.value;
    }

    return payload;
  }

  function createStatusId(name) {
    const baseId =
      String(name || "")
        .trim()
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "") || `STATUS_${Date.now()}`;

    let candidateId = baseId;
    let suffix = 2;

    while (statusPanels.some((panel) => panel.id === candidateId)) {
      candidateId = `${baseId}_${suffix}`;
      suffix += 1;
    }

    return candidateId;
  }

  function getTaskCategoryId(task) {
    return (
      task.categoryId || task.category?.id || task.panelId || task.panel?.id
    );
  }

  function getStatusName(statusId) {
    return (
      statusPanels.find((panel) => panel.id === normalizeStatus(statusId))
        ?.name || "Status"
    );
  }

  function getFloatingMenuPosition(triggerElement, menuWidth = 160) {
    const rect = triggerElement.getBoundingClientRect();
    const viewportPadding = 12;

    const left = Math.max(
      viewportPadding,
      Math.min(
        window.innerWidth - menuWidth - viewportPadding,
        rect.right - menuWidth
      )
    );
    const top = rect.bottom + 8;

    return {
      left,
      top,
    };
  }

  function applyTaskSearch() {
    const query = normalizeSearchValue(searchInput?.value || "");
    const panels = container.querySelectorAll(".task-panel");

    panels.forEach((panel) => {
      const panelTasks = panel.querySelectorAll(".task-item");
      let hasVisibleTask = query === "";

      panelTasks.forEach((taskElement) => {
        const title =
          taskElement
            .querySelector(".task-item-title")
            ?.textContent.toLowerCase() || "";
        const description = (
          taskElement.dataset.description || ""
        ).toLowerCase();
        const matches =
          query === "" || title.includes(query) || description.includes(query);

        taskElement.classList.toggle("task-item-hidden", !matches);

        if (matches) {
          hasVisibleTask = true;
        }
      });

      panel.classList.toggle("task-panel-hidden", !hasVisibleTask);
    });
  }

  function buildExportPath() {
    const params = new URLSearchParams();
    const searchValue = searchInput?.value?.trim();

    if (activeCategoryId) {
      params.set("categoryId", activeCategoryId);
    }

    if (searchValue) {
      params.set("search", searchValue);
    }

    const queryString = params.toString();
    return queryString
      ? `${TASKS_EXPORT_API_PATH}?${queryString}`
      : TASKS_EXPORT_API_PATH;
  }

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = filename || "tasks.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  function openCategoryModal() {
    panelModal.style.display = "flex";
    overlay.classList.add("active");
    panelNameInput.value = "";
    panelNameInput.focus();
  }

  function closeCategoryModal() {
    panelModal.style.display = "none";
    panelNameInput.value = "";
  }

  function openTaskModal(statusId) {
    activeStatusId = normalizeStatus(statusId);
    editingTaskId = null;
    taskModalTitle.textContent = "Dodaj nowe zadanie";
    taskSaveBtn.textContent = "Dodaj";
    renderTaskCategoryOptions(activeCategoryId);
    taskModal.style.display = "flex";
    overlay.classList.add("active");
    taskNameInput.value = "";
    taskDescriptionInput.value = "";
    taskPriorityInput.value = "MEDIUM";
    taskDeadlineInput.value = "";
    taskNameInput.focus();
  }

  function openTaskEditModal(task) {
    editingTaskId = Number(task.id);
    activeStatusId = normalizeStatus(task.status);
    taskModalTitle.textContent = "Edytuj zadanie";
    taskSaveBtn.textContent = "Zapisz";
    renderTaskCategoryOptions(getTaskCategoryId(task));
    taskModal.style.display = "flex";
    overlay.classList.add("active");
    taskNameInput.value = task.title || task.name || "";
    taskDescriptionInput.value = task.description || "";
    taskPriorityInput.value = task.priority || "MEDIUM";
    taskDeadlineInput.value = task.deadline || "";
    taskNameInput.focus();
  }

  function closeTaskModal() {
    taskModal.style.display = "none";
    taskNameInput.value = "";
    taskDescriptionInput.value = "";
    taskPriorityInput.value = "MEDIUM";
    taskDeadlineInput.value = "";
    taskCategoryInput.innerHTML = "";
    activeStatusId = statusPanels[0]?.id || "";
    editingTaskId = null;
    taskModalTitle.textContent = "Dodaj nowe zadanie";
    taskSaveBtn.textContent = "Dodaj";
  }

  function openTaskDetails(title, description) {
    taskDetailsOpen = { title, description };
    taskDetailsTitle.textContent = title;
    taskDetailsDescription.textContent = description || "Brak opisu.";
    taskDetailsModal.style.display = "flex";
    overlay.classList.add("active");
  }

  function closeTaskDetails() {
    taskDetailsOpen = null;
    taskDetailsModal.style.display = "none";
  }

  function closeAllModals() {
    closeCategoryModal();
    closeTaskModal();
    closeTaskDetails();
    overlay.classList.remove("active");
  }

  function renderCategoryTabs() {
    categoryTabs.innerHTML = categories
      .map((category) => {
        const isActive = Number(category.id) === Number(activeCategoryId);
        const isMenuOpen = Number(category.id) === Number(openCategoryMenu?.id);
        const menuStyle = isMenuOpen
          ? `display:block; left:${openCategoryMenu.left}px; top:${openCategoryMenu.top}px;`
          : "display:none;";

        return `
          <div class="category-tab-item" data-category-id="${category.id}">
            <div class="category-tab-shell">
              <button
                class="category-tab${isActive ? " active" : ""}"
                type="button"
                data-action="select-category"
              >
                ${escapeHtml(category.name || "Kategoria")}
              </button>
              <button
                class="category-options-btn"
                type="button"
                data-action="toggle-category-menu"
                aria-label="Ustawienia kategorii"
              >
                &vellip;
              </button>
            </div>
            <div
              class="category-options-menu"
              style="${menuStyle}"
            >
              <button type="button" data-action="rename-category">
                Zmien nazwe
              </button>
              <button type="button" data-action="delete-category">
                Usun
              </button>
            </div>
          </div>
        `;
      })
      .join("");
  }

  function renderTaskCategoryOptions(selectedCategoryId) {
    taskCategoryInput.innerHTML = categories
      .map(
        (category) => `
          <option
            value="${category.id}"
            ${Number(category.id) === Number(selectedCategoryId) ? "selected" : ""}
          >
            ${escapeHtml(category.name || "Kategoria")}
          </option>
        `
      )
      .join("");
  }

  function renderTaskElement(task) {
    const taskId = task.id;
    const taskName = task.title || task.name || "Task";
    const taskDescription = task.description || "";
    const statusId = normalizeStatus(task.status);
    const isMenuOpen = activeTaskMenuStatusId === `${statusId}:${taskId}`;
    const menuStyle =
      isMenuOpen && activeTaskMenuPosition
        ? `display:block; left:${activeTaskMenuPosition.left}px; top:${activeTaskMenuPosition.top}px;`
        : "display:none;";

    return `
      <div
        class="task-item"
        data-task-id="${taskId}"
        data-status-id="${statusId}"
        data-description="${escapeHtml(taskDescription)}"
      >
        <div class="task-item-content">
          <span class="task-item-title">${escapeHtml(taskName)}</span>
          <div class="task-actions">
            <button class="task-options-btn" type="button" data-action="toggle-task-menu">
              ...
            </button>
            <div
              class="task-options-menu"
              style="${menuStyle}"
            >
              <button class="task-expand-btn" type="button" data-action="expand-task">
                Rozwin
              </button>
              <button class="task-move-btn" type="button" data-action="move-task">
                Zmien status
              </button>
              <button class="task-edit-btn" type="button" data-action="edit-task">
                Edytuj
              </button>
              <button class="task-delete-btn" type="button" data-action="delete-task">
                Usun
              </button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  function renderPanelElement(panel) {
    const panelTasks = tasks.filter(
      (task) =>
        Number(getTaskCategoryId(task)) === Number(activeCategoryId) &&
        normalizeStatus(task.status) === panel.id
    );
    const isMenuOpen = activePanelMenuStatusId === panel.id;
    const menuStyle =
      isMenuOpen && activePanelMenuPosition
        ? `display:block; left:${activePanelMenuPosition.left}px; top:${activePanelMenuPosition.top}px;`
        : "display:none;";

    return `
      <div class="task-panel" data-status-id="${panel.id}">
        <div class="panel-header">
          <button class="options-btn" type="button" data-action="toggle-panel-menu">
            ...
          </button>
          <h3 class="panel-title">${escapeHtml(panel.name)}</h3>
          <div class="options-menu" style="${menuStyle}">
            <button class="rename-btn" type="button" data-action="rename-status">
              Zmien nazwe
            </button>
            <button class="delete-btn" type="button" data-action="delete-status">
              Usun
            </button>
            <button class="add-task-btn" type="button" data-action="add-task">
              Dodaj zadanie
            </button>
          </div>
        </div>
        <div class="panel-tasks">
          ${panelTasks.map((task) => renderTaskElement(task)).join("")}
        </div>
      </div>
    `;
  }

  function renderAddStatusButton() {
    return `
      <button class="add-status-btn" type="button" data-action="add-status">
        Dodaj status
      </button>
    `;
  }

  function renderDashboard() {
    renderCategoryTabs();

    if (!categories.length) {
      container.innerHTML = renderAddStatusButton();
      return;
    }

    if (!activeCategoryId) {
      activeCategoryId = Number(categories[0].id);
    }

    container.innerHTML =
      statusPanels.map((panel) => renderPanelElement(panel)).join("") +
      renderAddStatusButton();

    applyTaskSearch();
  }

  async function loadDashboardData() {
    try {
      const loadedCategories = await apiRequest(CATEGORIES_API_PATH, {
        method: "GET",
      });

      categories = Array.isArray(loadedCategories) ? loadedCategories : [];
      activeCategoryId = categories[0]?.id || null;
    } catch (error) {
      console.warn(
        "Could not load categories. Endpoint may not exist yet:",
        error
      );
    }

    try {
      const loadedTasks = await apiRequest(TASKS_API_PATH, {
        method: "GET",
      });

      tasks = Array.isArray(loadedTasks)
        ? loadedTasks.map((task) => ({
            ...task,
            status: normalizeStatus(task.status),
          }))
        : [];
    } catch (error) {
      console.warn("Could not load tasks. Endpoint may not exist yet:", error);
    }

    renderDashboard();
  }

  if (addCategoryBtn) {
    addCategoryBtn.addEventListener("click", openCategoryModal);
  }

  if (exportCsvBtn) {
    exportCsvBtn.addEventListener("click", async () => {
      exportCsvBtn.disabled = true;
      exportCsvBtn.textContent = "Exporting...";

      try {
        const { blob, filename } = await apiDownload(buildExportPath(), {
          method: "GET",
        });
        downloadBlob(blob, filename);
      } catch (error) {
        console.error("Failed to export tasks:", error);
        alert("Nie udalo sie wyeksportowac zadan.");
      } finally {
        exportCsvBtn.disabled = false;
        exportCsvBtn.textContent = "Export CSV";
      }
    });
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
    saveBtn.addEventListener("click", async () => {
      const name = panelNameInput.value.trim();

      if (!name) {
        alert("Wpisz nazwe kategorii!");
        return;
      }

      saveBtn.disabled = true;

      try {
        const savedCategory = await apiRequest(CATEGORIES_API_PATH, {
          method: "POST",
          body: JSON.stringify({ name }),
        });

        categories.push(savedCategory);
        activeCategoryId = Number(savedCategory.id);
        openCategoryMenu = null;
        closeAllModals();
        renderDashboard();
      } catch (error) {
        console.error("Failed to save category:", error);
        alert("Nie udalo sie zapisac kategorii w bazie.");
      } finally {
        saveBtn.disabled = false;
      }
    });
  }

  if (taskSaveBtn) {
    taskSaveBtn.addEventListener("click", async () => {
      const taskName = taskNameInput.value.trim();
      const taskDescription = taskDescriptionInput.value.trim();

      if (!taskName || !activeCategoryId) {
        return;
      }

      taskSaveBtn.disabled = true;

      try {
        const taskPayload = createTaskPayload();

        if (!taskPayload.title || !taskPayload.categoryId) {
          return;
        }

        if (editingTaskId) {
          const updatedTask = await apiRequest(`${TASKS_API_PATH}/${editingTaskId}`, {
            method: "PATCH",
            body: JSON.stringify({
              ...taskPayload,
              status: toBackendStatus(activeStatusId),
            }),
          });

          tasks = tasks.map((task) =>
            Number(task.id) === editingTaskId
              ? {
                  ...task,
                  ...updatedTask,
                  status: normalizeStatus(updatedTask.status),
                }
              : task
          );

          activeTaskMenuStatusId = null;
          closeAllModals();
          renderDashboard();
          return;
        }

        const savedTask = await apiRequest(TASKS_API_PATH, {
          method: "POST",
          body: JSON.stringify({
            ...taskPayload,
            status: toBackendStatus(activeStatusId),
          }),
        });

        tasks.push({
          ...savedTask,
          status: normalizeStatus(savedTask.status),
        });

        activePanelMenuStatusId = null;
        closeAllModals();
        renderDashboard();
      } catch (error) {
        console.error("Failed to save task:", error);
        alert("Nie udalo sie zapisac zadania w bazie.");
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

  categoryTabs.addEventListener("click", (event) => {
    const categoryItem = event.target.closest(".category-tab-item");

    if (!categoryItem) {
      return;
    }

    const categoryId = Number(categoryItem.dataset.categoryId);
    const action = event.target.dataset.action;

    if (action === "select-category") {
      activeCategoryId = categoryId;
      openCategoryMenu = null;
      renderDashboard();
      return;
    }

    if (action === "toggle-category-menu") {
      event.preventDefault();
      event.stopPropagation();
      openCategoryMenu =
        openCategoryMenu?.id === categoryId
          ? null
          : {
              id: categoryId,
              ...getFloatingMenuPosition(event.target),
            };
      renderCategoryTabs();
      return;
    }

    if (action === "rename-category") {
      const category = categories.find(
        (item) => Number(item.id) === categoryId
      );
      const newName = prompt("Nowa nazwa kategorii:", category?.name || "");

      if (!newName || !newName.trim()) {
        return;
      }

      apiRequest(`${CATEGORIES_API_PATH}/${categoryId}`, {
        method: "PUT",
        body: JSON.stringify({ name: newName.trim() }),
      })
        .then((updatedCategory) => {
          categories = categories.map((item) =>
            Number(item.id) === categoryId ? updatedCategory : item
          );
          openCategoryMenu = null;
          renderDashboard();
        })
        .catch((error) => {
          console.error("Failed to rename category:", error);
          alert("Nie udalo sie zmienic nazwy kategorii.");
        });
      return;
    }

    if (action === "delete-category") {
      const confirmed = confirm("Usunac kategorie razem z jej zadaniami?");

      if (!confirmed) {
        return;
      }

      apiRequest(`${CATEGORIES_API_PATH}/${categoryId}`, {
        method: "DELETE",
      })
        .then(() => {
          categories = categories.filter(
            (item) => Number(item.id) !== categoryId
          );
          tasks = tasks.filter(
            (task) => Number(getTaskCategoryId(task)) !== categoryId
          );

          if (Number(activeCategoryId) === categoryId) {
            activeCategoryId = categories[0]?.id || null;
          }

          openCategoryMenu = null;
          renderDashboard();
        })
        .catch((error) => {
          console.error("Failed to delete category:", error);
          alert("Nie udalo sie usunac kategorii.");
        });
    }
  });

  container.addEventListener("click", (event) => {
    const action = event.target.dataset.action;
    const panelElement = event.target.closest(".task-panel");
    const taskElement = event.target.closest(".task-item");

    if (action === "add-status") {
      const newStatusName = prompt("Nazwa nowego statusu:");

      if (!newStatusName || !newStatusName.trim()) {
        return;
      }

      const trimmedName = newStatusName.trim();
      statusPanels = [
        ...statusPanels,
        { id: createStatusId(trimmedName), name: trimmedName },
      ];
      renderDashboard();
      return;
    }

    if (!panelElement) {
      return;
    }

    const panelStatusId = panelElement.dataset.statusId;

    if (action === "toggle-panel-menu") {
      event.stopPropagation();
      const shouldClosePanelMenu = activePanelMenuStatusId === panelStatusId;
      activePanelMenuStatusId = shouldClosePanelMenu ? null : panelStatusId;
      activePanelMenuPosition = shouldClosePanelMenu
        ? null
        : getFloatingMenuPosition(event.target);
      activeTaskMenuStatusId = null;
      activeTaskMenuPosition = null;
      renderDashboard();
      return;
    }

    if (action === "rename-status") {
      const panel = statusPanels.find((item) => item.id === panelStatusId);
      const newName = prompt("Nowa nazwa statusu:", panel?.name || "");

      if (!newName || !newName.trim()) {
        return;
      }

      statusPanels = statusPanels.map((item) =>
        item.id === panelStatusId ? { ...item, name: newName.trim() } : item
      );
      activePanelMenuStatusId = null;
      renderDashboard();
      return;
    }

    if (action === "delete-status") {
      if (statusPanels.length <= 1) {
        alert("Musi zostac przynajmniej jeden status.");
        return;
      }

      const confirmed = confirm("Usunac status razem z jego zadaniami?");

      if (!confirmed) {
        return;
      }

      tasks = tasks.filter(
        (task) => normalizeStatus(task.status) !== panelStatusId
      );
      statusPanels = statusPanels.filter((item) => item.id !== panelStatusId);

      if (activeStatusId === panelStatusId) {
        activeStatusId = statusPanels[0]?.id || "";
      }

      activePanelMenuStatusId = null;
      renderDashboard();
      return;
    }

    if (action === "add-task") {
      activePanelMenuStatusId = null;
      openTaskModal(panelStatusId);
      return;
    }

    if (!taskElement) {
      return;
    }

    const taskId = Number(taskElement.dataset.taskId);
    const taskStatusId = taskElement.dataset.statusId;

    if (action === "toggle-task-menu") {
      event.stopPropagation();
      const taskMenuId = `${taskStatusId}:${taskId}`;
      const shouldCloseTaskMenu = activeTaskMenuStatusId === taskMenuId;
      activeTaskMenuStatusId = shouldCloseTaskMenu ? null : taskMenuId;
      activeTaskMenuPosition = shouldCloseTaskMenu
        ? null
        : getFloatingMenuPosition(event.target);
      activePanelMenuStatusId = null;
      activePanelMenuPosition = null;
      renderDashboard();
      return;
    }

    if (action === "expand-task") {
      const task = tasks.find((item) => Number(item.id) === taskId);
      activeTaskMenuStatusId = null;
      openTaskDetails(task?.title || "Task", task?.description || "");
      return;
    }

    if (action === "edit-task") {
      const task = tasks.find((item) => Number(item.id) === taskId);

      if (!task) {
        return;
      }

      activeTaskMenuStatusId = null;
      activeTaskMenuPosition = null;
      openTaskEditModal(task);
      return;
    }

    if (action === "delete-task") {
      const confirmed = confirm("Usunac zadanie?");

      if (!confirmed) {
        return;
      }

      apiRequest(`${TASKS_API_PATH}/${taskId}`, {
        method: "DELETE",
      })
        .then(() => {
          tasks = tasks.filter((item) => Number(item.id) !== taskId);
          activeTaskMenuStatusId = null;
          activeTaskMenuPosition = null;
          renderDashboard();
        })
        .catch((error) => {
          console.error("Failed to delete task:", error);
          alert("Nie udalo sie usunac zadania.");
        });
      return;
    }

    if (action === "move-task") {
      if (statusPanels.length <= 1) {
        alert("Brak innego statusu do przeniesienia.");
        return;
      }

      const currentStatus = normalizeStatus(taskStatusId);
      const availableStatuses = statusPanels.filter(
        (panel) => panel.id !== currentStatus
      );
      const selectedLabel = prompt(
        `Wybierz status:\n${availableStatuses
          .map((panel, index) => `${index + 1}. ${panel.name}`)
          .join("\n")}`
      );
      const selectedStatus = availableStatuses[Number(selectedLabel) - 1];

      if (!selectedStatus) {
        return;
      }

      apiRequest(`${TASKS_API_PATH}/${taskId}/status`, {
        method: "PATCH",
        body: JSON.stringify({
          status: toBackendStatus(selectedStatus.id),
        }),
      })
        .then((updatedTask) => {
          tasks = tasks.map((item) =>
            Number(item.id) === taskId
              ? {
                  ...item,
                  ...updatedTask,
                  status: normalizeStatus(updatedTask.status),
                }
              : item
          );

          activeTaskMenuStatusId = null;
          activeTaskMenuPosition = null;
          renderDashboard();
        })
        .catch((error) => {
          console.error("Failed to move task:", error);
          alert("Nie udalo sie zmienic statusu zadania.");
        });
    }
  });

  document.addEventListener("click", () => {
    if (
      openCategoryMenu !== null ||
      activePanelMenuStatusId !== null ||
      activeTaskMenuStatusId !== null
    ) {
      openCategoryMenu = null;
      activePanelMenuStatusId = null;
      activeTaskMenuStatusId = null;
      activePanelMenuPosition = null;
      activeTaskMenuPosition = null;
      renderDashboard();
    }
  });

  if (panelNameInput) {
    panelNameInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        saveBtn.click();
      }
    });
  }

  if (taskNameInput) {
    taskNameInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        taskSaveBtn.click();
      }
    });
  }

  loadDashboardData();
});
