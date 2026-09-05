// ========================================
// booking-option.js - Các tùy chọn và cấu hình
// ========================================

// ===== CẤU HÌNH PHÂN TRANG =====
const PAGINATION_CONFIG = {
    rowsPerPage: 10,
    maxPageButtons: 5
};

// ===== CẤU HÌNH TOAST =====
const TOAST_CONFIG = {
    duration: 3000,
    position: 'top-right'
};

// ===== CẤU HÌNH POPUP =====
const POPUP_CONFIG = {
    animationDuration: 300,
    closeOnEscape: true,
    closeOnOverlayClick: true
};

// ===== CẤU HÌNH API =====
const API_CONFIG = {
    baseUrl: '/api/admin/booking-details',
    timeout: 30000
};

// ===== TRẠNG THÁI BOOKING =====
const BOOKING_STATUS = {
    PENDING: 'PENDING',
    APPROVED: 'APPROVED',
    CONFIRMED: 'CONFIRMED',
    PAID: 'PAID',
    CHECKED_OUT: 'CHECKED_OUT',
    CANCELLED: 'CANCELLED',
    CHECK_IN:'CHECK_IN'
};

// ===== PHƯƠNG THỨC THANH TOÁN =====
const PAYMENT_METHODS = {
    CASH: 'TIEN_MAT',
    TRANSFER: 'CHUYEN_KHOAN',
    CARD: 'THE'
};

// ===== EVENT HANDLERS CHO PHÍM TẮT =====
document.addEventListener('keydown', function(e) {
    // ESC để đóng popup
    if (e.key === 'Escape') {
        closeAllPopups();
    }
});

function closeAllPopups() {
    const popups = [
        'walkInModal',
        'walkInQrModal',
        'foodPopup',
        'pickRoomPopup',
        'editPopup',
        'confirmPopup'
    ];

    popups.forEach(id => {
        const popup = document.getElementById(id);
        if (popup) popup.classList.remove('active');
    });
}

// ===== EXPORT CONFIG =====
window.BOOKING_CONFIG = {
    PAGINATION: PAGINATION_CONFIG,
    TOAST: TOAST_CONFIG,
    POPUP: POPUP_CONFIG,
    API: API_CONFIG,
    STATUS: BOOKING_STATUS,
    PAYMENT_METHODS: PAYMENT_METHODS
};