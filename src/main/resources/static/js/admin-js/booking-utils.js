
function showToast(m, t) {
    const c = document.getElementById('toastContainer');
    if (!c) return;
    const icons = {
        success: 'fa-check-circle',
        error: 'fa-times-circle',
        warning: 'fa-exclamation-triangle'
    };
    const toast = document.createElement('div');
    toast.className = 'toast ' + t;
    toast.innerHTML = '<i class="fas ' + icons[t] + '"></i> ' + m;
    c.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity .3s';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ===== PAGINATION =====
const ROWS = 10;
let cp = 1,
    tr = 0;

function iP() {
    const r = document.querySelectorAll('#tableBody tr[data-detail-id]');
    tr = r.length;
    document.getElementById('totalPages').textContent = Math.ceil(tr / ROWS) || 1;
    sP(1);
}

function sP(p) {
    const r = document.querySelectorAll('#tableBody tr[data-detail-id]');
    const tp = Math.ceil(r.length / ROWS) || 1;
    if (p < 1) p = 1;
    if (p > tp) p = tp;
    cp = p;
    r.forEach((row, i) => {
        row.style.display = (i >= (p - 1) * ROWS && i < p * ROWS) ? '' : 'none';
    });
    document.getElementById('currentPage').textContent = cp;
}

function goToPage(a) {
    const tp = Math.ceil(tr / ROWS) || 1;
    if (a === 'first') sP(1);
    else if (a === 'prev') sP(cp - 1);
    else if (a === 'next') sP(cp + 1);
    else if (a === 'last') sP(tp);
}