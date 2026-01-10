(() => {
  const root = document.documentElement;
  const key = "sc_theme";
  const saved = localStorage.getItem(key);
  if (saved === "light") {
    root.classList.add("light");
  }
  // Initialize settings icon on page load
  const initSettingsIcon = () => {
    const settingsIcon = document.querySelector('img#settingsIcon');
    if (settingsIcon) {
      settingsIcon.src = root.classList.contains("light")
        ? "/images/icons/12-removebg-preview (1).png"  // light theme settings icon
        : "/images/icons/11-removebg-preview (1).png";  // dark theme settings icon
    }
  };
  const updateIcon = (button) => {
    if (!button) return;
    // Show current theme's icon: light -> sun, dark -> moon
    const icon = button.querySelector('img#themeIcon') || button.querySelector('img');
    if (icon) {
      icon.src = root.classList.contains("light") 
        ? "/images/icons/10-removebg-preview (1).png"  // sun for light theme
        : "/images/icons/13-removebg-preview (1).png";  // moon for dark theme
    }
    // Update settings icon based on theme
    const settingsIcon = document.querySelector('img#settingsIcon');
    if (settingsIcon) {
      settingsIcon.src = root.classList.contains("light")
        ? "/images/icons/12-removebg-preview (1).png"  // light theme settings icon
        : "/images/icons/11-removebg-preview (1).png";  // dark theme settings icon
    }
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
    document.addEventListener("DOMContentLoaded", () => {
      wireUp();
      initSettingsIcon();
    });
  } else {
    wireUp();
    initSettingsIcon();
  }
})(); 


