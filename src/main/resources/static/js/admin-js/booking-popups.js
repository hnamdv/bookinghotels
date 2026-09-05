// ========================================
// booking-popups.js - Tất cả Popups
// ========================================

// ===== WALK-IN MODAL =====
function openWalkInModal() {
    document.getElementById('walkInModal').classList.add('active');
    const today = new Date().toISOString().split('T')[0];
    let tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().split('T')[0];
    document.getElementById('wCheckin').value = today;
    document.getElementById('wCheckout').value = tomorrowStr;
}

function closeWalkInModal() {
    document.getElementById('walkInModal').classList.remove('active');
}

// ===== QR MODAL =====
let createdBookingIdForQr = null;
let walkInPaymentInterval = null;
let isWalkInProcessing = false;

function startWalkInPaymentChecking(bookingId) {
    if (walkInPaymentInterval) clearInterval(walkInPaymentInterval);
    walkInPaymentInterval = setInterval(function() {
        if (isWalkInProcessing) return;
        isWalkInProcessing = true;
        API.invoiceStatus(bookingId)
            .then(data => {
                isWalkInProcessing = false;
                let currentStatus = data.status || data.paymentStatus;
                if (currentStatus === 'PAID') {
                    clearInterval(walkInPaymentInterval);
                    showToast("Đã nhận được tiền thanh toán!", "success");
                    setTimeout(() => {
                        document.getElementById('walkInQrModal').classList.remove('active');
                        location.reload();
                    }, 1500);
                } else if (currentStatus === 'CANCELLED') {
                    clearInterval(walkInPaymentInterval);
                }
            })
            .catch(error => {
                isWalkInProcessing = false;
                console.warn('Đang giữ kết nối...');
            });
    }, 3000);
}

// Đóng modal - GIỮ booking (không hủy)
function closeWalkInQrModalOnly() {
    if (walkInPaymentInterval) clearInterval(walkInPaymentInterval);
    document.getElementById('walkInQrModal').classList.remove('active');

    createdBookingIdForQr = null;
    walkInPaymentInterval = null;
    isWalkInProcessing = false;

    showToast('Đã đóng, đơn vẫn ở trạng thái chờ duyệt', 'warning');
    setTimeout(() => location.reload(), 500);
}

// Hủy booking khi đóng QR
function cancelWalkInQrModal() {
    if (walkInPaymentInterval) clearInterval(walkInPaymentInterval);
    document.getElementById('walkInQrModal').classList.remove('active');

    if (createdBookingIdForQr) {
        API.cancelBookingByQr(createdBookingIdForQr)
            .then(res => {
                showToast('Đã hủy đơn', 'warning');
            })
            .catch(err => {
                console.error('Lỗi hủy booking:', err);
            });
    }

    createdBookingIdForQr = null;
    walkInPaymentInterval = null;
    isWalkInProcessing = false;

    setTimeout(() => location.reload(), 500);
}

// ===== FOOD POPUP =====
let currentFoodBookingDetailId = null;

function openFoodPopup(detailId) {
    currentFoodBookingDetailId = detailId;
    const popup = document.getElementById("foodPopup");
    if (popup) popup.classList.add("active");

    const foodListEl = document.getElementById("foodList");
    foodListEl.innerHTML = '<p style="text-align:center;padding:25px;color:#888;"><i class="fas fa-spinner fa-spin"></i> Đang tải...</p>';

    API.getFoodList(detailId)
        .then(menuList => {
            if (!Array.isArray(menuList) || menuList.length === 0) {
                foodListEl.innerHTML = '<p style="text-align:center;color:#999;padding:20px;">Không có dịch vụ nào.</p>';
                return;
            }

            let html = "";
            menuList.forEach(item => {
                const qty = item.quantity || 0;
                const name = item.name || item.description || 'Dịch vụ';
                const price = Number(item.price || 0).toLocaleString('vi-VN');

                html += `
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; border-bottom:1px solid #EFF2F7; padding-bottom:8px;">
                        <div>
                            <b style="font-size:13px; color:#1F2C48;">${name}</b><br>
                            <span style="font-size:12px; color:#1E6CD4; font-weight:600;">${price} VND</span>
                        </div>
                        <input type="number" min="0" value="${qty}" data-fwb-id="${item.id}" class="food-qty-input" style="width:70px; padding:6px; border:1px solid #CBD5E1; border-radius:6px; text-align:center; font-weight:600;">
                    </div>`;
            });
            foodListEl.innerHTML = html;
        })
        .catch(err => {
            console.error('Error:', err);
            foodListEl.innerHTML = '<p style="text-align:center;color:#EF4444;padding:20px;">❌ Lỗi kết nối!</p>';
        });
}

function closeFoodPopup() {
    const popup = document.getElementById("foodPopup");
    if (popup) popup.classList.remove("active");
    currentFoodBookingDetailId = null;
}

// ===== PICK ROOM POPUP =====
let pickedRooms = {};

