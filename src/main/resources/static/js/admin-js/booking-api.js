// ========================================
// booking-api.js - Tất cả API Calls
// ========================================

const API = {
    // ===== WALK-IN =====
    walkIn: async (data) => {
        const response = await fetch('/booking/admin/walk-in', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(data)
        });
        return response.json();
    },

    // ===== INVOICE STATUS =====
    invoiceStatus: async (bookingId) => {
        const response = await fetch('/booking/api/invoice-status/' + bookingId);
        return response.json();
    },

    // ===== MARK AS PAID =====
    markAsPaid: async (detailId) => {
        const response = await fetch('/admin/api/booking-details/' + detailId + '/mark-paid', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' }
        });
        return response.json();
    },

    // ===== CHECKOUT =====
    checkout: async (detailId) => {
        const response = await fetch('/admin/api/booking-details/' + detailId + '/checkout', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' }
        });
        return response.json();
    },

    // ===== FOOD LIST =====
    getFoodList: async (detailId) => {
        const response = await fetch("/admin/api/booking-details/" + detailId + "/foods");
        return response.json();
    },

    // ===== SAVE FOOD ORDER =====
    saveFoodOrder: async (detailId, items) => {
        const response = await fetch("/admin/api/booking-details/" + detailId + "/foods", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(items)
        });
        return response.json();
    },

    // ===== AVAILABLE ROOMS =====
    getAvailableRooms: async (detailId) => {
        const response = await fetch('/admin/api/booking-details/' + detailId + '/available-rooms');
        return response.json();
    },

    // ===== UPDATE FIELD =====
    updateField: async (detailId, field, value) => {
        const response = await fetch('/admin/api/booking-details/' + detailId + '/update-field?field=' + field + '&value=' + encodeURIComponent(value), {
            method: 'PUT'
        });
        return response.json();
    },

    // ===== APPROVE =====
    approve: async (detailId, roomId) => {
        let url = '/admin/api/booking-details/' + detailId + '/approve';
        if (roomId) url += '?roomId=' + roomId;
        const response = await fetch(url, { method: 'PUT' });
        return response.json();
    },

    // ===== REJECT =====
    reject: async (detailId) => {
        const response = await fetch('/admin/api/booking-details/' + detailId + '/reject', { method: 'PUT' });
        return response.json();
    }
};