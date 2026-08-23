// ========================================
// booking-main.js - File chính
// ========================================

// ===== INITIALIZE =====
document.addEventListener("DOMContentLoaded", function() {
    // 1. Khởi tạo phân trang
    iP();

    // 2. Khởi tạo biểu đồ
    if (typeof forecastDays !== 'undefined' && typeof forecastData !== 'undefined') {
        initChart(forecastDays, forecastData);
    }

    // 3. Event listeners
    document.getElementById('walkInModal')?.addEventListener('click', function(e) {
        if (e.target === this) closeWalkInModal();
    });

    document.getElementById('foodPopup')?.addEventListener('click', function(e) {
        if (e.target === this) closeFoodPopup();
    });

    document.getElementById('pickRoomPopup')?.addEventListener('click', function(e) {
        if (e.target === this) this.classList.remove('active');
    });

    document.getElementById('editPopup')?.addEventListener('click', function(e) {
        if (e.target === this) closeEditPopup();
    });
});