document.addEventListener("DOMContentLoaded", () => {
  const DASHBOARDS_API_PATH = "/api/dashboards";
  const CATEGORIES_API_PATH = "/api/categories";
  const TASKS_API_PATH = "/api/tasks";
  const TASKS_EXPORT_API_PATH = "/api/tasks/export.csv";
  const STATUS_API_PATH = "/api/statuses";
  let draggedTaskId = null;

  const GANTT_ZOOM_LEVELS = [
    { id: "compact", name: "Kompakt", dayWidth: 18, scaleStep: 14 },
    { id: "week", name: "Tydzien", dayWidth: 34, scaleStep: 7 },
    { id: "day", name: "Dzien", dayWidth: 64, scaleStep: 1 },
  ];

  const addCategoryBtn = document.getElementById("add-category-btn");
  const exportCsvBtn = document.getElementById("export-csv-btn");
  const addDashboardBtn = document.getElementById("add-dashboard-btn");
  const dashboardSelect = document.getElementById("dashboard-select");
  const dashboardModal = document.getElementById("dashboard-modal");
  const panelModal = document.getElementById("panel-modal");
  const taskModal = document.getElementById("task-modal");
  const taskDetailsModal = document.getElementById("task-details-modal");
  const overlay = document.getElementById("overlay");

  const saveBtn = document.getElementById("save-btn");
  const cancelBtn = document.getElementById("cancel-btn");
  const dashboardSaveBtn = document.getElementById("dashboard-save-btn");
  const dashboardCancelBtn = document.getElementById("dashboard-cancel-btn");
  const taskSaveBtn = document.getElementById("task-save-btn");
  const taskCancelBtn = document.getElementById("task-cancel-btn");
  const taskDetailsCloseBtn = document.getElementById("task-details-close-btn");

  const container = document.querySelector(".task-container");
  const categoryTabs = document.getElementById("category-tabs");
  const searchInput = document.querySelector(".search-input");
  const clearSearchButton = document.querySelector(".clear-search");
  const viewSelect = document.getElementById("view-select");

  const panelNameInput = document.getElementById("panel-name-input");
  const dashboardNameInput = document.getElementById("dashboard-name-input");
  const taskNameInput = document.getElementById("task-name-input");
  const taskDescriptionInput = document.getElementById(
    "task-description-input",
  );
  const taskModalTitle = document.getElementById("task-modal-title");
  const taskPriorityInput = document.getElementById("task-priority-input");
  const taskDeadlineInput = document.getElementById("task-deadline-input");
  const taskCategoryInput = document.getElementById("task-category-input");

  const taskDetailsTitle = document.getElementById("task-details-title");
  const taskDetailsDescription = document.getElementById(
    "task-details-description",
  );

  let statusPanels = [];
  let activeCategoryId = null;
  let activeDashboardId = null;
  let activeStatusId = statusPanels[0]?.id || "";
  let openCategoryMenu = null;
  let activeTaskMenuStatusId = null;
  let activePanelMenuStatusId = null;
  let activeTaskMenuPosition = null;
  let activePanelMenuPosition = null;
  let editingTaskId = null;
  let taskDetailsOpen = null;
  let activeView = viewSelect?.value || "board";
  let ganttZoomIndex = 1;
  let ganttScrollTarget = null;
  let dashboards = [];
  let categories = [];
  let tasks = [];
  const ACTIVE_DASHBOARD_KEY = "planix_active_dashboard_id";

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

  function createTaskPayload() {
    if (!activeStatusId || activeStatusId === "") {
      return null;
    }

    const payload = {
      title: taskNameInput.value.trim(),
      description: taskDescriptionInput.value.trim(),
      priority: taskPriorityInput.value,
      categoryId: Number(taskCategoryInput.value || activeCategoryId),
      dashboardId: activeDashboardId,
      statusId: Number(activeStatusId),
    };

    if (taskDeadlineInput.value) {
      payload.deadline = taskDeadlineInput.value;
    }

    return payload;
  }

  function getTaskCategoryId(task) {
    return (
      task.categoryId || task.category?.id || task.panelId || task.panel?.id
    );
  }

  function getStatusName(statusId) {
    return (
      statusPanels.find((panel) => panel.id === String(statusId))?.name ||
      "Status"
    );
  }

  function formatDate(value) {
    if (!value) {
      return "Brak terminu";
    }

    const date = new Date(`${value}T00:00:00`);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleDateString("pl-PL");
  }

  function formatDateObject(date) {
    if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
      return "";
    }

    return date.toLocaleDateString("pl-PL");
  }

  function addDays(date, days) {
    const nextDate = new Date(date);
    nextDate.setDate(nextDate.getDate() + days);
    return nextDate;
  }

  function getDaysBetween(startDate, endDate) {
    const dayMs = 24 * 60 * 60 * 1000;
    return Math.round((endDate - startDate) / dayMs);
  }

  function getGanttZoom() {
    return GANTT_ZOOM_LEVELS[ganttZoomIndex] || GANTT_ZOOM_LEVELS[1];
  }

  function getFilteredTasks() {
    const query = normalizeSearchValue(searchInput?.value || "");

    return tasks.filter((task) => {
      const inActiveCategory =
        !activeCategoryId ||
        Number(getTaskCategoryId(task)) === Number(activeCategoryId);
      const title = String(task.title || task.name || "").toLowerCase();
      const description = String(task.description || "").toLowerCase();
      const matchesSearch =
        query === "" || title.includes(query) || description.includes(query);

      return inActiveCategory && matchesSearch;
    });
  }

  function getPriorityClass(priority) {
    return `priority-${String(priority || "medium").toLowerCase()}`;
  }

  function getFloatingMenuPosition(triggerElement, menuWidth = 160) {
    const rect = triggerElement.getBoundingClientRect();
    const viewportPadding = 12;

    const left = Math.max(
      viewportPadding,
      Math.min(
        window.innerWidth - menuWidth - viewportPadding,
        rect.right - menuWidth,
      ),
    );
    const top = rect.bottom + 8;

    return {
      left,
      top,
    };
  }

  function applyTaskSearch() {
    if (activeView === "gantt") {
      renderDashboard();
      return;
    }

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

    if (activeDashboardId) {
      params.set("dashboardId", activeDashboardId);
    }

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

  function openDashboardModal() {
    dashboardModal.style.display = "flex";
    overlay.classList.add("active");
    dashboardNameInput.value = "";
    dashboardNameInput.focus();
  }

  function closeDashboardModal() {
    dashboardModal.style.display = "none";
    dashboardNameInput.value = "";
  }

  function closeCategoryModal() {
    panelModal.style.display = "none";
    panelNameInput.value = "";
  }

  function openTaskModal(statusId) {
    activeStatusId = String(statusId || "");
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
    activeStatusId = String(task.statusId || "");
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
    activeStatusId = "";
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
    closeDashboardModal();
    closeCategoryModal();
    closeTaskModal();
    closeTaskDetails();
    overlay.classList.remove("active");
  }

  function renderDashboardOptions() {
    dashboardSelect.innerHTML = dashboards
      .map(
        (dashboard) => `
          <option
            value="${dashboard.id}"
            ${Number(dashboard.id) === Number(activeDashboardId) ? "selected" : ""}
          >
            ${escapeHtml(dashboard.name || "Workspace")}
          </option>
        `,
      )
      .join("");

    dashboardSelect.disabled = dashboards.length === 0;
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
    taskCategoryInput.innerHTML = `
      <option value="">Brak kategorii</option>
      ${categories
        .map(
          (category) => `
            <option
              value="${category.id}"
              ${Number(category.id) === Number(selectedCategoryId) ? "selected" : ""}
            >
              ${escapeHtml(category.name || "Kategoria")}
            </option>
          `,
        )
        .join("")}
    `;
  }

  function renderTaskElement(task) {
    const taskId = task.id;
    const taskName = task.title || task.name || "Task";
    const taskDescription = task.description || "";
    const statusId = String(task.statusId || "");
    const isMenuOpen = activeTaskMenuStatusId === `${statusId}:${taskId}`;
    const menuStyle =
      isMenuOpen && activeTaskMenuPosition
        ? `display:block; left:${activeTaskMenuPosition.left}px; top:${activeTaskMenuPosition.top}px;`
        : "display:none;";

    return `
      <div
        class="task-item"
        draggable="true"
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
        String(task.statusId) === String(panel.id),
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

  function renderGanttView() {
    const visibleTasks = getFilteredTasks();
    const datedTasks = visibleTasks.filter((task) => task.deadline);
    const undatedTasks = visibleTasks.filter((task) => !task.deadline);
    const dayMs = 24 * 60 * 60 * 1000;
    const zoom = getGanttZoom();
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const taskDates = datedTasks
      .map((task) => new Date(`${task.deadline}T00:00:00`))
      .filter((date) => !Number.isNaN(date.getTime()));
    const minDate = taskDates.length
      ? new Date(
          Math.min(today.getTime(), ...taskDates.map((date) => date.getTime())),
        )
      : today;
    const maxDate = taskDates.length
      ? new Date(
          Math.max(today.getTime(), ...taskDates.map((date) => date.getTime())),
        )
      : new Date(today.getTime() + 6 * dayMs);
    const totalDays = Math.max(1, Math.round((maxDate - minDate) / dayMs) + 1);
    const timelineWidth = Math.max(560, totalDays * zoom.dayWidth);
    const gridStyle = `style="--gantt-timeline-width:${timelineWidth}px; --gantt-day-width:${zoom.dayWidth}px;"`;
    const scaleMarkers = [];
    const todayOffset = getDaysBetween(minDate, today);

    for (let offset = 0; offset < totalDays; offset += zoom.scaleStep) {
      scaleMarkers.push({
        date: addDays(minDate, offset),
        left: offset * zoom.dayWidth,
      });
    }

    container.className = "task-container gantt-view";

    if (!visibleTasks.length) {
      container.innerHTML = `
        <div class="gantt-wrapper">
          <div class="empty-state">Brak zadan do wyswietlenia.</div>
        </div>
      `;
      return;
    }

    container.innerHTML = `
      <div class="gantt-wrapper">
        <div class="gantt-toolbar">
          <div class="gantt-summary">
            ${visibleTasks.length} zadan - ${escapeHtml(formatDateObject(minDate))} - ${escapeHtml(formatDateObject(maxDate))}
          </div>
          <div class="gantt-controls">
            <button type="button" data-action="gantt-zoom-out">-</button>
            <span>${escapeHtml(zoom.name)}</span>
            <button type="button" data-action="gantt-zoom-in">+</button>
            <button type="button" data-action="gantt-today">Dzisiaj</button>
            <button type="button" data-action="gantt-fit">Dopasuj</button>
          </div>
        </div>
        <div class="gantt-scroll">
          <div class="gantt-header" ${gridStyle}>
            <div class="gantt-sticky-cell gantt-header-cell">Zadanie</div>
            <div class="gantt-scale">
              ${scaleMarkers
                .map(
                  (marker) => `
                    <span style="left:${marker.left}px;">
                      ${escapeHtml(formatDateObject(marker.date))}
                    </span>
                  `,
                )
                .join("")}
              ${
                todayOffset >= 0 && todayOffset < totalDays
                  ? `<span class="gantt-today-line" style="left:${todayOffset * zoom.dayWidth}px;"></span>`
                  : ""
              }
            </div>
            <div class="gantt-header-cell gantt-actions-cell">Akcje</div>
          </div>
          ${datedTasks
            .map((task) => {
              const statusId = String(task.statusId || "");
              const deadline = new Date(`${task.deadline}T00:00:00`);
              const endOffset = Math.max(
                0,
                Math.round((deadline - minDate) / dayMs),
              );
              const barLeft = endOffset * zoom.dayWidth;
              const barWidth = Math.max(18, Math.min(zoom.dayWidth, 54));

              return `
                <div
                  class="gantt-row gantt-task-row"
                  ${gridStyle}
                  data-task-id="${task.id}"
                  data-status-id="${statusId}"
                  data-description="${escapeHtml(task.description || "")}"
                >
                  <div class="gantt-task-info gantt-sticky-cell">
                    <span class="gantt-task-title">${escapeHtml(task.title || task.name || "Task")}</span>
                    <span class="gantt-task-meta">${escapeHtml(getStatusName(statusId))} - ${escapeHtml(task.priority || "MEDIUM")}</span>
                  </div>
                  <div class="gantt-timeline">
                    ${
                      todayOffset >= 0 && todayOffset < totalDays
                        ? `<span class="gantt-today-line" style="left:${todayOffset * zoom.dayWidth}px;"></span>`
                        : ""
                    }
                    <span
                      class="gantt-bar ${getPriorityClass(task.priority)}"
                      style="left:${barLeft}px; width:${barWidth}px;"
                      title="${escapeHtml(formatDate(task.deadline))}"
                    ></span>
                  </div>
                  <div class="gantt-row-actions gantt-actions-cell">
                    <button type="button" data-action="expand-task">Rozwin</button>
                    <button type="button" data-action="move-task">Status</button>
                    <button type="button" data-action="edit-task">Edytuj</button>
                    <button type="button" data-action="delete-task">Usun</button>
                  </div>
                </div>
              `;
            })
            .join("")}
          ${undatedTasks
            .map((task) => {
              const statusId = String(task.statusId || "");

              return `
                <div
                  class="gantt-row gantt-task-row"
                  ${gridStyle}
                  data-task-id="${task.id}"
                  data-status-id="${statusId}"
                  data-description="${escapeHtml(task.description || "")}"
                >
                  <div class="gantt-task-info gantt-sticky-cell">
                    <span class="gantt-task-title">${escapeHtml(task.title || task.name || "Task")}</span>
                    <span class="gantt-task-meta">${escapeHtml(getStatusName(statusId))} - ${escapeHtml(task.priority || "MEDIUM")}</span>
                  </div>
                  <div class="gantt-timeline">
                    <span class="gantt-task-meta">Brak terminu</span>
                  </div>
                  <div class="gantt-row-actions gantt-actions-cell">
                    <button type="button" data-action="expand-task">Rozwin</button>
                    <button type="button" data-action="move-task">Status</button>
                    <button type="button" data-action="edit-task">Edytuj</button>
                    <button type="button" data-action="delete-task">Usun</button>
                  </div>
                </div>
              `;
            })
            .join("")}
        </div>
      </div>
    `;

    const ganttScroll = container.querySelector(".gantt-scroll");

    if (ganttScroll && ganttScrollTarget === "today") {
      ganttScroll.scrollLeft = Math.max(
        0,
        todayOffset * zoom.dayWidth - ganttScroll.clientWidth / 2,
      );
    }

    ganttScrollTarget = null;
  }

  function renderDashboard() {
    renderDashboardOptions();
    console.log("🎨 renderDashboard() called");
    console.log("📊 categories:", categories);
    console.log("📊 statusPanels:", statusPanels);
    console.log("📊 activeDashboardId:", activeDashboardId);

    renderCategoryTabs();

    // Pokazuj statusy zawsze, niezależnie od kategorii
    if (!statusPanels.length) {
      console.warn("⚠️ No status panels! Showing only add-status button");
      container.innerHTML = renderAddStatusButton();
      return;
    }

    if (activeView === "gantt") {
      renderGanttView();
      return;
    }

    container.className = "task-container";
    console.log("✅ Rendering", statusPanels.length, "status panels");
    container.innerHTML =
      statusPanels.map((panel) => renderPanelElement(panel)).join("") +
      renderAddStatusButton();

    applyTaskSearch();
  }

  async function loadDashboardData() {
    try {
      const loadedDashboards = await apiRequest(DASHBOARDS_API_PATH, {
        method: "GET",
      });

      dashboards = Array.isArray(loadedDashboards) ? loadedDashboards : [];

      // Jeśli nie ma dashboardów, automatycznie utwórz jeden
      if (dashboards.length === 0) {
        console.warn("⚠️ No dashboards found, creating default dashboard...");
        try {
          const newDashboard = await apiRequest(DASHBOARDS_API_PATH, {
            method: "POST",
            body: JSON.stringify({ name: "Mój Dashboard" }),
          });
          dashboards = [newDashboard];
          console.log("✅ Default dashboard created:", newDashboard);
        } catch (createError) {
          console.error("❌ Failed to create default dashboard:", createError);
        }
      }

      const storedDashboardId = Number(
        localStorage.getItem(ACTIVE_DASHBOARD_KEY),
      );
      const storedDashboard = dashboards.find(
        (dashboard) => Number(dashboard.id) === storedDashboardId,
      );
      activeDashboardId = storedDashboard?.id || dashboards[0]?.id || null;
      if (activeDashboardId) {
        localStorage.setItem(ACTIVE_DASHBOARD_KEY, activeDashboardId);
      }
      console.log("✅ Dashboards loaded:", dashboards);
      console.log("✅ Active Dashboard ID:", activeDashboardId);
    } catch (error) {
      console.warn(
        "Could not load dashboards. Endpoint may not exist yet:",
        error,
      );
    }

    try {
      const loadedCategories = await apiRequest(
        `${CATEGORIES_API_PATH}?dashboardId=${activeDashboardId}`,
        {
          method: "GET",
        },
      );

      categories = Array.isArray(loadedCategories) ? loadedCategories : [];
      activeCategoryId = categories[0]?.id || null;
      console.log("✅ Categories loaded:", categories);
      console.log("✅ Active Category ID:", activeCategoryId);

      // Load statuses for the active category
      if (activeCategoryId) {
        await loadStatuses();
      }
    } catch (error) {
      console.warn(
        "Could not load categories. Endpoint may not exist yet:",
        error,
      );
    }

    try {
      const loadedTasks = await apiRequest(
        `${TASKS_API_PATH}?dashboardId=${activeDashboardId}`,
        {
          method: "GET",
        },
      );

      tasks = Array.isArray(loadedTasks)
        ? loadedTasks.map((task) => ({
            ...task,
            status: String(task.statusId || ""),
          }))
        : [];
    } catch (error) {
      console.warn("Could not load tasks. Endpoint may not exist yet:", error);
    }

    renderDashboard();
  }

  async function loadWorkspaceData() {
    categories = [];
    tasks = [];
    statusPanels = [];
    activeCategoryId = null;
    activeStatusId = "";

    if (!activeDashboardId) {
      renderDashboard();
      return;
    }

    try {
      const [loadedCategories, loadedTasks] = await Promise.all([
        apiRequest(`${CATEGORIES_API_PATH}?dashboardId=${activeDashboardId}`, {
          method: "GET",
        }),
        apiRequest(`${TASKS_API_PATH}?dashboardId=${activeDashboardId}`, {
          method: "GET",
        }),
      ]);

      categories = Array.isArray(loadedCategories) ? loadedCategories : [];
      activeCategoryId = categories[0]?.id || null;
      tasks = Array.isArray(loadedTasks)
        ? loadedTasks.map((task) => ({
            ...task,
            status: String(task.statusId || ""),
          }))
        : [];

      if (activeCategoryId) {
        await loadStatuses();
      }
    } catch (error) {
      console.error("Could not load workspace data:", error);
    }

    renderDashboard();
  }

  async function loadStatuses() {
    if (!activeCategoryId) {
      console.warn("⚠️ No activeCategoryId - no statuses available");
      statusPanels = [];
      return;
    }

    try {
      const loadedStatuses = await apiRequest(
        `${STATUS_API_PATH}?categoryId=${activeCategoryId}`,
        {
          method: "GET",
        },
      );

      console.log("✅ Statuses loaded from backend:", loadedStatuses);

      if (Array.isArray(loadedStatuses) && loadedStatuses.length) {
        statusPanels = loadedStatuses.map((status) => ({
          id: String(status.id),
          backendId: status.id,
          name: status.name,
        }));
      } else {
        console.warn("⚠️ No statuses from backend - empty array");
        statusPanels = [];
      }
      console.log("✅ Final statusPanels:", statusPanels);
    } catch (err) {
      console.error("❌ Cannot load statuses", err);
      statusPanels = [];
    }
  }

  function handleTaskAction(action, taskElement) {
    if (!taskElement) {
      return false;
    }

    const taskId = Number(taskElement.dataset.taskId);
    const taskStatusId = taskElement.dataset.statusId;

    if (action === "expand-task") {
      const task = tasks.find((item) => Number(item.id) === taskId);
      activeTaskMenuStatusId = null;
      openTaskDetails(task?.title || "Task", task?.description || "");
      return true;
    }

    if (action === "edit-task") {
      const task = tasks.find((item) => Number(item.id) === taskId);

      if (!task) {
        return true;
      }

      activeTaskMenuStatusId = null;
      activeTaskMenuPosition = null;
      openTaskEditModal(task);
      return true;
    }

    if (action === "delete-task") {
      const confirmed = confirm("Usunac zadanie?");

      if (!confirmed) {
        return true;
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
      return true;
    }

    if (action === "move-task") {
      if (statusPanels.length <= 1) {
        alert("Brak innego statusu do przeniesienia.");
        return true;
      }

      const currentStatus = String(taskStatusId || "");
      const availableStatuses = statusPanels.filter(
        (panel) => panel.id !== currentStatus,
      );
      const selectedLabel = prompt(
        `Wybierz status:\n${availableStatuses
          .map((panel, index) => `${index + 1}. ${panel.name}`)
          .join("\n")}`,
      );
      const selectedStatus = availableStatuses[Number(selectedLabel) - 1];

      if (!selectedStatus) {
        return true;
      }

      apiRequest(`${TASKS_API_PATH}/${taskId}/status`, {
        method: "PATCH",
        body: JSON.stringify({
          statusId: Number(selectedStatus.id),
        }),
      })
        .then((updatedTask) => {
          tasks = tasks.map((item) =>
            Number(item.id) === taskId
              ? {
                  ...item,
                  ...updatedTask,
                }
              : item,
          );

          activeTaskMenuStatusId = null;
          activeTaskMenuPosition = null;
          renderDashboard();
        })
        .catch((error) => {
          console.error("Failed to move task:", error);
          alert("Nie udalo sie zmienic statusu zadania.");
        });
      return true;
    }

    return false;
  }

  if (addCategoryBtn) {
    addCategoryBtn.addEventListener("click", openCategoryModal);
  }

  if (addDashboardBtn) {
    addDashboardBtn.addEventListener("click", openDashboardModal);
  }

  if (exportCsvBtn) {
    exportCsvBtn.addEventListener("click", async () => {
      exportCsvBtn.disabled = true;
      exportCsvBtn.textContent = "Eksportowanie...";

      try {
        const { blob, filename } = await apiDownload(buildExportPath(), {
          method: "GET",
        });
        downloadBlob(blob, filename);
      } catch (error) {
        console.error("Failed to export tasks:", error);

        let errorMessage = "Nie udało się wyeksportować zadań.";
        if (error.message && error.message.includes("Session expired")) {
          errorMessage = "Sesja wygasła. Zaloguj się ponownie.";
        } else if (error.message && error.message.includes("No token")) {
          errorMessage = "Musisz się zalogować aby wyeksportować zadania.";
        } else if (error.message) {
          errorMessage = `Błąd: ${error.message}`;
        }

        alert(errorMessage);
      } finally {
        exportCsvBtn.disabled = false;
        exportCsvBtn.textContent = "Eksport CSV";
      }
    });
  }

  if (cancelBtn) {
    cancelBtn.addEventListener("click", closeAllModals);
  }

  if (dashboardCancelBtn) {
    dashboardCancelBtn.addEventListener("click", closeAllModals);
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
          body: JSON.stringify({
            name,
            dashboardId: activeDashboardId,
          }),
        });

        categories.push(savedCategory);
        activeCategoryId = Number(savedCategory.id);
        await loadStatuses();
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

  if (dashboardSaveBtn) {
    dashboardSaveBtn.addEventListener("click", async () => {
      const name = dashboardNameInput.value.trim();

      if (!name) {
        dashboardNameInput.focus();
        return;
      }

      dashboardSaveBtn.disabled = true;

      try {
        const dashboard = await apiRequest(DASHBOARDS_API_PATH, {
          method: "POST",
          body: JSON.stringify({ name }),
        });

        dashboards.push(dashboard);
        activeDashboardId = dashboard.id;
        localStorage.setItem(ACTIVE_DASHBOARD_KEY, activeDashboardId);
        closeAllModals();
        await loadWorkspaceData();
      } catch (error) {
        console.error("Failed to create dashboard:", error);
        alert("Nie udalo sie utworzyc workspace.");
      } finally {
        dashboardSaveBtn.disabled = false;
      }
    });
  }

  if (taskSaveBtn) {
    taskSaveBtn.addEventListener("click", async () => {
      const taskName = taskNameInput.value.trim();
      const taskDescription = taskDescriptionInput.value.trim();

      if (!taskName) {
        return;
      }

      taskSaveBtn.disabled = true;

      try {
        const taskPayload = createTaskPayload();

        if (!taskPayload) {
          alert("Wybierz status dla zadania");
          return;
        }

        if (!taskPayload.title) {
          return;
        }

        if (editingTaskId) {
          const updatedTask = await apiRequest(
            `${TASKS_API_PATH}/${editingTaskId}`,
            {
              method: "PATCH",
              body: JSON.stringify(taskPayload),
            },
          );

          tasks = tasks.map((task) =>
            Number(task.id) === editingTaskId
              ? {
                  ...task,
                  ...updatedTask,
                }
              : task,
          );

          activeTaskMenuStatusId = null;
          closeAllModals();
          renderDashboard();
          return;
        }

        const savedTask = await apiRequest(TASKS_API_PATH, {
          method: "POST",
          body: JSON.stringify(taskPayload),
        });

        tasks.push(savedTask);

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

  if (viewSelect) {
    viewSelect.addEventListener("change", () => {
      activeView = viewSelect.value;
      activePanelMenuStatusId = null;
      activeTaskMenuStatusId = null;
      activePanelMenuPosition = null;
      activeTaskMenuPosition = null;
      renderDashboard();
    });
  }

  if (dashboardSelect) {
    dashboardSelect.addEventListener("change", async () => {
      activeDashboardId = Number(dashboardSelect.value) || null;

      if (activeDashboardId) {
        localStorage.setItem(ACTIVE_DASHBOARD_KEY, activeDashboardId);
      }

      closeAllModals();
      await loadWorkspaceData();
    });
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

  if (categoryTabs) {
    categoryTabs.addEventListener("click", async (event) => {
      const categoryItem = event.target.closest(".category-tab-item");

      if (!categoryItem) {
        return;
      }

      const categoryId = Number(categoryItem.dataset.categoryId);
      const action = event.target.dataset.action;

      if (action === "select-category") {
        activeCategoryId = categoryId;
        openCategoryMenu = null;

        await loadStatuses();

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
          (item) => Number(item.id) === categoryId,
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
              Number(item.id) === categoryId ? updatedCategory : item,
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
              (item) => Number(item.id) !== categoryId,
            );
            tasks = tasks.filter(
              (task) => Number(getTaskCategoryId(task)) !== categoryId,
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
  }

  container.addEventListener("click", async (event) => {
    const action = event.target.dataset.action;
    const panelElement = event.target.closest(".task-panel");
    const taskElement = event.target.closest(".task-item, .gantt-task-row");

    if (action === "gantt-zoom-out") {
      ganttZoomIndex = Math.max(0, ganttZoomIndex - 1);
      renderDashboard();
      return;
    }

    if (action === "gantt-zoom-in") {
      ganttZoomIndex = Math.min(
        GANTT_ZOOM_LEVELS.length - 1,
        ganttZoomIndex + 1,
      );
      renderDashboard();
      return;
    }

    if (action === "gantt-fit") {
      ganttZoomIndex = 0;
      renderDashboard();
      return;
    }

    if (action === "gantt-today") {
      ganttScrollTarget = "today";
      renderDashboard();
      return;
    }

    if (action === "add-status") {
      if (!activeCategoryId) {
        alert("Nie ma wybranej kategorii. Proszę wybrać kategorię.");
        return;
      }

      const name = prompt("Nazwa nowego statusu:");

      if (!name?.trim()) return;

      try {
        const created = await apiRequest(STATUS_API_PATH, {
          method: "POST",
          body: JSON.stringify({
            categoryId: activeCategoryId,
            name: name.trim(),
          }),
        });

        statusPanels.push({
          id: String(created.id),
          backendId: created.id,
          name: created.name,
        });

        renderDashboard();
      } catch (err) {
        console.error(err);
        alert("Nie udało się utworzyć statusu.");
      }

      return;
    }

    if (taskElement && handleTaskAction(action, taskElement)) {
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
      const panel = statusPanels.find(
        (s) => String(s.id) === String(panelStatusId),
      );

      const name = prompt("Nowa nazwa statusu:", panel?.name || "");

      if (!name?.trim()) return;

      try {
        const updated = await apiRequest(
          `${STATUS_API_PATH}/${panel.backendId || panel.id}`,
          {
            method: "PUT",
            body: JSON.stringify({
              name: name.trim(),
            }),
          },
        );

        statusPanels = statusPanels.map((s) =>
          String(s.id) === String(panelStatusId)
            ? {
                ...s,
                name: updated.name,
              }
            : s,
        );

        renderDashboard();
      } catch (err) {
        alert("Nie udało się zmienić statusu.");
      }

      return;
    }

    if (action === "delete-status") {
      const confirmed = confirm("Usunąć status?");

      if (!confirmed) return;

      const panel = statusPanels.find(
        (s) => String(s.id) === String(panelStatusId),
      );

      try {
        await apiRequest(`${STATUS_API_PATH}/${panel.backendId || panel.id}`, {
          method: "DELETE",
        });

        statusPanels = statusPanels.filter(
          (s) => String(s.id) !== String(panelStatusId),
        );

        activePanelMenuStatusId = null;
        activePanelMenuPosition = null;

        renderDashboard();
      } catch (err) {
        alert("Nie udało się usunąć statusu.");
      }

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
  });

  container.addEventListener("dragover", (event) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
    const panelElement = event.target.closest(".task-panel");
    if (panelElement) {
      panelElement.style.backgroundColor = "rgba(100, 150, 200, 0.1)";
    }
  });

  container.addEventListener("dragleave", (event) => {
    const panelElement = event.target.closest(".task-panel");
    if (panelElement) {
      panelElement.style.backgroundColor = "";
    }
  });

  container.addEventListener("drop", async (event) => {
    event.preventDefault();
    console.log("🔴 DROP EVENT FIRED");

    const panelElement = event.target.closest(".task-panel");
    console.log("📌 Panel Element:", panelElement);
    console.log("📌 draggedTaskId:", draggedTaskId);

    if (!panelElement || draggedTaskId === null) {
      console.log("❌ No panel element or draggedTaskId is null");
      return;
    }

    const newStatusId = panelElement.dataset.statusId;
    console.log("📌 newStatusId from dataset:", newStatusId);

    const task = tasks.find((t) => Number(t.id) === draggedTaskId);
    console.log("📌 Task found:", task);
    if (!task) {
      console.log("❌ Task not found");
      return;
    }

    const currentStatus = String(task.statusId || "");
    console.log(
      "📌 currentStatus:",
      currentStatus,
      "vs newStatusId:",
      newStatusId,
    );

    if (currentStatus === newStatusId) {
      console.log("⚠️ Same status, skipping");
      return;
    }

    try {
      console.log(
        "🔵 Sending PATCH request with statusId:",
        Number(newStatusId),
      );
      const updatedTask = await apiRequest(
        `${TASKS_API_PATH}/${draggedTaskId}/status`,
        {
          method: "PATCH",
          body: JSON.stringify({
            statusId: Number(newStatusId),
          }),
        },
      );

      console.log("✅ Updated task:", updatedTask);

      // Reload tasks from server to ensure UI is in sync
      try {
      const loadedTasks = await apiRequest(
        `${TASKS_API_PATH}?dashboardId=${activeDashboardId}`,
        {
          method: "GET",
        },
      );

        tasks = Array.isArray(loadedTasks)
          ? loadedTasks.map((task) => ({
              ...task,
              status: String(task.statusId || ""),
            }))
          : [];
        console.log("✅ Tasks reloaded from server:", tasks);
      } catch (err) {
        console.warn("⚠️ Failed to reload tasks, using local update:", err);
        // Fallback: update locally
        tasks = tasks.map((t) =>
          Number(t.id) === draggedTaskId
            ? {
                ...t,
                ...updatedTask,
              }
            : t,
        );
      }

      renderDashboard();
    } catch (err) {
      console.error("❌ Error:", err);
      alert("Nie udało się przenieść zadania.");
    }
  });

  container.addEventListener("dragstart", (event) => {
    const taskElement = event.target.closest(".task-item");

    if (!taskElement) {
      console.log("❌ No task element found");
      return;
    }

    draggedTaskId = Number(taskElement.dataset.taskId);
    console.log("🟢 DRAG START - draggedTaskId:", draggedTaskId);
    taskElement.classList.add("dragging");
  });

  container.addEventListener("dragend", (event) => {
    const taskElement = event.target.closest(".task-item");

    if (taskElement) {
      taskElement.classList.remove("dragging");
    }

    draggedTaskId = null;
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

  if (panelNameInput && saveBtn) {
    panelNameInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        saveBtn.click();
      }
    });
  }

  if (dashboardNameInput && dashboardSaveBtn) {
    dashboardNameInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        dashboardSaveBtn.click();
      }
    });
  }

  if (taskNameInput && taskSaveBtn) {
    taskNameInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        taskSaveBtn.click();
      }
    });
  }

  loadDashboardData();
});
