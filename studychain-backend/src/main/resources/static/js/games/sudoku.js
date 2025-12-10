(() => {
  // Utilities
  const range = n => Array.from({ length: n }, (_, i) => i);
  const shuffle = arr => {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  };

  // Generate a valid solved 9x9 grid using pattern method then shuffle rows/cols within bands/stacks
  function generateSolved() {
    const pattern = (r, c) => (r * 3 + Math.floor(r / 3) + c) % 9;
    const base = range(9).map(r => range(9).map(c => pattern(r, c) + 1));
    // shuffle rows within bands
    const rowBands = [0, 3, 6].map(b => shuffle([b, b + 1, b + 2]));
    const rowsOrder = rowBands.flat();
    // shuffle cols within stacks
    const colStacks = [0, 3, 6].map(s => shuffle([s, s + 1, s + 2]));
    const colsOrder = colStacks.flat();
    // shuffle bands and stacks themselves
    const bandOrder = shuffle([0, 1, 2]).flatMap(b => [b * 3, b * 3 + 1, b * 3 + 2]);
    const stackOrder = shuffle([0, 1, 2]).flatMap(s => [s * 3, s * 3 + 1, s * 3 + 2]);
    const finalRows = bandOrder.map(i => rowsOrder[i]);
    const finalCols = stackOrder.map(i => colsOrder[i]);
    return finalRows.map(rr => finalCols.map(cc => base[rr][cc]));
  }

  function cloneGrid(grid) {
    return grid.map(row => row.slice());
  }

  function findEmpty(grid) {
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        if (grid[r][c] === 0) return [r, c];
      }
    }
    return null;
  }

  function isSafe(grid, r, c, n) {
    for (let i = 0; i < 9; i++) {
      if (grid[r][i] === n || grid[i][c] === n) return false;
    }
    const br = Math.floor(r / 3) * 3, bc = Math.floor(c / 3) * 3;
    for (let i = 0; i < 3; i++) {
      for (let j = 0; j < 3; j++) {
        if (grid[br + i][bc + j] === n) return false;
      }
    }
    return true;
  }

  function solve(grid) {
    const pos = findEmpty(grid);
    if (!pos) return true;
    const [r, c] = pos;
    for (let n = 1; n <= 9; n++) {
      if (isSafe(grid, r, c, n)) {
        grid[r][c] = n;
        if (solve(grid)) return true;
        grid[r][c] = 0;
      }
    }
    return false;
  }

  // Create puzzle by removing cells from a solved grid
  function makePuzzle(solved, clues = 36) {
    const puzzle = cloneGrid(solved);
    // remove 81 - clues cells
    const positions = shuffle(range(81));
    let toRemove = 81 - Math.max(17, Math.min(80, clues));
    for (let idx of positions) {
      if (toRemove <= 0) break;
      const r = Math.floor(idx / 9), c = idx % 9;
      if (puzzle[r][c] !== 0) {
        puzzle[r][c] = 0;
        toRemove--;
      }
    }
    return puzzle;
  }

  const gridEl = document.getElementById('sudokuGrid');
  if (!gridEl) return;

  const statusEl = document.getElementById('sudokuStatus');
  const newBtn = document.getElementById('sudokuNew');
  const solveBtn = document.getElementById('sudokuSolve');
  const checkBtn = document.getElementById('sudokuCheck');
  const clearBtn = document.getElementById('sudokuClear');
  const diffBtns = Array.from(document.querySelectorAll('.sudoku-diff'));

  let solution = generateSolved();
  // default: Medium = 38 clues
  let selectedClues = 38;
  let puzzle = makePuzzle(solution, selectedClues);
  let readOnlySet = new Set(); // indices (r*9+c) that are fixed

  function buildGrid() {
    gridEl.innerHTML = '';
    readOnlySet.clear();
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        const v = puzzle[r][c];
        const input = document.createElement('input');
        input.setAttribute('inputmode', 'numeric');
        input.setAttribute('maxlength', '1');
        input.className = 'sudoku-cell';
        // add helper classes for thick borders
        if (r === 2 || r === 5) input.classList.add('cell-r' + (r + 1));
        if (c === 2 || c === 5) input.classList.add('cell-c' + (c + 1));
        input.dataset.r = String(r);
        input.dataset.c = String(c);
        if (v !== 0) {
          input.value = String(v);
          input.readOnly = true;
          input.classList.add('readonly');
          readOnlySet.add(r * 9 + c);
        } else {
          input.value = '';
        }
        input.addEventListener('input', () => {
          // keep only digits 1-9
          const val = input.value.replace(/[^1-9]/g, '');
          input.value = val.slice(-1);
          if (input.value.length > 0) {
            const rr = parseInt(input.dataset.r, 10);
            const cc = parseInt(input.dataset.c, 10);
            puzzle[rr][cc] = parseInt(input.value, 10);
          }
        });
        input.addEventListener('keydown', (e) => {
          if (e.key === 'Backspace' || e.key === 'Delete') {
            const rr = parseInt(input.dataset.r, 10);
            const cc = parseInt(input.dataset.c, 10);
            if (!readOnlySet.has(rr * 9 + cc)) {
              input.value = '';
              puzzle[rr][cc] = 0;
            }
            e.preventDefault();
          }
        });
        gridEl.appendChild(input);
      }
    }
  }

  function refreshInputsFromPuzzle() {
    const inputs = gridEl.querySelectorAll('.sudoku-cell');
    inputs.forEach(inp => {
      const r = parseInt(inp.dataset.r, 10);
      const c = parseInt(inp.dataset.c, 10);
      const fixed = readOnlySet.has(r * 9 + c);
      inp.readOnly = fixed;
      if (!fixed) {
        inp.value = puzzle[r][c] === 0 ? '' : String(puzzle[r][c]);
      }
    });
  }

  function setStatus(msg, ok = true) {
    if (!statusEl) return;
    statusEl.textContent = msg;
    statusEl.style.color = ok ? 'var(--muted)' : 'var(--danger)';
  }

  function checkCurrent() {
    // Validate rows, cols, and blocks (ignoring zeros)
    const rowsOk = range(9).every(r => {
      const seen = new Set();
      for (let c = 0; c < 9; c++) {
        const v = puzzle[r][c];
        if (v === 0) continue;
        if (seen.has(v)) return false;
        seen.add(v);
      }
      return true;
    });
    if (!rowsOk) return false;
    const colsOk = range(9).every(c => {
      const seen = new Set();
      for (let r = 0; r < 9; r++) {
        const v = puzzle[r][c];
        if (v === 0) continue;
        if (seen.has(v)) return false;
        seen.add(v);
      }
      return true;
    });
    if (!colsOk) return false;
    const blocksOk = [0, 3, 6].every(br =>
      [0, 3, 6].every(bc => {
        const seen = new Set();
        for (let i = 0; i < 3; i++) {
          for (let j = 0; j < 3; j++) {
            const v = puzzle[br + i][bc + j];
            if (v === 0) continue;
            if (seen.has(v)) return false;
            seen.add(v);
          }
        }
        return true;
      })
    );
    return blocksOk;
  }

  function newPuzzle() {
    solution = generateSolved();
    puzzle = makePuzzle(solution, selectedClues);
    buildGrid();
    setStatus('New puzzle generated.');
  }

  function solvePuzzle() {
    const grid = cloneGrid(puzzle);
    const ok = solve(grid);
    if (ok) {
      puzzle = grid;
      refreshInputsFromPuzzle();
      setStatus('Solved!');
    } else {
      setStatus('No solution found.', false);
    }
  }

  function clearNonFixed() {
    for (let r = 0; r < 9; r++) for (let c = 0; c < 9; c++) {
      if (!readOnlySet.has(r * 9 + c)) puzzle[r][c] = 0;
    }
    refreshInputsFromPuzzle();
    setStatus('Cleared.');
  }

  // Event bindings
  if (newBtn) newBtn.addEventListener('click', newPuzzle);
  if (solveBtn) solveBtn.addEventListener('click', solvePuzzle);
  if (checkBtn) checkBtn.addEventListener('click', () => {
    const valid = checkCurrent();
    if (!valid) setStatus('There are conflicts.', false);
    else if (puzzle.every((row, r) => row.every((v, c) => v === solution[r][c]))) {
      setStatus('Correct! 🎉');
    } else {
      setStatus('Looks good so far.');
    }
  });
  if (clearBtn) clearBtn.addEventListener('click', clearNonFixed);

  // Difficulty selection handlers
  if (diffBtns.length > 0) {
    diffBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        diffBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const cluesAttr = btn.getAttribute('data-clues');
        const cluesValue = cluesAttr ? parseInt(cluesAttr, 10) : 38;
        // Clamp sanity range
        selectedClues = Math.max(17, Math.min(80, isNaN(cluesValue) ? 38 : cluesValue));
        newPuzzle();
      });
    });
  }

  // Initial render
  buildGrid();
})();