function openPickRoomPopup(did, rtid) {
    document.getElementById('pickRoomCardGrid').innerHTML = '<p style="color:#999;text-align:center;grid-column:1/-1;padding:20px">Đang tải...</p>';
    document.getElementById('pickRoomPopup').classList.add('active');

    API.getAvailableRooms(did)
        .then(d => {
            const g = document.getElementById('pickRoomCardGrid');
            if (d.success && d.rooms && d.rooms.length > 0) {
                g.innerHTML = d.rooms.map(r =>
                    '<div class="room-card" onclick="pickRoom(' + did + ',' + r.id + ',\'' + r.roomNumber + '\')"><div class="room-number">' + r.roomNumber + '</div></div>'
                ).join('');
            } else {
                g.innerHTML = '<p style="color:#EF4444;text-align:center;grid-column:1/-1;padding:20px">❌ Hết phòng trống</p>';
            }
        })
        .catch(e => {
            document.getElementById('pickRoomCardGrid').innerHTML = '<p style="color:#EF4444;text-align:center;grid-column:1/-1;padding:20px">❌ Lỗi kết nối</p>';
        });
}

function closePickRoomPopup() {
    document.getElementById('pickRoomPopup').classList.remove('active');
}

function pickRoom(did, rid, rn) {
    pickedRooms[did] = { roomId: rid, roomNumber: rn };
    const row = document.querySelector('tr[data-detail-id="' + did + '"]');
    if (row) row.cells[3].innerHTML = '<span class="room-picked" onclick="openPickRoomPopup(' + did + ',0)"><i class="fas fa-bed"></i> ' + rn + '</span>';
    document.getElementById('pickRoomPopup').classList.remove('active');
    showToast('Đã chọn phòng ' + rn, 'success');
}

// ===== EDIT POPUP =====
let editDetailId = null,
    editField = null;

function openEditPopup(did, field, val) {
    editDetailId = did;
    editField = field;
    const t = document.getElementById('editPopupTitle');
    const c = document.getElementById('editPopupContent');

    if (field === 'name') {
        t.innerHTML = '<i class="fas fa-user-edit"></i> Sửa tên khách';
        c.innerHTML = '<div class="form-group"><label>Tên khách</label><input id="editVal" class="form-control" value="' + val + '"></div>';
    } else if (field === 'phone') {
        t.innerHTML = '<i class="fas fa-phone"></i> Sửa SĐT';
        c.innerHTML = '<div class="form-group"><label>Số điện thoại</label><input id="editVal" class="form-control" value="' + val + '"></div>';
    } else if (field === 'guests') {
        const parts = val.split(',');
        t.innerHTML = '<i class="fas fa-users"></i> Sửa số khách';
        c.innerHTML = '<div class="form-group"><label>Người lớn</label><input type="number" id="editAdult" class="form-control" value="' + parts[0] + '" min="1"></div><div class="form-group"><label>Trẻ em</label><input type="number" id="editChild" class="form-control" value="' + (parts[1] || 0) + '" min="0"></div>';
    }
    document.getElementById('editPopup').classList.add('active');
}

function closeEditPopup() {
    document.getElementById('editPopup').classList.remove('active');
    editDetailId = null;
    editField = null;
}

// ===== CONFIRM POPUP =====
let confirmCallback = null;

function showConfirmPopup(config) {
    const overlay = document.getElementById('confirmPopup');
    if (!overlay) {
        if (confirm(config.title || 'Xác nhận?')) {
            if (config.onConfirm) config.onConfirm();
        }
        return;
    }

    const icon = document.getElementById('confirmIcon');
    const title = document.getElementById('confirmTitle');
    const message = document.getElementById('confirmMessage');
    const actionBtn = document.getElementById('confirmActionBtn');

    icon.textContent = config.icon || '⚠️';
    icon.className = 'icon ' + (config.iconClass || 'warning');
    title.textContent = config.title || 'Xác nhận';
    message.textContent = config.message || '';
    actionBtn.textContent = config.btnText || 'Xác nhận';
    actionBtn.className = config.btnClass || 'btn-success-confirm';
    confirmCallback = config.onConfirm || null;

    actionBtn.onclick = function() {
        if (confirmCallback) confirmCallback();
        closeConfirmPopup();
    };

    overlay.classList.add('active');
}

function closeConfirmPopup() {
    const overlay = document.getElementById('confirmPopup');
    if (overlay) {
        overlay.classList.remove('active');
    }
    confirmCallback = null;
}

document.getElementById('confirmPopup')?.addEventListener('click', function(e) {
    if (e.target === this) closeConfirmPopup();
});

// ===== DROPDOWN =====
function toggleDropdown(event, btn) {
    if (event) event.stopPropagation();
    const dropdown = btn.closest('.action-dropdown');
    const menu = dropdown.querySelector('.dropdown-menu');
    const isOpen = menu.classList.contains('show');

    document.querySelectorAll('.action-dropdown .dropdown-menu.show').forEach(m => {
        if (m !== menu) m.classList.remove('show');
    });

    menu.classList.toggle('show');
}

function closeDropdown(element) {
    const dropdown = element.closest('.action-dropdown');
    if (dropdown) {
        const menu = dropdown.querySelector('.dropdown-menu');
        if (menu) menu.classList.remove('show');
    }
}

document.addEventListener('click', function(e) {
    document.querySelectorAll('.action-dropdown .dropdown-menu.show').forEach(menu => {
        if (!menu.closest('.action-dropdown').contains(e.target)) {
            menu.classList.remove('show');
        }
    });
});