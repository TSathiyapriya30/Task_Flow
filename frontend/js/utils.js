/* ==========================================================================
   TaskFlow - utils.js
   Small shared helper functions used across pages.
   ========================================================================== */

/**
 * Reads a query-string parameter from the current URL.
 */
function getQueryParam(name) {
  const params = new URLSearchParams(window.location.search);
  return params.get(name);
}

/**
 * Formats an ISO date string (e.g. "2026-09-15") into a readable date.
 */
function formatDate(isoDate) {
  if (!isoDate) return "No due date";
  const date = new Date(isoDate + "T00:00:00");
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

/**
 * Formats an ISO datetime string into a readable date + time.
 */
function formatDateTime(isoDateTime) {
  if (!isoDateTime) return "";
  const date = new Date(isoDateTime);
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Turns "IN_PROGRESS" into "In Progress" for display purposes.
 */
function humanizeEnum(value) {
  if (!value) return "";
  return value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

/**
 * Escapes text before inserting it into innerHTML, to avoid
 * accidentally rendering user input as HTML.
 */
function escapeHtml(text) {
  if (text === null || text === undefined) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

/**
 * Shows an alert banner element (expects an element with class "alert").
 */
function showAlert(element, message, type = "danger") {
  if (!element) return;
  element.textContent = message;
  element.className = `alert alert-${type} visible`;
}

function hideAlert(element) {
  if (!element) return;
  element.className = "alert";
  element.textContent = "";
}

/**
 * Displays validation/field errors coming back from the API
 * (GlobalExceptionHandler returns { message, errors: [...] }).
 */
function formatApiErrors(errorBody) {
  if (!errorBody) return "Something went wrong. Please try again.";
  if (errorBody.errors && errorBody.errors.length > 0) {
    return errorBody.errors.join(" | ");
  }
  return errorBody.message || "Something went wrong. Please try again.";
}
