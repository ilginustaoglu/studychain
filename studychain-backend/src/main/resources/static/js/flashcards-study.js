(() => {
  const list = Array.from(document.querySelectorAll("#cardsData li")).map(li => ({
    front: li.getAttribute("data-front") || "",
    back: li.getAttribute("data-back") || ""
  }));
  const cardEl = document.getElementById("card");
  const frontEl = cardEl?.querySelector(".front");
  const backEl = cardEl?.querySelector(".back");
  const prevBtn = document.getElementById("prevBtn");
  const nextBtn = document.getElementById("nextBtn");
  const flipBtn = document.getElementById("flipBtn");
  const progressEl = document.getElementById("progress");

  let index = 0;
  let showingFront = true;

  function render() {
    const total = list.length;
    if (!cardEl || !frontEl || !backEl || total === 0) {
      if (progressEl) progressEl.textContent = "0 / 0";
      if (frontEl) frontEl.textContent = "No cards";
      cardEl?.classList.add("show-front");
      cardEl?.classList.remove("show-back");
      [prevBtn, nextBtn, flipBtn].forEach(btn => btn && (btn.disabled = true));
      return;
    }
    index = Math.max(0, Math.min(index, total - 1));
    const current = list[index];
    frontEl.textContent = current.front || "";
    backEl.textContent = current.back || "";
    if (showingFront) {
      cardEl.classList.add("show-front");
      cardEl.classList.remove("show-back");
    } else {
      cardEl.classList.add("show-back");
      cardEl.classList.remove("show-front");
    }
    if (progressEl) progressEl.textContent = `${index + 1} / ${total}`;
    if (prevBtn) prevBtn.disabled = index === 0;
    if (nextBtn) nextBtn.disabled = index >= total - 1;
  }

  function flip() {
    showingFront = !showingFront;
    render();
  }

  function next() {
    if (index < list.length - 1) {
      index += 1;
      showingFront = true;
      render();
    }
  }

  function prev() {
    if (index > 0) {
      index -= 1;
      showingFront = true;
      render();
    }
  }

  flipBtn?.addEventListener("click", flip);
  nextBtn?.addEventListener("click", next);
  prevBtn?.addEventListener("click", prev);
  cardEl?.addEventListener("click", flip);
  window.addEventListener("keydown", (e) => {
    if (e.key === " " || e.key === "Enter") {
      e.preventDefault();
      flip();
    } else if (e.key === "ArrowRight") {
      next();
    } else if (e.key === "ArrowLeft") {
      prev();
    }
  });

  render();
})();


