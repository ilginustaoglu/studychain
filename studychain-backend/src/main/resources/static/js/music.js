document.addEventListener('DOMContentLoaded', function () {
    const player = document.getElementById('musicPlayer');
    if (!player) return;
    const items = document.querySelectorAll('.music-item');
    const tabs = document.querySelectorAll('.music-tab');
    const listItems = document.querySelectorAll('.music-list li');
    let currentCategory = 'MUSIC';

    items.forEach(btn => {
        btn.addEventListener('click', () => {
            const src = btn.getAttribute('data-src');
            if (!src) return;
            if (player.src !== location.origin + src && player.src !== src) {
                player.src = src;
            }
            player.play().catch(() => {/* ignore */});
        });
    });

    function applyFilter() {
        listItems.forEach(li => {
            const btn = li.querySelector('.music-item');
            if (!btn) return;
            const cat = btn.getAttribute('data-category') || 'MUSIC';
            li.style.display = (cat === currentCategory) ? '' : 'none';
        });
    }

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentCategory = tab.getAttribute('data-tab') || 'MUSIC';
            applyFilter();
        });
    });

    applyFilter();
});


