// ========================================
// booking-api.js - Tất cả API Calls
// ========================================

const API = {
    // ===== WALK-IN =====
    walkIn: async (data) => {
        try {
            const response = await fetch('/booking/admin/walk-in', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(data)
            });
            return await response.json();
        } catch (error) {
            console.error('Walk-in API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== INVOICE STATUS =====
    invoiceStatus: async (bookingId) => {
        try {
            const response = await fetch('/booking/api/invoice-status/' + bookingId);
            return await response.json();
        } catch (error) {
            console.error('Invoice status API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== GET BOOKING DETAIL =====
    getBookingDetail: async (detailId) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId);
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return await response.json();
        } catch (error) {
            console.error('Get booking detail API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== APPROVE BOOKING (PENDING → APPROVED) =====
    approve: async (detailId, roomId = null) => {
        try {
            let url = '/admin/api/booking-details/' + detailId + '/approve';
            if (roomId) url += '?roomId=' + roomId;

            const response = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' }
            });
            return await response.json();
        } catch (error) {
            console.error('Approve API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== MARK AS PAID (APPROVED → PAID) =====
    markAsPaid: async (detailId) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId + '/mark-paid', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' }
            });
            return await response.json();
        } catch (error) {
            console.error('Mark as paid API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== CHECKOUT (PAID → CHECKED_OUT) =====
    checkout: async (detailId) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId + '/check-out', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' }
            });
            return await response.json();
        } catch (error) {
            console.error('Checkout API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== CANCEL BOOKING (→ CANCELLED) =====
    cancel: async (detailId, reason = '') => {
        try {
            let url = '/admin/api/booking-details/' + detailId + '/reject';
            if (reason) url += '?reason=' + encodeURIComponent(reason);

            const response = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' }
            });
            return await response.json();
        } catch (error) {
            console.error('Cancel API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== ASSIGN ROOM (Chỉ gán phòng, không đổi status) =====
    assignRoom: async (detailId, roomId) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId + '/assign-room?roomId=' + roomId, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' }
            });
            return await response.json();
        } catch (error) {
            console.error('Assign room API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== GET FOOD LIST =====
    getFoodList: async (detailId) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId + '/foods');
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            const data = await response.json();

            // Xử lý response để trả về array
            if (data && data.success && Array.isArray(data.foods)) {
                return data.foods;
            } else if (Array.isArray(data)) {
                return data;
            } else {
                console.warn('Unexpected food list format:', data);
                return [];
            }
        } catch (error) {
            console.error('Get food list API error:', error);
            return [];
        }
    },

    // ===== SAVE FOOD ORDER =====
    saveFoodOrder: async (detailId, items) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId + '/foods', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(items)
            });
            return await response.json();
        } catch (error) {
            console.error('Save food order API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== GET AVAILABLE ROOMS =====
    getAvailableRooms: async (detailId) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId + '/available-rooms');
            return await response.json();
        } catch (error) {
            console.error('Get available rooms API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== UPDATE FIELD =====
    updateField: async (detailId, field, value) => {
        try {
            const response = await fetch('/admin/api/booking-details/' + detailId + '/update-field?field=' + field + '&value=' + encodeURIComponent(value), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' }
            });
            return await response.json();
        } catch (error) {
            console.error('Update field API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    },

    // ===== REJECT BOOKING (Tương đương CANCEL) =====
    reject: async (detailId, reason = '') => {
        try {
            let url = '/admin/api/booking-details/' + detailId + '/reject';
            if (reason) url += '?reason=' + encodeURIComponent(reason);

            const response = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' }
            });
            return await response.json();
        } catch (error) {
            console.error('Reject API error:', error);
            return { success: false, message: 'Lỗi kết nối server' };
        }
    }
};

// Export API object
window.API = API;