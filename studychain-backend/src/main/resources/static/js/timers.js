(() => {
  let isStudy = true;
  let isRunning = false;
  let studyMs = 0;
  let breakMs = 0;
  let timerId = null;

  const studyDisplay = document.getElementById("studyDisplay");
  const breakDisplay = document.getElementById("breakDisplay");
  const modeToggle = document.getElementById("modeToggle");
  const playPause = document.getElementById("playPause");
  const resetTimers = document.getElementById("resetTimers");
  const studyBlock = studyDisplay ? studyDisplay.parentElement : null;
  const breakBlock = breakDisplay ? breakDisplay.parentElement : null;

  if (!studyDisplay || !breakDisplay || !modeToggle || !playPause || !resetTimers) return;

  const fmt = (ms) => {
    const total = Math.floor(ms / 1000);
    const h = String(Math.floor(total / 3600)).padStart(2, "0");
    const m = String(Math.floor((total % 3600) / 60)).padStart(2, "0");
    const s = String(total % 60).padStart(2, "0");
    return `${h}:${m}:${s}`;
  };

  const render = () => {
    studyDisplay.textContent = fmt(studyMs);
    breakDisplay.textContent = fmt(breakMs);
    // Show the inactive timer name on the button
    modeToggle.textContent = isStudy ? "Break" : "Study";
    playPause.textContent = isRunning ? "Stop" : "Play";

    // Highlight the running timer block
    if (studyBlock && breakBlock) {
      studyBlock.classList.toggle("is-active", isRunning && isStudy);
      breakBlock.classList.toggle("is-active", isRunning && !isStudy);
    }
  };

  const tick = () => {
    if (!isRunning) return;
    if (isStudy) {
      studyMs += 1000;
    }
    else breakMs += 1000;
    render();
  };

  modeToggle.addEventListener("click", async () => {
    isStudy = !isStudy;
    render();
  });

  playPause.addEventListener("click", async () => {
    isRunning = !isRunning;
    if (isRunning && !timerId) {
      timerId = setInterval(tick, 1000);
    }
    if (!isRunning && timerId) {
      clearInterval(timerId);
      timerId = null;
    }
    render();
  });

  resetTimers.addEventListener("click", async () => {
    const ok = confirm("Sayaçlar sıfırlansın mı?");
    if (!ok) return;
    // Record a session if any study time
    const minutes = Math.floor(studyMs / 60000);
    if (minutes > 0) {
      try {
        await fetch("/study/session", {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({ minutes: String(minutes) }).toString(),
        });
      } catch {}
    }
    studyMs = 0;
    breakMs = 0;
    isRunning = false;
    if (timerId) {
      clearInterval(timerId);
      timerId = null;
    }
    render();
  });

  render();
})(); 


