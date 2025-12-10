document.addEventListener('DOMContentLoaded', () => {
	const tabButtons = Array.from(document.querySelectorAll('button[data-target]'));
	const sections = new Map();

	document.querySelectorAll('[data-section]').forEach((el) => {
		sections.set(el.getAttribute('data-section'), el);
	});

	// (Application tab is currently empty)

	function activate(target) {
		// Toggle button styles and aria-selected
		tabButtons.forEach((btn) => {
			const isActive = btn.getAttribute('data-target') === target;
			btn.setAttribute('aria-selected', String(isActive));
			if (isActive) {
				btn.classList.remove('btn-ghost');
				btn.classList.add('btn-primary');
			} else {
				btn.classList.remove('btn-primary');
				btn.classList.add('btn-ghost');
			}
		});

		// Toggle sections visibility
		sections.forEach((sectionEl, key) => {
			if (key === target) {
				sectionEl.classList.remove('hidden');
			} else {
				sectionEl.classList.add('hidden');
			}
		});
	}

	// Click handlers
	tabButtons.forEach((btn) => {
		btn.addEventListener('click', () => {
			const target = btn.getAttribute('data-target');
			if (target) {
				// Update URL hash so tab persists on reload/redirect
				if (location.hash !== `#${target}`) {
					history.replaceState(null, '', `#${target}`);
				}
				activate(target);
			}
		});
	});

	// Initial activation: use hash if available, else default to the first tab
	const initialHash = (location.hash || '').replace('#', '');
	const initialModel = (window.SC_SETTINGS_TAB || '').toString();
	const initial = initialHash || initialModel || (tabButtons[0] && tabButtons[0].getAttribute('data-target')) || 'application';
	if (initial) {
		activate(initial);
	}
});


