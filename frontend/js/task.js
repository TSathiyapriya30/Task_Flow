/* ==========================================================================
   TaskFlow - task.js
   Shared logic for create-task.html, edit-task.html, and task-details.html.
   Each page only has the DOM elements it needs, so we feature-detect which
   page we're on and wire up only the relevant behavior.
   ========================================================================== */

document.addEventListener("DOMContentLoaded", () => {
  requireAuth();

  if (document.getElementById("create-task-form")) {
    initCreateTaskPage();
  }

  if (document.getElementById("edit-task-form")) {
    initEditTaskPage();
  }

  if (document.getElementById("task-detail-container")) {
    initTaskDetailsPage();
  }
});

/* ---------------- Create Task ---------------- */

function initCreateTaskPage() {
  const form = document.getElementById("create-task-form");
  const alertBox = document.getElementById("form-alert");
  const submitBtn = document.getElementById("submit-btn");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    hideAlert(alertBox);

    const payload = readTaskForm();
    if (!payload.title) {
      showAlert(alertBox, "Title is required.");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Creating...";

    try {
      const task = await TaskFlowAPI.createTask(payload);
      window.location.href = `task-details.html?id=${task.id}`;
    } catch (err) {
      showAlert(alertBox, err.message || "Could not create task.");
      submitBtn.disabled = false;
      submitBtn.textContent = "Create Task";
    }
  });
}

/* ---------------- Edit Task ---------------- */

function initEditTaskPage() {
  const taskId = getQueryParam("id");
  if (!taskId) {
    window.location.href = "dashboard.html";
    return;
  }

  const form = document.getElementById("edit-task-form");
  const alertBox = document.getElementById("form-alert");
  const submitBtn = document.getElementById("submit-btn");

  loadTaskIntoForm(taskId, alertBox);

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    hideAlert(alertBox);

    const payload = readTaskForm();
    if (!payload.title) {
      showAlert(alertBox, "Title is required.");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Saving...";

    try {
      await TaskFlowAPI.updateTask(taskId, payload);
      window.location.href = `task-details.html?id=${taskId}`;
    } catch (err) {
      showAlert(alertBox, err.message || "Could not update task.");
      submitBtn.disabled = false;
      submitBtn.textContent = "Save Changes";
    }
  });
}

async function loadTaskIntoForm(taskId, alertBox) {
  try {
    const task = await TaskFlowAPI.getTask(taskId);
    document.getElementById("title").value = task.title;
    document.getElementById("description").value = task.description || "";
    document.getElementById("status").value = task.status;
    document.getElementById("priority").value = task.priority;
    document.getElementById("dueDate").value = task.dueDate || "";
  } catch (err) {
    showAlert(alertBox, err.message || "Could not load task.");
  }
}

/* ---------------- Shared form reader ---------------- */

function readTaskForm() {
  return {
    title: document.getElementById("title").value.trim(),
    description: document.getElementById("description").value.trim(),
    status: document.getElementById("status").value,
    priority: document.getElementById("priority").value,
    dueDate: document.getElementById("dueDate").value || null,
  };
}

/* ---------------- Task Details ---------------- */

function initTaskDetailsPage() {
  const taskId = getQueryParam("id");
  if (!taskId) {
    window.location.href = "dashboard.html";
    return;
  }

  loadTaskDetails(taskId);

  document.getElementById("delete-task-btn").addEventListener("click", () => {
    document.getElementById("delete-modal").classList.add("visible");
  });

  document.getElementById("cancel-delete").addEventListener("click", () => {
    document.getElementById("delete-modal").classList.remove("visible");
  });

  document.getElementById("confirm-delete").addEventListener("click", async () => {
    try {
      await TaskFlowAPI.deleteTask(taskId);
      window.location.href = "dashboard.html";
    } catch (err) {
      alert(err.message || "Could not delete task.");
    }
  });

  document.getElementById("status-select").addEventListener("change", async (e) => {
    try {
      const updated = await TaskFlowAPI.updateTaskStatus(taskId, e.target.value);
      renderStatusBadge(updated.status);
    } catch (err) {
      alert(err.message || "Could not update status.");
    }
  });
}

async function loadTaskDetails(taskId) {
  const container = document.getElementById("task-detail-container");

  try {
    const task = await TaskFlowAPI.getTask(taskId);

    document.getElementById("detail-title").textContent = task.title;
    document.getElementById("detail-description").textContent = task.description || "No description provided.";
    document.getElementById("detail-due-date").textContent = formatDate(task.dueDate);
    document.getElementById("detail-priority").innerHTML =
      `<span class="badge badge-priority-${task.priority}">${humanizeEnum(task.priority)}</span>`;
    document.getElementById("detail-created").textContent = formatDateTime(task.createdAt);
    document.getElementById("detail-updated").textContent = formatDateTime(task.updatedAt);

    document.getElementById("status-select").value = task.status;
    renderStatusBadge(task.status);

    document.getElementById("edit-task-link").href = `edit-task.html?id=${task.id}`;

    container.style.display = "block";
    document.getElementById("detail-loading").style.display = "none";
  } catch (err) {
    document.getElementById("detail-loading").innerHTML =
      `<div class="empty-state"><h3>Task not found</h3><p>${escapeHtml(err.message)}</p><a class="btn btn-primary" href="dashboard.html">Back to Dashboard</a></div>`;
  }
}

function renderStatusBadge(status) {
  const badge = document.getElementById("detail-status-badge");
  badge.className = `badge badge-status-${status}`;
  badge.textContent = humanizeEnum(status);
}
