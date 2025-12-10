(() => {
    const canvas = document.getElementById('tetrisCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    const BLOCK_SIZE = 24;
    const COLS = 10;
    const ROWS = 20;
    const WIDTH = COLS * BLOCK_SIZE;
    const HEIGHT = ROWS * BLOCK_SIZE;
    canvas.width = WIDTH;
    canvas.height = HEIGHT;

    const COLORS = [
        null,
        '#00BCD4', // I
        '#3F51B5', // J
        '#FF9800', // L
        '#FFC107', // O
        '#4CAF50', // S
        '#9C27B0', // T
        '#F44336', // Z
    ];

    const SHAPES = {
        I: [
            [1, 1, 1, 1],
        ],
        J: [
            [2, 0, 0],
            [2, 2, 2],
        ],
        L: [
            [0, 0, 3],
            [3, 3, 3],
        ],
        O: [
            [4, 4],
            [4, 4],
        ],
        S: [
            [0, 5, 5],
            [5, 5, 0],
        ],
        T: [
            [0, 6, 0],
            [6, 6, 6],
        ],
        Z: [
            [7, 7, 0],
            [0, 7, 7],
        ],
    };
    const SHAPE_KEYS = Object.keys(SHAPES);

    function createMatrix(w, h) {
        const m = [];
        while (h--) m.push(new Array(w).fill(0));
        return m;
    }

    function rotate(matrix) {
        // Rotate 90° clockwise: transpose then reverse each row
        const transposed = matrix[0].map((_, x) => matrix.map(row => row[x]));
        return transposed.map(row => row.slice().reverse());
    }

    function merge(board, piece) {
        piece.matrix.forEach((row, y) => {
            row.forEach((val, x) => {
                if (val !== 0) {
                    board[y + piece.y][x + piece.x] = val;
                }
            });
        });
    }

    function collide(board, piece) {
        for (let y = 0; y < piece.matrix.length; y++) {
            for (let x = 0; x < piece.matrix[y].length; x++) {
                const val = piece.matrix[y][x];
                if (val !== 0) {
                    const by = y + piece.y;
                    const bx = x + piece.x;
                    if (bx < 0 || bx >= COLS) return true;
                    if (by >= ROWS) return true;
                    if (by < 0) continue; // allow piece to be above the board
                    if (board[by][bx] !== 0) return true;
                }
            }
        }
        return false;
    }

    function drawCell(x, y, colorIndex) {
        if (colorIndex === 0) return;
        ctx.fillStyle = COLORS[colorIndex];
        ctx.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        ctx.strokeStyle = 'rgba(0,0,0,0.2)';
        ctx.strokeRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
    }

    function drawBoard() {
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--surface') || '#111';
        ctx.fillRect(0, 0, WIDTH, HEIGHT);
        // grid
        ctx.strokeStyle = 'rgba(255,255,255,0.05)';
        for (let x = 0; x <= COLS; x++) {
            ctx.beginPath();
            ctx.moveTo(x * BLOCK_SIZE + 0.5, 0);
            ctx.lineTo(x * BLOCK_SIZE + 0.5, HEIGHT);
            ctx.stroke();
        }
        for (let y = 0; y <= ROWS; y++) {
            ctx.beginPath();
            ctx.moveTo(0, y * BLOCK_SIZE + 0.5);
            ctx.lineTo(WIDTH, y * BLOCK_SIZE + 0.5);
            ctx.stroke();
        }
        // board cells
        for (let y = 0; y < ROWS; y++) {
            for (let x = 0; x < COLS; x++) {
                drawCell(x, y, board[y][x]);
            }
        }
        // current piece
        piece.matrix.forEach((row, dy) => {
            row.forEach((val, dx) => {
                if (val !== 0) {
                    drawCell(piece.x + dx, piece.y + dy, val);
                }
            });
        });
    }

    function sweep() {
        let lines = 0;
        for (let y = ROWS - 1; y >= 0; y--) {
            if (board[y].every(v => v !== 0)) {
                const row = board.splice(y, 1)[0].fill(0);
                board.unshift(row);
                y++;
                lines++;
            }
        }
        if (lines > 0) {
            score += [0, 100, 300, 500, 800][lines] || lines * 200;
            linesCleared += lines;
            updateUI();
        }
    }

    function spawnPiece() {
        const key = SHAPE_KEYS[(Math.random() * SHAPE_KEYS.length) | 0];
        const shape = SHAPES[key].map(r => r.slice());
        const mino = { matrix: shape, x: 0, y: 0 };
        mino.x = ((COLS / 2) | 0) - ((shape[0].length / 2) | 0);
        mino.y = 0; // start at top row
        piece = mino;
        if (collide(board, piece)) {
            // game over
            running = false;
            cancelAnimationFrame(rafId);
        }
    }

    function drop() {
        piece.y++;
        if (collide(board, piece)) {
            piece.y--;
            merge(board, piece);
            sweep();
            spawnPiece();
        }
        dropCounter = 0;
    }

    function update(time = 0) {
        if (!running) return;
        const delta = time - lastTime;
        lastTime = time;
        dropCounter += delta;
        if (dropCounter > dropInterval) {
            drop();
        }
        drawBoard();
        rafId = requestAnimationFrame(update);
    }

    function move(dir) {
        piece.x += dir;
        if (collide(board, piece)) {
            piece.x -= dir;
        }
    }

    function hardDrop() {
        while (!collide(board, piece)) {
            piece.y++;
        }
        piece.y--;
        merge(board, piece);
        sweep();
        spawnPiece();
        drawBoard();
    }

    function rotatePiece() {
        const rotated = rotate(piece.matrix);
        const prevMatrix = piece.matrix;
        const prevX = piece.x;
        const prevY = piece.y;
        // If rotation would place blocks above the visible board, prefer downward kicks
        let needsDownKick = false;
        for (let y = 0; y < rotated.length; y++) {
            for (let x = 0; x < rotated[y].length; x++) {
                if (rotated[y][x] !== 0 && (piece.y + y) < 0) {
                    needsDownKick = true;
                    break;
                }
            }
            if (needsDownKick) break;
        }
        const kicks = [
            { x: 0, y: 0 },
            { x: 1, y: 0 },
            { x: -1, y: 0 },
            { x: 2, y: 0 },
            { x: -2, y: 0 },
        ];
        if (needsDownKick) {
            kicks.push({ x: 0, y: 1 }, { x: 0, y: 2 });
        }
        for (const k of kicks) {
            piece.matrix = rotated;
            piece.x = prevX + k.x;
            piece.y = prevY + k.y;
            if (!collide(board, piece)) {
                return; // rotation with kick applied
            }
        }
        // Revert if all kicks fail
        piece.matrix = prevMatrix;
        piece.x = prevX;
        piece.y = prevY;
    }

    function updateUI() {
        const sEl = document.getElementById('tetrisScore');
        const lEl = document.getElementById('tetrisLines');
        if (sEl) sEl.textContent = `Score: ${score}`;
        if (lEl) lEl.textContent = `Lines: ${linesCleared}`;
    }

    function reset() {
        for (let y = 0; y < ROWS; y++) board[y].fill(0);
        score = 0;
        linesCleared = 0;
        updateUI();
        spawnPiece();
        lastTime = 0;
        dropCounter = 0;
    }

    function clearFieldOnly() {
        // Clear placed blocks without starting the game or spawning a piece
        for (let y = 0; y < ROWS; y++) board[y].fill(0);
        piece = { matrix: [[0]], x: 0, y: 0 };
        running = false;
        dropCounter = 0;
        lastTime = 0;
        drawBoard();
    }

    // State
    const board = createMatrix(COLS, ROWS);
    let piece = { matrix: [[0]], x: 0, y: 0 };
    let lastTime = 0;
    let dropCounter = 0;
    let dropInterval = 700; // ms
    let running = false;
    let rafId = 0;
    let score = 0;
    let linesCleared = 0;

    // Controls
    function onKey(e) {
        const handled = ['ArrowLeft','ArrowRight','ArrowDown','ArrowUp','Space'];
        if (handled.includes(e.code)) e.preventDefault();
        if (!running) return;
        if (e.code === 'ArrowLeft') move(-1);
        else if (e.code === 'ArrowRight') move(1);
        else if (e.code === 'ArrowDown') drop();
        else if (e.code === 'ArrowUp') rotatePiece();
        else if (e.code === 'Space') hardDrop();
    }
    window.addEventListener('keydown', onKey);

    const btnStart = document.getElementById('tetrisStart');
    const btnRestart = document.getElementById('tetrisRestart');
    btnStart && btnStart.addEventListener('click', () => {
        if (running) return;
        reset();
        running = true;
        cancelAnimationFrame(rafId);
        rafId = requestAnimationFrame(update);
    });
    btnRestart && btnRestart.addEventListener('click', () => {
        // Only clear the field; do not start the game loop
        clearFieldOnly();
    });

    // Initial draw
    drawBoard();
})(); 


