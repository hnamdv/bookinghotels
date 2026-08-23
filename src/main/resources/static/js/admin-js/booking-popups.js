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
                    showToast("🎉 Hệ thống đã nhận được tiền thanh toán qua QR!", "success");
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
                console.warn('Đang giữ kết nối an toàn với máy chủ...');
            });
    }, 3000);
}

function cancelWalkInQrModal() {
    if (walkInPaymentInterval) clearInterval(walkInPaymentInterval);
    document.getElementById('walkInQrModal').classList.remove('active');
    location.reload();
}

// ===== FOOD POPUP =====
let currentFoodBookingDetailId = null;

function openFoodPopup(detailId) {
    currentFoodBookingDetailId = detailId;
    const popup = document.getElementById("foodPopup");
    if (popup) popup.classList.add("active");

    const foodListEl = document.getElementById("foodList");
    foodListEl.innerHTML = '<p style="text-align:center;padding:25px;color:#888;"><i class="fas fa-spinner fa-spin"></i> Đang tải danh sách menu...</p>';

    API.getFoodList(detailId)
        .then(menuList => {
            if (!menuList || menuList.length === 0) {
                foodListEl.innerHTML = '<p style="text-align:center;color:#999;padding:20px;">Không có danh sách dịch vụ nào.</p>';
                return;
            }
            let html = "";
            menuList.forEach(item => {
                const qty = item.quantity || 0;
                html += `
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; border-bottom:1px solid #EFF2F7; padding-bottom:8px;">
                        <div>
                            <b style="font-size:13px; color:#1F2C48;">${item.name}</b><br>
                            <span style="font-size:12px; color:#1E6CD4; font-weight:600;">${Number(item.price || 0).toLocaleString('vi-VN')} VND</span>
                        </div>
                        <input type="number" min="0" value="${qty}" data-fwb-id="${item.id}" class="food-qty-input" style="width:70px; padding:6px; border:1px solid #CBD5E1; border-radius:6px; text-align:center; font-weight:600;">
                    </div>`;
            });
            foodListEl.innerHTML = html;
        })
        .catch(err => {
            console.error(err);
            foodListEl.innerHTML = '<p style="text-align:center;color:#EF4444;padding:20px;">❌ Lỗi kết nối tới máy chủ!</p>';
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
                g.innerHTML = '<p style="color:#EF4444;text-align:center;grid-column:1/-1;padding:20px">❌ Hết phòng trống (còn ' + (d.availableCount || 0) + '/' + (d.totalRooms || 0) + ' phòng)</p>';
            }
        })
        .catch(e => {
            document.getElementById('pickRoomCardGrid').innerHTML = '<p style="color:#EF4444;text-align:center;grid-column:1/-1;padding:20px">❌ Lỗi kết nối máy chủ</p>';
        });
}

