/* ==========================================================================
   TaskFlow - auth.js
   Handles storing the JWT token + current user, and route guarding.
   Runs on every page (included via <script> in every HTML file).
   ========================================================================== */

const AuthStorage = {
  TOKEN_KEY: "taskflow_token",
  USER_KEY: "taskflow_user",

  getToken() {
    return localStorage.getItem(this.TOKEN_KEY);
  },

  setToken(token) {
    localStorage.setItem(this.TOKEN_KEY, token);
  },

  clearToken() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  },

  getUser() {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },

  setUser(user) {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  },

  isAuthenticated() {
    return !!this.getToken();
  },
};

/**
 * Call this at the top of any page that REQUIRES login (dashboard,
 * create-task, edit-task, task-details). Redirects to login if
 * there's no token.
 */
function requireAuth() {
  if (!AuthStorage.isAuthenticated()) {
    window.location.href = "login.html";
  }
}

/**
 * Call this on login/register pages. If the user is already logged in,
 * skip straight to the dashboard.
 */
function redirectIfAuthenticated() {
  if (AuthStorage.isAuthenticated()) {
    window.location.href = "dashboard.html";
  }
}

/**
 * Logs the user out: clears storage and redirects to the landing page.
 */
function logout() {
  AuthStorage.clearToken();
  window.location.href = "index.html";
}

/**
 * Wires up any element with [data-logout] to call logout() on click.
 * Also fills in any element with [data-user-name] with the logged in
 * user's name, if available.
 */
document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("[data-logout]").forEach((el) => {
    el.addEventListener("click", (e) => {
      e.preventDefault();
      logout();
    });
  });

  const user = AuthStorage.getUser();
  if (user) {
    document.querySelectorAll("[data-user-name]").forEach((el) => {
      el.textContent = user.name;
    });
  }
});
