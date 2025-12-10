(() => {
  const fab = document.getElementById("toolsFab");
  const panel = document.getElementById("toolsPanel");
  if (!fab || !panel) return;
  fab.addEventListener("click", () => {
    panel.classList.toggle("hidden");
  });
})(); 


