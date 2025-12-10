document.addEventListener('DOMContentLoaded', () => {
	const canvas = document.getElementById('studyChart');
	if (!canvas) return;
	const ctx = canvas.getContext('2d');

	function render(statsObj, unitLabel = 'Minutes') {
		const entries = Object.entries(statsObj || {});
		const width = canvas.width;
		const height = canvas.height;
		const padding = { top: 24, right: 28, bottom: 42, left: 56 };
		const innerWidth = width - padding.left - padding.right;
		const innerHeight = height - padding.top - padding.bottom;
		const barGap = 10;
		const n = entries.length;
		const barWidth = (innerWidth - barGap * Math.max(0, n - 1)) / Math.max(1, n);
		const rawMax = Math.max(1, ...entries.map(([, v]) => Number(v) || 0));
		// nice max (1,2,5 x 10^k)
		const niceMax = (() => {
			const pow10 = Math.pow(10, Math.floor(Math.log10(rawMax)));
			const m = rawMax / pow10;
			let base;
			if (m <= 1) base = 1; else if (m <= 2) base = 2; else if (m <= 5) base = 5; else base = 10;
			return base * pow10;
		})();
		// clear
		ctx.clearRect(0, 0, width, height);
		if (entries.length === 0) {
			ctx.fillStyle = '#94a3b8';
			ctx.font = '12px ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Arial';
			ctx.fillText('No data', 10, 20);
			return;
		}
		// grid + axes
		ctx.strokeStyle = '#1f2937';
		ctx.lineWidth = 1;
		// y grid lines
		ctx.font = '11px ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Arial';
		ctx.fillStyle = '#94a3b8';
		for (let i = 0; i <= 4; i++) {
			const t = i / 4;
			const y = padding.top + innerHeight - t * innerHeight;
			// grid
			ctx.strokeStyle = i === 0 ? '#1f2937' : '#111827';
			ctx.beginPath();
			ctx.moveTo(padding.left, y);
			ctx.lineTo(width - padding.right, y);
			ctx.stroke();
			// labels
			const val = Math.round((t * niceMax));
			const label = String(val);
			ctx.fillText(label, 8, y - 2);
		}
		// axes
		ctx.strokeStyle = '#1f2937';
		ctx.beginPath();
		ctx.moveTo(padding.left, padding.top);
		ctx.lineTo(padding.left, height - padding.bottom);
		ctx.lineTo(width - padding.right, height - padding.bottom);
		ctx.stroke();
		// axis titles
		ctx.save();
		ctx.fillStyle = '#94a3b8';
		ctx.font = '12px ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Arial';
		// Y title
		ctx.translate(16, padding.top + innerHeight / 2);
		ctx.rotate(-Math.PI / 2);
		ctx.fillText(unitLabel, 0, 0);
		ctx.restore();
		// X title
		ctx.fillText('Date', width - padding.right - 36, height - 8);

		// bars
		entries.forEach(([label, val], i) => {
			const x = padding.left + i * (barWidth + barGap);
			const h = ((Number(val) || 0) / niceMax) * innerHeight;
			const y = padding.top + (innerHeight - h);
			ctx.fillStyle = '#6366f1';
			ctx.fillRect(x, y, barWidth, h);
			// x labels
			ctx.fillStyle = '#94a3b8';
			const short = label.slice(5); // MM-DD
			const textX = x;
			const textY = height - padding.bottom + 14;
			ctx.fillText(short, textX, textY);
			// value labels
			if (h > 14) {
				ctx.fillStyle = '#e5e7eb';
				ctx.font = '10px ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Arial';
				const valStr = String(Number(val) || 0);
				ctx.fillText(valStr, x + 2, y - 2);
			}
		});
	}

	// initial (week only)
	render(window.SC_STUDY_STATS_WEEK || {}, 'Minutes');
});


