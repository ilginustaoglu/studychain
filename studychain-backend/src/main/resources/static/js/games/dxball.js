(() => {
    const canvas = document.getElementById('dxballCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const W = canvas.width;
    const H = canvas.height;

    // Game state
    let running = false;
    let rafId = 0;
    let score = 0;
    let lives = 3;
    let level = 1;
    let coins = 0;

    const paddle = {
        w: 96,
        h: 12,
        x: (W - 96) / 2,
        y: H - 24,
        speed: 7,
        moveLeft: false,
        moveRight: false,
    };

    const ball = {
        r: 7,
        x: W / 2,
        y: H - 40,
        vx: 1.2,
        vy: -1.2,
        stuck: true, // stuck to paddle until launch
    };

    const brick = {
        rows: 5,
        cols: 10,
        w: 56,
        h: 18,
        pad: 6,
        top: 32,
        left: 20,
    };

    const bricks = [];
    const powerUps = [];
    function generateBricksLayout(lv) {
        const rows = Math.min(5 + Math.floor((lv - 1) / 2), 8);
        const cols = brick.cols;
        const layout = [];
        for (let r = 0; r < rows; r++) {
            layout[r] = [];
            for (let c = 0; c < cols; c++) {
                let alive = true;
                const mode = lv % 3;
                if (mode === 1) {
                    alive = ((r + c) % 2 === 0); // checkerboard
                } else if (mode === 2) {
                    alive = (r === 0 || c === 0 || r === rows - 1 || c === cols - 1); // hollow
                } // else full
                layout[r][c] = { alive, hits: 1 };
            }
        }
        return layout;
    }
    function resetBricks() {
        const layout = generateBricksLayout(level);
        bricks.length = 0;
        for (let r = 0; r < layout.length; r++) {
            bricks[r] = [];
            for (let c = 0; c < layout[r].length; c++) {
                bricks[r][c] = layout[r][c];
            }
        }
        brick.rows = layout.length;
    }

    function resetBall() {
        ball.x = paddle.x + paddle.w / 2;
        ball.y = paddle.y - ball.r - 1;
        const base = 1.2;
        ball.vx = base * (Math.random() > 0.5 ? 1 : -1);
        ball.vy = -base;
        ball.stuck = true;
    }

    function resetGame(full = false) {
        if (full) {
            score = 0;
            lives = 3;
            level = 1;
            coins = 0;
            resetBricks();
        }
        paddle.x = (W - paddle.w) / 2;
        resetBall();
        updateUI();
    }

    function updateUI() {
        const s = document.getElementById('dxballScore');
        const l = document.getElementById('dxballLives');
        const lv = document.getElementById('dxballLevel');
        const c = document.getElementById('dxballCoins');
        if (s) s.textContent = `Score: ${score}`;
        if (l) l.textContent = `Lives: ${lives}`;
        if (lv) lv.textContent = `Level: ${level}`;
        if (c) c.textContent = `Coins: ${coins}`;
    }

    function drawBackground() {
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--surface') || '#111';
        ctx.fillRect(0, 0, W, H);
    }

    function drawPaddle() {
        ctx.fillStyle = '#90CAF9';
        ctx.fillRect(paddle.x, paddle.y, paddle.w, paddle.h);
    }

    function drawBall() {
        ctx.fillStyle = '#FF7043';
        ctx.beginPath();
        ctx.arc(ball.x, ball.y, ball.r, 0, Math.PI * 2);
        ctx.fill();
    }

    function drawBricks() {
        for (let r = 0; r < brick.rows; r++) {
            for (let c = 0; c < brick.cols; c++) {
                if (!bricks[r] || !bricks[r][c] || !bricks[r][c].alive) continue;
                const x = brick.left + c * (brick.w + brick.pad);
                const y = brick.top + r * (brick.h + brick.pad);
                ctx.fillStyle = ['#CE93D8', '#81C784', '#FFB74D', '#64B5F6', '#E57373'][r % 5];
                ctx.fillRect(x, y, brick.w, brick.h);
            }
        }
    }

    function drawPowerUps() {
        for (const p of powerUps) {
            if (!p.active) continue;
            ctx.fillStyle = p.type === 'COIN' ? '#FFD54F' : (p.type === 'SHIELD' ? '#4DB6AC' : '#BA68C8');
            ctx.fillRect(p.x - 8, p.y - 8, 16, 16);
        }
    }

    let shieldUntil = 0;
    let lastSpeedIncrease = performance.now();

    function step() {
        if (!running) return;

        // Move paddle
        if (paddle.moveLeft) paddle.x -= paddle.speed;
        if (paddle.moveRight) paddle.x += paddle.speed;
        paddle.x = Math.max(0, Math.min(W - paddle.w, paddle.x));

        // Move ball
        if (ball.stuck) {
            ball.x = paddle.x + paddle.w / 2;
            ball.y = paddle.y - ball.r - 1;
        } else {
            ball.x += ball.vx;
            ball.y += ball.vy;
        }

        // time-based speed up
        const now = performance.now();
        if (now - lastSpeedIncrease > 10000) {
            lastSpeedIncrease = now;
            ball.vx *= 1.06;
            ball.vy *= 1.06;
        }

        // Wall collisions
        if (ball.x - ball.r < 0) { ball.x = ball.r; ball.vx *= -1; }
        if (ball.x + ball.r > W) { ball.x = W - ball.r; ball.vx *= -1; }
        if (ball.y - ball.r < 0) { ball.y = ball.r; ball.vy *= -1; }

        // Paddle collision
        if (ball.y + ball.r >= paddle.y &&
            ball.x >= paddle.x && ball.x <= paddle.x + paddle.w &&
            ball.vy > 0) {
            // Place ball just above paddle
            ball.y = paddle.y - ball.r - 1;
            // Reflect velocity vector across collision normal from nearest point on paddle
            const nearestX = Math.max(paddle.x, Math.min(ball.x, paddle.x + paddle.w));
            const nearestY = Math.max(paddle.y, Math.min(ball.y, paddle.y + paddle.h));
            let nx = ball.x - nearestX;
            let ny = ball.y - nearestY;
            const len = Math.hypot(nx, ny) || 1;
            nx /= len; ny /= len;
            // v' = v - 2 (v·n) n
            const dot = ball.vx * nx + ball.vy * ny;
            ball.vx = ball.vx - 2 * dot * nx;
            ball.vy = ball.vy - 2 * dot * ny;
            // Ensure minimum upward component to avoid sticking
            if (ball.vy > -0.6) ball.vy = -0.6;
        }

        // Brick collisions
        outer:
        for (let r = 0; r < brick.rows; r++) {
            for (let c = 0; c < brick.cols; c++) {
                if (!bricks[r] || !bricks[r][c]) continue;
                const b = bricks[r][c];
                if (!b.alive) continue;
                const x = brick.left + c * (brick.w + brick.pad);
                const y = brick.top + r * (brick.h + brick.pad);
                if (ball.x + ball.r > x && ball.x - ball.r < x + brick.w &&
                    ball.y + ball.r > y && ball.y - ball.r < y + brick.h) {
                    // hit
                    b.alive = false;
                    score += 10;
                    updateUI();
                    // chance to drop power-up
                    if (Math.random() < 0.15) {
                        const types = ['COIN','SHIELD','MULTI'];
                        const type = types[(Math.random() * types.length) | 0];
                        powerUps.push({ x: x + brick.w / 2, y: y + brick.h / 2, vy: 1.2, type, active: true });
                    }
                    // reflect on the side we hit more
                    const overlapX = Math.min(ball.x + ball.r - x, x + brick.w - (ball.x - ball.r));
                    const overlapY = Math.min(ball.y + ball.r - y, y + brick.h - (ball.y - ball.r));
                    if (overlapX < overlapY) ball.vx *= -1;
                    else ball.vy *= -1;
                    break outer;
                }
            }
        }

        // Missed ball
        if (ball.y - ball.r > H) {
            if (performance.now() < shieldUntil) {
                ball.y = H - ball.r - 1;
                ball.vy = -Math.abs(ball.vy);
            } else {
                lives -= 1;
                updateUI();
                if (lives <= 0) {
                    running = false;
                    cancelAnimationFrame(rafId);
                } else {
                    resetBall();
                }
            }
        }

        // Power-ups falling
        for (const p of powerUps) {
            if (!p.active) continue;
            p.y += p.vy;
            // collected by paddle?
            if (p.y >= paddle.y && p.x >= paddle.x && p.x <= paddle.x + paddle.w) {
                p.active = false;
                if (p.type === 'COIN') {
                    coins += 1;
                } else if (p.type === 'SHIELD') {
                    shieldUntil = performance.now() + 8000;
                } else if (p.type === 'MULTI') {
                    // simple multi-ball effect: mirror and increase current velocity a bit
                    ball.vx *= -1.05;
                    ball.vy *= 1.05;
                    score += 5;
                }
                updateUI();
            }
        }

        // Win condition
        let anyAlive = false;
        for (let r = 0; r < brick.rows; r++) {
            for (let c = 0; c < brick.cols; c++) {
                if (bricks[r] && bricks[r][c] && bricks[r][c].alive) { anyAlive = true; break; }
            }
            if (anyAlive) break;
        }
        if (!anyAlive) {
            level += 1;
            // speed bump per level
            ball.vx *= 1.1;
            ball.vy *= 1.1;
            resetBricks();
            resetBall();
            updateUI();
        }

        // Draw
        drawBackground();
        drawBricks();
        // Shield hint
        if (performance.now() < shieldUntil) {
            ctx.fillStyle = 'rgba(77,182,172,0.25)';
            ctx.fillRect(0, H - 6, W, 6);
        }
        drawPowerUps();
        drawPaddle();
        drawBall();

        rafId = requestAnimationFrame(step);
    }

    // Controls
    function keydown(e) {
        if (e.code === 'ArrowLeft') paddle.moveLeft = true;
        if (e.code === 'ArrowRight') paddle.moveRight = true;
        if (e.code === 'Space') {
            if (ball.stuck) ball.stuck = false;
        }
    }
    function keyup(e) {
        if (e.code === 'ArrowLeft') paddle.moveLeft = false;
        if (e.code === 'ArrowRight') paddle.moveRight = false;
    }
    window.addEventListener('keydown', keydown);
    window.addEventListener('keyup', keyup);

    // Mouse move control
    canvas.addEventListener('mousemove', (e) => {
        const rect = canvas.getBoundingClientRect();
        const mx = e.clientX - rect.left;
        paddle.x = Math.max(0, Math.min(W - paddle.w, mx - paddle.w / 2));
    });

    const btnRestart = document.getElementById('dxballRestart');
    btnRestart && btnRestart.addEventListener('click', () => {
        resetGame(true);
        if (!running) {
            running = true;
            rafId = requestAnimationFrame(step);
        }
    });

    // Init
    resetGame(true);
    drawBackground();
    drawBricks();
    drawPaddle();
    drawBall();
})(); 


