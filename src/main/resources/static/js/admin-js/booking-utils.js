// ========================================
// booking-utils.js - Các hàm tiện ích
// ========================================

// ===== FORMAT TIỀN TỆ =====
function formatCurrency(amount) {
    return Number(amount || 0).toLocaleString('vi-VN') + ' VND';
}

// ===== FORMAT NGÀY =====
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

// ===== KIỂM TRA TRẠNG THÁI =====
function getStatusBadge(status) {
    const statusMap = {
        'PENDING': { text: 'CHỜ DUYỆT', class: 'pending' },
        'APPROVED': { text: 'ĐÃ DUYỆT', class: 'confirmed' },
        'CONFIRMED': { text: 'ĐÃ XÁC NHẬN', class: 'confirmed' },
        'PAID': { text: 'ĐÃ THANH TOÁN', class: 'paid' },
        'CHECKED_OUT': { text: 'ĐÃ TRẢ PHÒNG', class: 'check_out' },
        'CANCELLED': { text: 'ĐÃ HỦY', class: 'cancelled' },
        'CHECK_IN': { text: 'ĐÃ ĐẾN', class: 'check_in' }
    };

    return statusMap[status] || { text: status, class: '' };
}

// ===== ESCAPE HTML =====
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ===== DEBOUNCE =====
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}