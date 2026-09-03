/* ==========================================================================
   TaskFlow - login.js
   ========================================================================== */

document.addEventListener("DOMContentLoaded", () => {
  redirectIfAuthenticated();

  const form = document.getElementById("login-form");
  const alertBox = document.getElementById("form-alert");
  const submitBtn = document.getElementById("submit-btn");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    hideAlert(alertBox);

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    if (!email || !password) {
      showAlert(alertBox, "Please fill in both email and password.");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Logging in...";

    try {
      const response = await TaskFlowAPI.login({ email, password });
      AuthStorage.setToken(response.token);
      AuthStorage.setUser(response.user);
      window.location.href = "dashboard.html";
    } catch (err) {
      showAlert(alertBox, err.message || "Login failed. Please try again.");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Log In";
    }
  });
});