function pickRoom(did, rid, rn) {
    pickedRooms[did] = { roomId: rid, roomNumber: rn };
    const row = document.querySelector('tr[data-detail-id="' + did + '"]');
    if (row) row.cells[3].innerHTML = '<span class="room-picked" onclick="openPickRoomPopup(' + did + ',0)"><i class="fas fa-bed"></i> ' + rn + '</span>';
    document.getElementById('pickRoomPopup').classList.remove('active');
    showToast('✅ Đã chọn phòng ' + rn + ' (bấm Duyệt để lưu)', 'success');
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
    } else if (field === 'bookingDate' || field === 'checkinDate' || field === 'checkoutDate') {
        t.innerHTML = '<i class="fas fa-calendar"></i> Sửa ngày';
        c.innerHTML = '<div class="form-group"><label>Ngày</label><input type="date" id="editVal" class="form-control" value="' + val + '"></div>';
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
            const icon = document.getElementById('confirmIcon');
            const title = document.getElementById('confirmTitle');
            const message = document.getElementById('confirmMessage');
            const actionBtn = document.getElementById('confirmActionBtn');

            icon.textContent = config.icon || '⚠️';
            icon.className = 'icon ' + (config.iconClass || 'warning');
            title.textContent = config.title || 'Xác nhận';
            message.textContent = config.message || 'Bạn có chắc chắn muốn thực hiện hành động này?';
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
            document.getElementById('confirmPopup').classList.remove('active');
            confirmCallback = null;
        }

        document.getElementById('confirmPopup')?.addEventListener('click', function(e) {
            if (e.target === this) closeConfirmPopup();
        });

        // ===== MARK AS PAID =====
        function markAsPaid(detailId) {
            showConfirmPopup({
                icon: '✅',
                iconClass: 'success',
                title: 'Xác nhận thanh toán',
                message: 'Khách hàng đã thanh toán tiền phòng cho đơn này?\nSau khi xác nhận, trạng thái sẽ chuyển thành "ĐÃ THANH TOÁN".',
                btnText: 'Xác nhận thanh toán',
                btnClass: 'btn-success-confirm',
                onConfirm: function() {
                    fetch('/admin/api/booking-details/' + detailId + '/mark-paid', {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' }
                    })
                    .then(r => r.json())
                    .then(res => {
                        if (res.success) {
                            showToast("✅ Đã cập nhật trạng thái thanh toán!", "success");
                            setTimeout(() => location.reload(), 1000);
                        } else {
                            showToast("❌ " + res.message, "error");
                        }
                    })
                    .catch(err => {
                        console.error(err);
                        showToast("❌ Lỗi kết nối tới máy chủ!", "error");
                    });
                }
            });
        }

        // ===== REJECT =====
        function rejectDetail(did) {
            showConfirmPopup({
                icon: '⚠️',
                iconClass: 'danger',
                title: 'Xác nhận hủy đơn',
                message: 'Bạn có chắc chắn muốn hủy đơn đặt phòng này?\nHành động này không thể hoàn tác!',
                btnText: 'Xác nhận hủy',
                btnClass: 'btn-danger-confirm',
                onConfirm: function() {
                    fetch('/admin/api/booking-details/' + did + '/reject', { method: 'PUT' })
                    .then(r => r.json())
                    .then(res => {
                        if (res.success) {
                            showToast("✅ Đã hủy đơn thành công!", "success");
                            setTimeout(() => location.reload(), 1000);
                        } else {
                            showToast("❌ " + res.message, "error");
                        }
                    })
                    .catch(e => {
                        showToast("❌ Lỗi kết nối!", "error");
                    });
                }
            });
        }

        // ===== CHECK-OUT =====
        function checkOut(detailId) {
            showConfirmPopup({
                icon: '🚪',
                iconClass: 'info',
                title: 'Xác nhận trả phòng',
                message: 'Khách hàng đã trả phòng?\nSau khi xác nhận, trạng thái phòng sẽ chuyển thành "ĐÃ TRẢ PHÒNG".',
                btnText: 'Xác nhận trả phòng',
                btnClass: 'btn-info-confirm',
                onConfirm: function() {
                    fetch('/admin/api/booking-details/' + detailId + '/checkout', {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' }
                    })
                    .then(r => r.json())
                    .then(res => {
                        if (res.success) {
                            showToast("✅ Đã trả phòng thành công!", "success");
                            setTimeout(() => location.reload(), 1000);
                        } else {
                            showToast("❌ " + res.message, "error");
                        }
                    })
                    .catch(err => {
                        console.error(err);
                        showToast("❌ Lỗi kết nối tới máy chủ!", "error");
                    });
                }
            });
        }

        // ===== APPROVE =====
        function approveNow(did) {
            const p = pickedRooms[did];
            if (p && p.roomId) {
                showConfirmPopup({
                    icon: '✅',
                    iconClass: 'success',
                    title: 'Xác nhận duyệt đơn',
                    message: 'Xác nhận duyệt đơn đặt phòng và gán phòng ' + p.roomNumber + '?',
                    btnText: 'Xác nhận duyệt',
                    btnClass: 'btn-success-confirm',
                    onConfirm: function() {
                        fetch('/admin/api/booking-details/' + did + '/approve?roomId=' + p.roomId, { method: 'PUT' })
                        .then(r => r.json())
                        .then(res => {
                            if (res.success) {
                                showToast("✅ Đã duyệt đơn và gán phòng thành công!", "success");
                                setTimeout(() => location.reload(), 1000);
                            } else {
                                showToast("❌ " + res.message, "error");
                            }
                        })
                        .catch(e => {
                            showToast("❌ Lỗi kết nối!", "error");
                        });
                    }
                });
            } else {
                showToast("⚠️ Vui lòng bấm vào 'Chọn phòng' để gán phòng trước khi duyệt!", "warning");
            }
        }