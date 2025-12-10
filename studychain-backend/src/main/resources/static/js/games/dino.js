(() => {
  const canvas = document.getElementById('dinoCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  // Game state
  const GROUND_Y = canvas.height - 30;
  const gravity = 0.6;
  const lowGravity = 0.35; // applied while jump is held
  const jumpVelocity = -8.0; // slower initial jump
  const dino = { x: 30, y: GROUND_Y - 32, w: 42, h: 32, vy: 0, onGround: true };
  let obstacles = [];
  let clouds = [];
  let frame = 0;
  // Base speed is chosen by UI; default medium scaled down to slow overall pace
  let baseSpeed = 4; // default medium after scaling (UI medium is 6 -> ~3.6)
  let speed = baseSpeed;
  let running = false;
  let score = 0;
  let best = 0;
  let lastTs = 0;
  let spawnCooldownMs = 0;

  // Variable jump state
  let jumpKeyHeld = false;
  let airTimeMs = 0;
  const maxAirMs = 4500;
  let jumpsAvailable = 2;

  // Fixed default speed (speed controls removed)
  baseSpeed = 4;
  speed = baseSpeed;

  // Helpers
  function reset() {
    dino.y = GROUND_Y - dino.h;
    dino.vy = 0;
    dino.onGround = true;
    jumpsAvailable = 2;
    obstacles = [];
    clouds = [];
    frame = 0;
    speed = baseSpeed;
    score = 0;
    lastTs = 0;
    spawnCooldownMs = 0;
    airTimeMs = 0;
    jumpKeyHeld = false;
  }

  function spawnGroup() {
    // Enforce separation from previous group's last obstacle
    const lastRight = obstacles.length ? (obstacles[obstacles.length - 1].x + obstacles[obstacles.length - 1].w) : 0;
    const speedFactorPx = 6 / Math.max(2, speed);
    const minSepPx = Math.floor((300 + Math.random() * 180) * speedFactorPx);
    const baseX = Math.max(canvas.width + 60, lastRight + minSepPx);

    // Helper to push obstacle at absolute X
    const addObstacleAbs = (absX, w, h) => {
      obstacles.push({ x: absX, y: GROUND_Y - h, w, h });
    };

    // Never more than 2 obstacles in a group
    const groupCount = Math.random() < 0.6 ? 1 : 2;

    // First obstacle
    const w1 = 14 + Math.floor(Math.random() * 16);
    const h1 = 28 + Math.floor(Math.random() * 20);
    addObstacleAbs(baseX, w1, h1);

    if (groupCount === 2) {
      // Ensure a clear minimum gap after the first's right edge
      const minInnerGap = 90 + Math.floor(Math.random() * 60); // 90-150px
      const secondX = baseX + w1 + minInnerGap;
      const w2 = 14 + Math.floor(Math.random() * 16);
      const h2 = 28 + Math.floor(Math.random() * 20);
      addObstacleAbs(secondX, w2, h2);
    }

    const baseCooldown = 1600 + Math.floor(Math.random() * 1200); // more spacing between groups
    const speedFactor = 6 / Math.max(2, speed);
    spawnCooldownMs = Math.floor(baseCooldown * speedFactor);
  }

  function spawnCloud() {
    const y = 20 + Math.random() * 60;
    const w = 30 + Math.random() * 40;
    clouds.push({ x: canvas.width + 20, y, w, h: 14 + Math.random() * 8, vx: 1.5 + Math.random() * 1.5 });
  }

  function collides(a, b) {
    return !(a.x + a.w < b.x || a.x > b.x + b.w || a.y + a.h < b.y || a.y > b.y + b.h);
  }

  function drawGround() {
    ctx.strokeStyle = '#64748b';
    ctx.beginPath();
    ctx.moveTo(0, GROUND_Y + 0.5);
    ctx.lineTo(canvas.width, GROUND_Y + 0.5);
    ctx.stroke();
  }

  function drawDino() {
    ctx.fillStyle = '#e2e8f0';
    ctx.fillRect(dino.x, dino.y, dino.w, dino.h);
    // eye
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(dino.x + dino.w - 10, dino.y + 8, 4, 4);
  }

  function drawObstacle(o) {
    ctx.fillStyle = '#94a3b8';
    ctx.fillRect(o.x, o.y, o.w, o.h);
  }

  function drawCloud(c) {
    ctx.fillStyle = '#475569';
    ctx.fillRect(c.x, c.y, c.w, c.h);
  }

  function drawScore() {
    ctx.fillStyle = '#94a3b8';
    ctx.font = '14px sans-serif';
    ctx.fillText(`Score: ${score}`, 8, 16);
    if (best > 0) ctx.fillText(`Best: ${best}`, 8, 32);
  }

  function update(dt) {
    frame++;
    // Dino physics with variable jump
    const inAir = !dino.onGround;
    if (inAir) {
      airTimeMs += dt;
    } else {
      airTimeMs = 0;
    }
    const g = (jumpKeyHeld && inAir && airTimeMs < maxAirMs) ? lowGravity : gravity;
    dino.vy += g;
    dino.y += dino.vy;
    if (dino.y >= GROUND_Y - dino.h) {
      dino.y = GROUND_Y - dino.h;
      dino.vy = 0;
      dino.onGround = true;
      jumpsAvailable = 2;
      airTimeMs = 0;
    }
    if (inAir && airTimeMs >= maxAirMs) jumpKeyHeld = false;
    // Obstacles
    spawnCooldownMs -= dt;
    if (spawnCooldownMs <= 0) {
      spawnGroup();
    }
    obstacles.forEach(o => o.x -= speed);
    obstacles = obstacles.filter(o => o.x + o.w > -10);
    // Clouds
    if (frame % 120 === 0) spawnCloud();
    clouds.forEach(c => c.x -= c.vx);
    clouds = clouds.filter(c => c.x + c.w > -10);
    // Difficulty and score
    // Increase speed gently but cap relative to base
    if (frame % 240 === 0 && speed < baseSpeed * 1.8) speed += 0.2;
    if (frame % 5 === 0) score++;
  }

  function render() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    clouds.forEach(drawCloud);
    drawGround();
    obstacles.forEach(drawObstacle);
    drawDino();
    drawScore();
  }

  function step(ts) {
    if (!running) return;
    if (!lastTs) lastTs = ts;
    const dt = Math.max(8, Math.min(34, ts - lastTs)); // clamp 8-34ms
    lastTs = ts;
    update(dt);
    // Check collisions
    const dbox = { x: dino.x + 4, y: dino.y + 4, w: dino.w - 8, h: dino.h - 8 };
    for (let o of obstacles) {
      if (collides(dbox, o)) {
        running = false;
        best = Math.max(best, score);
        const scoreEl = document.getElementById('dinoScore');
        if (scoreEl) scoreEl.textContent = `Score: ${score} (Game Over)`;
        render();
        return;
      }
    }
    render();
    const scoreEl = document.getElementById('dinoScore');
    if (scoreEl) scoreEl.textContent = `Score: ${score}`;
    requestAnimationFrame(step);
  }

  function jump() {
    if (jumpsAvailable <= 0) return;
    dino.vy = jumpVelocity;
    dino.onGround = false;
    airTimeMs = 0;
    jumpsAvailable--;
  }

  function start() {
    if (running) return;
    reset();
    running = true;
    requestAnimationFrame(step);
  }

  function restart() {
    reset();
    render();
    const scoreEl = document.getElementById('dinoScore');
    if (scoreEl) scoreEl.textContent = `Score: ${score}`;
  }

  // Controls
  document.getElementById('dinoStart')?.addEventListener('click', start);
  document.getElementById('dinoRestart')?.addEventListener('click', restart);
  window.addEventListener('keydown', (e) => {
    if (e.code === 'Space' || e.code === 'ArrowUp') {
      if (!e.repeat) {
        // allow double jump before landing
        jump();
      }
      jumpKeyHeld = true;
      e.preventDefault();
    }
  });
  window.addEventListener('keyup', (e) => {
    if (e.code === 'Space' || e.code === 'ArrowUp') {
      jumpKeyHeld = false;
    }
  });

  // Initial paint
  render();
})();


