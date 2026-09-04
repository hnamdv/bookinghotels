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
                    showToast(result.message, "success");
                    setTimeout(() => location.reload(), 1000);
                }
            } else {
                showToast(result.message, "error");
            }
        })
        .catch(err => {
            console.error(err);
            showToast("Lỗi kết nối máy chủ!", "error");
        });
}

// ===== HÀM DUYỆT BOOKING =====
function approveBooking(detailId) {
    const pickedRoom = pickedRooms[detailId];

    if (pickedRoom && pickedRoom.roomId) {
        showConfirmPopup({
            icon: '✅',
            iconClass: 'success',
            title: 'Xác nhận duyệt',
            message: 'Duyệt booking #' + detailId + ' - Phòng ' + pickedRoom.roomNumber + '?',
            btnText: 'Duyệt',
            btnClass: 'btn-success-confirm',
            onConfirm: function() {
                API.approve(detailId, pickedRoom.roomId)
                    .then(res => {
                        if (res.success) {
                            showToast(res.message, "success");
                            setTimeout(() => location.reload(), 1000);
                        } else {
                            showToast(res.message, "error");
                        }
                    })
                    .catch(err => {
                        console.error('Approve error:', err);
                        showToast("Lỗi kết nối!", "error");
                    });
            }
        });
        return;
    }

    API.getBookingDetail(detailId)
        .then(detail => {
            if (detail.success && detail.roomId) {
                showConfirmPopup({
                    icon: '✅',
                    iconClass: 'success',
                    title: 'Xác nhận duyệt',
                    message: 'Duyệt booking #' + detailId + '?',
                    btnText: 'Duyệt',
                    btnClass: 'btn-success-confirm',
                    onConfirm: function() {
                        API.approve(detailId, detail.roomId)
                            .then(res => {
                                if (res.success) {
                                    showToast(res.message, "success");
                                    setTimeout(() => location.reload(), 1000);
                                } else {
                                    showToast(res.message, "error");
                                }
                            })
                            .catch(err => {
                                console.error('Approve error:', err);
                                showToast("Lỗi kết nối!", "error");
                            });
                    }
                });
            } else {
                showToast('Vui lòng chọn phòng trước khi duyệt', 'warning');
                openPickRoomPopup(detailId, detail.roomTypeId);
            }
        })
        .catch(err => {
            console.error('Get detail error:', err);
            showToast('Vui lòng chọn phòng trước khi duyệt', 'warning');
            openPickRoomPopup(detailId, 0);
        });
}

// ===== HÀM XÁC NHẬN THANH TOÁN =====
function markAsPaid(detailId) {
    showConfirmPopup({
        icon: '💳',
        iconClass: 'success',
        title: 'Xác nhận thanh toán',
        message: 'Xác nhận khách đã thanh toán?',
        btnText: 'Xác nhận',
        btnClass: 'btn-success-confirm',
        onConfirm: function() {
            API.markAsPaid(detailId)
                .then(res => {
                    if (res.success) {
                        showToast(res.message, "success");
                        setTimeout(() => location.reload(), 1000);
                    } else {
                        showToast(res.message, "error");
                    }
                })
                .catch(err => {
                    console.error('Mark as paid error:', err);
                    showToast("Lỗi kết nối!", "error");
                });
        }
    });
}
function checkInBooking(detailId){
    showConfirmPopup({
        icon: '🔑',
                iconClass: 'warning',
                title: 'Xác nhận nhận phòng',
                message: 'Xác nhận khách đã nhận phòng?',
                btnText: 'Nhận phòng',
                btnClass: 'btn-warning-confirm',
                onConfirm: function(){
                    API.checkin(detailId)
                        .then(res => {
                            if(res.success){
                                showToast(res.message,"success")
                                setTimeout(() => location.reload(), 1000);
                            }else{
                                showToast(res.message, "error");
                            }
                        })
                        .catch(err => {
                            console.error('Checkout error:', err);
                            showToast("Lỗi kết nối!", "error");
                        });
                }
    });
}
// ===== HÀM TRẢ PHÒNG =====
function checkOutBooking(detailId) {
    showConfirmPopup({
        icon: '🚪',
        iconClass: 'info',
        title: 'Xác nhận trả phòng',
        message: 'Xác nhận khách đã trả phòng?',
        btnText: 'Xác nhận',
        btnClass: 'btn-info-confirm',
        onConfirm: function() {
            API.checkout(detailId)
                .then(res => {
                    if (res.success) {
                        showToast(res.message, "success");
                        setTimeout(() => location.reload(), 1000);
                    } else {
                        showToast(res.message, "error");
                    }
                })
                .catch(err => {
                    console.error('Checkout error:', err);
                    showToast("Lỗi kết nối!", "error");
                });
        }
    });
}

