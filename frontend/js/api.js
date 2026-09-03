/* ==========================================================================
   TaskFlow - api.js
   Centralized, reusable functions for talking to the Spring Boot backend
   using the native fetch() API. No Axios, no frameworks.
   ========================================================================== */

/**
 * Base URL of the backend API.
 * Change this if your Spring Boot backend runs on a different host/port.
 * It is intentionally kept in one place so it's easy to configure.
 */
const API_BASE_URL = window.TASKFLOW_API_BASE_URL || "https://task-flow-iizd.onrender.com/api";

/**
 * Internal helper: performs a fetch() call, attaches the JWT (if present),
 * parses the JSON response, and throws a normalized error object on failure.
 */
async function apiRequest(path, { method = "GET", body, auth = true } = {}) {
  const headers = {
    "Content-Type": "application/json",
  };

  if (auth) {
    const token = AuthStorage.getToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (networkError) {
    throw {
      status: 0,
      message: "Could not reach the server. Is the backend running?",
    };
  }

  // 204 No Content or empty bodies
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    // If the token is invalid/expired, force the user back to login.
    if (response.status === 401 && auth) {
      AuthStorage.clearToken();
    }
    throw {
      status: response.status,
      message: data ? formatApiErrors(data) : "Request failed",
      body: data,
    };
  }

  return data;
}

const TaskFlowAPI = {
  // ---------------- Auth ----------------
  register(payload) {
    return apiRequest("/auth/register", { method: "POST", body: payload, auth: false });
  },

  login(payload) {
    return apiRequest("/auth/login", { method: "POST", body: payload, auth: false });
  },

  getCurrentUser() {
    return apiRequest("/auth/me", { method: "GET" });
  },

  // ---------------- Tasks ----------------
  getTasks({ search, status, priority, sortBy, order } = {}) {
    const params = new URLSearchParams();
    if (search) params.set("search", search);
    if (status) params.set("status", status);
    if (priority) params.set("priority", priority);
    if (sortBy) params.set("sortBy", sortBy);
    if (order) params.set("order", order);

    const query = params.toString();
    return apiRequest(`/tasks${query ? `?${query}` : ""}`, { method: "GET" });
  },

  getTaskSummary() {
    return apiRequest("/tasks/summary", { method: "GET" });
  },

  getTask(id) {
    return apiRequest(`/tasks/${id}`, { method: "GET" });
  },

  createTask(payload) {
    return apiRequest("/tasks", { method: "POST", body: payload });
  },

  updateTask(id, payload) {
    return apiRequest(`/tasks/${id}`, { method: "PUT", body: payload });
  },

  updateTaskStatus(id, status) {
    return apiRequest(`/tasks/${id}/status`, { method: "PATCH", body: { status } });
  },

  deleteTask(id) {
    return apiRequest(`/tasks/${id}`, { method: "DELETE" });
  },
};
