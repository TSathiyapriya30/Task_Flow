/* ==========================================================================
   TaskFlow - register.js
   ========================================================================== */

document.addEventListener("DOMContentLoaded", () => {
  redirectIfAuthenticated();

  const form = document.getElementById("register-form");
  const alertBox = document.getElementById("form-alert");
  const submitBtn = document.getElementById("submit-btn");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    hideAlert(alertBox);

    const name = document.getElementById("name").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    if (!name || !email || !password || !confirmPassword) {
      showAlert(alertBox, "Please fill in all fields.");
      return;
    }

    if (password !== confirmPassword) {
      showAlert(alertBox, "Passwords do not match.");
      return;
    }

    if (password.length < 8) {
      showAlert(alertBox, "Password must be at least 8 characters long.");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Creating account...";

    try {
      const response = await TaskFlowAPI.register({ name, email, password, confirmPassword });
      AuthStorage.setToken(response.token);
      AuthStorage.setUser(response.user);
      window.location.href = "dashboard.html";
    } catch (err) {
      showAlert(alertBox, err.message || "Registration failed. Please try again.");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Create Account";
    }
  });
});