// ===== HÀM HỦY BOOKING =====
function cancelBooking(detailId) {
    showConfirmPopup({
        icon: '⚠️',
        iconClass: 'danger',
        title: 'Xác nhận hủy',
        message: 'Hủy booking #' + detailId + '?',
        btnText: 'Hủy booking',
        btnClass: 'btn-danger-confirm',
        onConfirm: function() {
            API.cancel(detailId)
                .then(res => {
                    if (res.success) {
                        showToast(res.message, "success");
                        setTimeout(() => location.reload(), 1000);
                    } else {
                        showToast(res.message, "error");
                    }
                })
                .catch(err => {
                    console.error('Cancel error:', err);
                    showToast("Lỗi kết nối!", "error");
                });
        }
    });
}

// ===== HÀM MỞ POPUP SỬA BOOKING =====
function openEditBooking(detailId) {
    // Lấy thông tin chi tiết
    API.getBookingDetail(detailId)
        .then(detail => {
            if (detail.success) {
                // Điền thông tin vào form
                document.getElementById('editBookingId').value = detailId;
                document.getElementById('editBookingName').value = detail.customerName || '';
                document.getElementById('editBookingPhone').value = detail.customerPhone || '';
                document.getElementById('editBookingEmail').value = detail.customerEmail || '';
                document.getElementById('editBookingCheckin').value = detail.checkinDate || '';
                document.getElementById('editBookingCheckout').value = detail.checkoutDate || '';
                document.getElementById('editBookingAdult').value = detail.adultCount || 1;
                document.getElementById('editBookingChild').value = detail.childCount || 0;

                // Mở popup
                document.getElementById('editBookingPopup').classList.add('active');
            } else {
                showToast('Không tìm thấy thông tin booking', 'error');
            }
        })
        .catch(err => {
            console.error('Get detail error:', err);
            showToast('Lỗi kết nối!', 'error');
        });
}

function closeEditBookingPopup() {
    document.getElementById('editBookingPopup').classList.remove('active');
}

// ===== HÀM LƯU SỬA BOOKING =====
function saveEditBooking(event) {
    event.preventDefault();

    const detailId = document.getElementById('editBookingId').value;
    const name = document.getElementById('editBookingName').value;
    const phone = document.getElementById('editBookingPhone').value;
    const adult = document.getElementById('editBookingAdult').value;
    const child = document.getElementById('editBookingChild').value;

    if (!detailId) {
        showToast('Không tìm thấy ID booking', 'error');
        return;
    }

    // Cập nhật từng field
    const updates = [];

    if (name) updates.push(API.updateField(detailId, 'name', name));
    if (phone) updates.push(API.updateField(detailId, 'phone', phone));
    updates.push(API.updateField(detailId, 'guests', adult + ',' + child));

    // Chờ tất cả updates hoàn thành
    Promise.all(updates)
        .then(results => {
            const allSuccess = results.every(r => r.success);
            if (allSuccess) {
                showToast('Cập nhật thành công!', 'success');
                closeEditBookingPopup();
                setTimeout(() => location.reload(), 1000);
            } else {
                const errorResult = results.find(r => !r.success);
                showToast(errorResult?.message || 'Cập nhật thất bại', 'error');
            }
        })
        .catch(err => {
            console.error('Update error:', err);
            showToast('Lỗi kết nối!', 'error');
        });
}

// ===== HÀM GÁN PHÒNG VÀ DUYỆT =====
function assignAndApprove(detailId, roomId) {
    showConfirmPopup({
        icon: '✅',
        iconClass: 'success',
        title: 'Xác nhận',
        message: 'Gán phòng và duyệt?',
        btnText: 'Xác nhận',
        btnClass: 'btn-success-confirm',
        onConfirm: function() {
            API.approve(detailId, roomId)
                .then(res => {
                    if (res.success) {
                        showToast(res.message, "success");
                        closePickRoomPopup();
                        setTimeout(() => location.reload(), 1000);
                    } else {
                        showToast(res.message, "error");
                    }
                })
                .catch(err => {
                    console.error('Approve error:', err);
                    showToast("Lỗi kết nối!", "error");
                });
        }
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
        if (fwbId && qty > 0) {
            items.push({ fwbId: fwbId, quantity: qty });
        }
    });

    API.saveFoodOrder(currentFoodBookingDetailId, items)
        .then(res => {
            if (res.success) {
                showToast(res.message, "success");
                closeFoodPopup();
                setTimeout(() => location.reload(), 1000);
            } else {
                showToast(res.message, "error");
            }
        })
        .catch(err => {
            console.error(err);
            showToast("Lỗi kết nối!", "error");
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
                showToast(res.message, "success");
                closeEditPopup();
                setTimeout(() => location.reload(), 1000);
            } else {
                showToast(res.message, "error");
            }
        })
        .catch(e => {
            showToast("Lỗi kết nối!", "error");
        });
}

// ===== APPROVE (Hàm cũ giữ lại để tương thích) =====
function approveNow(did) {
    approveBooking(did);
}

// ===== REJECT (Hàm cũ giữ lại để tương thích) =====
function rejectDetail(did) {
    cancelBooking(did);
}

// ===== CHECKOUT (Hàm cũ giữ lại để tương thích) =====
function checkOut(detailId) {
    checkOutBooking(detailId);
}