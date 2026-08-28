(function () {
  var form = document.getElementById("contactForm");
  if (!form) return;

  var feedback = document.getElementById("contactFeedback");
  var submitBtn = document.getElementById("contactSubmit");
  var apiBase = (window.PBW_WEBSITE_API || "/adminpanel/api/website").replace(/\/$/, "");

  function showFeedback(text, isError) {
    if (!feedback) return;
    feedback.hidden = false;
    feedback.textContent = text;
    feedback.classList.toggle("is-error", !!isError);
    feedback.classList.toggle("is-success", !isError);
  }

  form.addEventListener("submit", function (e) {
    e.preventDefault();

    var name = document.getElementById("contactName").value.trim();
    var email = document.getElementById("contactEmail").value.trim();
    var subject = document.getElementById("contactSubject").value.trim();
    var message = document.getElementById("contactMessage").value.trim();

    if (!name || !email || !message) {
      showFeedback("Please fill in your name, email, and message.", true);
      return;
    }

    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = "Sending…";
    }

    fetch(apiBase + "/contact", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name: name,
        email: email,
        subject: subject,
        message: message,
      }),
    })
      .then(function (res) {
        return res.json().then(function (data) {
          return { ok: res.ok, data: data };
        });
      })
      .then(function (result) {
        if (result.ok && result.data && result.data.success) {
          form.reset();
          showFeedback(
            result.data.message || "Thank you — we received your message and will reply soon.",
            false
          );
        } else {
          var err =
            (result.data && (result.data.message || result.data.errors)) ||
            "Unable to send your message. Please try again or email us directly.";
          showFeedback(typeof err === "string" ? err : "Please check your details and try again.", true);
        }
      })
      .catch(function () {
        showFeedback(
          "Could not reach the server. Check your connection or email support@posbillingwala.com.",
          true
        );
      })
      .finally(function () {
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.textContent = "Send message";
        }
      });
  });
})();
