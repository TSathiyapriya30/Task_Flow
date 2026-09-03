/* ==========================================================================
   TaskFlow - dashboard.js
   Loads the summary stats + task list, and wires up search/filter/sort
   and delete confirmation.
   ========================================================================== */

let currentFilters = {
  search: "",
  status: "",
  priority: "",
  sortBy: "createdAt",
  order: "desc",
};

let taskIdPendingDelete = null;

document.addEventListener("DOMContentLoaded", () => {
  requireAuth();

  loadSummary();
  loadTasks();

  const searchInput = document.getElementById("search-input");
  const statusFilter = document.getElementById("status-filter");
  const priorityFilter = document.getElementById("priority-filter");
  const sortSelect = document.getElementById("sort-select");

  let searchTimeout;
  searchInput.addEventListener("input", () => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      currentFilters.search = searchInput.value.trim();
      loadTasks();
    }, 300);
  });

  statusFilter.addEventListener("change", () => {
    currentFilters.status = statusFilter.value;
    loadTasks();
  });

  priorityFilter.addEventListener("change", () => {
    currentFilters.priority = priorityFilter.value;
    loadTasks();
  });

  sortSelect.addEventListener("change", () => {
    const [sortBy, order] = sortSelect.value.split(":");
    currentFilters.sortBy = sortBy;
    currentFilters.order = order;
    loadTasks();
  });

  // Delete confirmation modal wiring
  document.getElementById("cancel-delete").addEventListener("click", closeDeleteModal);
  document.getElementById("confirm-delete").addEventListener("click", confirmDelete);
});

async function loadSummary() {
  try {
    const summary = await TaskFlowAPI.getTaskSummary();
    document.getElementById("stat-total").textContent = summary.total;
    document.getElementById("stat-pending").textContent = summary.pending;
    document.getElementById("stat-progress").textContent = summary.inProgress;
    document.getElementById("stat-completed").textContent = summary.completed;
  } catch (err) {
    console.error("Failed to load summary", err);
  }
}

async function loadTasks() {
  const listEl = document.getElementById("task-list");
  listEl.innerHTML = '<div class="loading">Loading tasks...</div>';

  try {
    const tasks = await TaskFlowAPI.getTasks(currentFilters);
    renderTasks(tasks);
  } catch (err) {
    listEl.innerHTML = `<div class="empty-state"><h3>Could not load tasks</h3><p>${escapeHtml(err.message)}</p></div>`;
  }
}

function renderTasks(tasks) {
  const listEl = document.getElementById("task-list");

  if (!tasks || tasks.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <h3>No tasks found</h3>
        <p>Try adjusting your filters, or create a new task to get started.</p>
      </div>`;
    return;
  }

  listEl.innerHTML = tasks.map(taskCardHtml).join("");

  listEl.querySelectorAll("[data-delete-id]").forEach((btn) => {
    btn.addEventListener("click", () => openDeleteModal(btn.getAttribute("data-delete-id")));
  });
}

function taskCardHtml(task) {
  return `
    <div class="task-card">
      <div class="task-card-main">
        <h3><a href="task-details.html?id=${task.id}">${escapeHtml(task.title)}</a></h3>
        ${task.description ? `<p class="task-description">${escapeHtml(task.description)}</p>` : ""}
        <div class="task-meta">
          <span class="badge badge-status-${task.status}">${humanizeEnum(task.status)}</span>
          <span class="badge badge-priority-${task.priority}">${humanizeEnum(task.priority)} priority</span>
          <span class="task-due">Due: ${formatDate(task.dueDate)}</span>
        </div>
      </div>
      <div class="task-actions">
        <a class="btn btn-secondary btn-sm" href="edit-task.html?id=${task.id}">Edit</a>
        <button class="btn btn-danger btn-sm" data-delete-id="${task.id}">Delete</button>
      </div>
    </div>
  `;
}

function openDeleteModal(taskId) {
  taskIdPendingDelete = taskId;
  document.getElementById("delete-modal").classList.add("visible");
}

function closeDeleteModal() {
  taskIdPendingDelete = null;
  document.getElementById("delete-modal").classList.remove("visible");
}

async function confirmDelete() {
  if (!taskIdPendingDelete) return;
  try {
    await TaskFlowAPI.deleteTask(taskIdPendingDelete);
    closeDeleteModal();
    loadSummary();
    loadTasks();
  } catch (err) {
    alert(err.message || "Could not delete task.");
    closeDeleteModal();
  }
}
