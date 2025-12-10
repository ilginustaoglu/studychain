(() => {
  const root = document.documentElement;
  const key = "sc_theme";
  const saved = localStorage.getItem(key);
  if (saved === "light") {
    root.classList.add("light");
  }
  const updateIcon = (button) => {
    if (!button) return;
    // Show the next theme's icon: light -> moon, dark -> sun
    button.textContent = root.classList.contains("light") ? "🌙" : "☀️";
  };
  const wireUp = () => {
    const buttons = Array.from(
      document.querySelectorAll('[data-theme-toggle], #themeToggle, .theme-toggle')
    );
    if (buttons.length === 0) return;
    const toggle = () => {
      root.classList.toggle("light");
      localStorage.setItem(
        key,
        root.classList.contains("light") ? "light" : "dark"
      );
      buttons.forEach(updateIcon);
    };
    buttons.forEach((btn) => {
      updateIcon(btn);
      btn.addEventListener("click", toggle);
    });
  };
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", wireUp);
  } else {
    wireUp();
  }
})(); 


