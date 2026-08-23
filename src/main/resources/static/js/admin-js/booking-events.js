// ========================================
// booking-events.js - Các sự kiện chính
// ========================================

// ===== SUBMIT WALK-IN FORM =====
function submitWalkInForm(event) {
    event.preventDefault();
    const paymentMethod = document.getElementById('wPaymentMethod').value;

    const data = {
        roomTypeId: document.getElementById('wRoomTypeId').value,
        customerName: document.getElementById('wName').value,
        customerPhone: document.getElementById('wPhone').value,
        customerEmail: document.getElementById('wEmail').value,
        adultCount: document.getElementById('wAdult').value,
        childCount: document.getElementById('wChild').value,
        checkinDate: document.getElementById('wCheckin').value,
        checkoutDate: document.getElementById('wCheckout').value,
        paymentMethod: paymentMethod
    };

    API.walkIn(data)
        .then(result => {
            if (result.success) {
                closeWalkInModal();
                if (paymentMethod === 'CHUYEN_KHOAN') {
                    createdBookingIdForQr = result.bookingId;
                    const amount = Math.round(parseFloat(result.totalAmount || 0));
                    const content = "FEELHOMEBK" + result.bookingId;
                    document.getElementById('qrContentText').innerText = content;
                    const qrUrl = `https://qr.sepay.vn/img?bank=MBBank&acc=0855587468&template=compact&showinfo=true&holder=NGUYEN%20DUC%20PHAT&amount=${amount}&des=${content}`;
                    document.getElementById('dynamicQrImg').src = qrUrl;
                    document.getElementById('walkInQrModal').classList.add('active');
                    startWalkInPaymentChecking(result.bookingId);
                } else {
                    showToast("✅ " + result.message, "success");
                    setTimeout(() => location.reload(), 1000);
                }
            } else {
                showToast("❌ " + result.message, "error");
            }
        })
        .catch(err => {
            console.error(err);
            showToast("❌ Lỗi kết nối máy chủ!", "error");
        });
}

// ===== MARK AS PAID =====
function markAsPaid(detailId) {
    if (!confirm("Xác nhận khách đã thanh toán tiền phòng cho đơn này?")) return;
    API.markAsPaid(detailId)
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

// ===== CHECK-OUT =====
function checkOut(detailId) {
    if (!confirm("Xác nhận khách đã trả phòng?")) return;
    API.checkout(detailId)
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

// ===== SAVE FOOD ORDER =====
function saveFoodOrder() {
    if (!currentFoodBookingDetailId) return;
    const inputs = document.querySelectorAll("#foodList .food-qty-input");

    let items = [];
    inputs.forEach(input => {
        const qty = parseInt(input.value) || 0;
        const fwbId = parseInt(input.getAttribute("data-fwb-id"));
        if (fwbId) {
            items.push({ fwbId: fwbId, quantity: qty });
        }
    });

    API.saveFoodOrder(currentFoodBookingDetailId, items)
        .then(res => {
            if (res.success) {
                showToast("✅ Đã cập nhật phụ thu thành công!", "success");
                closeFoodPopup();
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

// ===== SAVE EDIT POPUP =====
function saveEditPopup() {
    if (!editDetailId || !editField) return;
    let val = "";
    if (editField === 'guests') {
        const a = document.getElementById('editAdult').value;
        const ch = document.getElementById('editChild').value;
        val = a + ',' + ch;
    } else {
        val = document.getElementById('editVal').value;
    }

    API.updateField(editDetailId, editField, val)
        .then(res => {
            if (res.success) {
                showToast("✅ Cập nhật thành công!", "success");
                closeEditPopup();
                setTimeout(() => location.reload(), 1000);
            } else {
                showToast("❌ " + res.message, "error");
            }
        })
        .catch(e => {
            showToast("❌ Lỗi kết nối!", "error");
        });
}

// ===== APPROVE =====
function approveNow(did) {
    const p = pickedRooms[did];
    let url = '/admin/api/booking-details/' + did + '/approve';
    if (p && p.roomId) {
        url += '?roomId=' + p.roomId;
    } else {
        showToast("⚠️ Vui lòng bấm vào 'Chọn phòng' để gán phòng trước khi duyệt!", "warning");
        return;
    }

    API.approve(did, p.roomId)
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

// ===== REJECT =====
function rejectDetail(did) {
    if (!confirm("Bạn có chắc chắn muốn hủy đơn này không?")) return;
    API.reject(did)
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