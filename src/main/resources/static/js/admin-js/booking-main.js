// ========================================
// booking-main.js - File chính
// ========================================

// ===== INITIALIZE =====
document.addEventListener("DOMContentLoaded", function() {
    // 1. Khởi tạo phân trang
    initPagination();

    // 2. Khởi tạo biểu đồ
    if (typeof forecastDays !== 'undefined' && typeof forecastData !== 'undefined') {
        initChart(forecastDays, forecastData);
    }

    // 3. Event listeners cho các popup
    document.getElementById('walkInModal')?.addEventListener('click', function(e) {
        if (e.target === this) closeWalkInModal();
    });

    document.getElementById('foodPopup')?.addEventListener('click', function(e) {
        if (e.target === this) closeFoodPopup();
    });

    document.getElementById('pickRoomPopup')?.addEventListener('click', function(e) {
        if (e.target === this) closePickRoomPopup();
    });

    document.getElementById('editPopup')?.addEventListener('click', function(e) {
        if (e.target === this) closeEditPopup();
    });

    document.getElementById('editBookingPopup')?.addEventListener('click', function(e) {
        if (e.target === this) closeEditBookingPopup();
    });

    document.getElementById('walkInQrModal')?.addEventListener('click', function(e) {
        if (e.target === this) cancelWalkInQrModal();
    });
});

// ===== SHOW TOAST =====
function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    if (!container) {
        console.log(message);
        return;
    }

    const icons = {
        success: 'fa-check-circle',
        error: 'fa-times-circle',
        warning: 'fa-exclamation-triangle'
    };

    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.innerHTML = '<i class="fas ' + (icons[type] || icons.success) + '"></i> ' + message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity .3s';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ===== PAGINATION =====
const ROWS_PER_PAGE = 10;
let currentPage = 1;
let totalRows = 0;

function initPagination() {
    const rows = document.querySelectorAll('#tableBody tr[data-detail-id]');
    totalRows = rows.length;
    const totalPages = Math.ceil(totalRows / ROWS_PER_PAGE) || 1;
    document.getElementById('totalPages').textContent = totalPages;
    showPage(1);
}

function showPage(page) {
    const rows = document.querySelectorAll('#tableBody tr[data-detail-id]');
    const totalPages = Math.ceil(rows.length / ROWS_PER_PAGE) || 1;

    if (page < 1) page = 1;
    if (page > totalPages) page = totalPages;

    currentPage = page;

    rows.forEach((row, index) => {
        row.style.display = (index >= (page - 1) * ROWS_PER_PAGE && index < page * ROWS_PER_PAGE) ? '' : 'none';
    });

    document.getElementById('currentPage').textContent = currentPage;
}

function goToPage(action) {
    const totalPages = Math.ceil(totalRows / ROWS_PER_PAGE) || 1;

    if (action === 'first') showPage(1);
    else if (action === 'prev') showPage(currentPage - 1);
    else if (action === 'next') showPage(currentPage + 1);
    else if (action === 'last') showPage(totalPages);
}