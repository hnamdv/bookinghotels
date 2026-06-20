const USER_ID = 1;
function money(value){ return value === null || value === undefined ? 'Liên hệ' : new Intl.NumberFormat('vi-VN').format(value) + ' đ/đêm'; }
function fallback(){ return 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1000&q=80'; }
async function loadDetail(){
  const page = document.getElementById('detailPage');
  const id = new URLSearchParams(location.search).get('id');
  if(!id){ page.innerHTML = '<div class="empty">Thiếu mã phòng.</div>'; return; }
  try{
    const res = await fetch('/api/public/rooms/' + id);
    if(!res.ok){ page.innerHTML = '<div class="empty">Không tìm thấy phòng.</div>'; return; }
    const room = await res.json();
    const imgs = room.images && room.images.length ? room.images : [fallback()];
    page.innerHTML = `
      <div class="detail-box">
        <div>
          <div class="detail-main-img"><img id="mainImg" src="${imgs[0]}" alt="${room.nameType}"></div>
          <div class="thumbs">${imgs.map(img => `<img src="${img}" onclick="document.getElementById('mainImg').src='${img}'">`).join('')}</div>
        </div>
        <div class="detail-info">
          <h1>${room.nameType || 'Chi tiết phòng'}</h1>
          <p><strong>Khách sạn:</strong> ${room.hotelName || 'FeelHome Hotel'}</p>
          <p><strong>Địa chỉ:</strong> ${room.hotelAddress || 'Đang cập nhật'}</p>
          ${room.hotelPhone ? `<p><strong>Điện thoại:</strong> ${room.hotelPhone}</p>` : ''}
          ${room.hotelEmail ? `<p><strong>Email:</strong> ${room.hotelEmail}</p>` : ''}
          <p><strong>Giá:</strong> <span class="room-price">${money(room.price)}</span></p>
          <p><strong>Sức chứa:</strong> ${room.capacity || 1} khách</p>
          <p><strong>Giường:</strong> ${room.bed || 'Tiêu chuẩn'}</p>
          <p><strong>Tiện ích:</strong> ${room.hasWifi ? 'Wifi, ' : ''}${room.hasBathtub ? 'Bồn tắm, ' : ''}${room.hasBalcony ? 'Ban công, ' : ''}${room.hasTv ? 'TV' : ''}</p>
          <p>${room.description || 'Không gian nghỉ dưỡng tiện nghi, sang trọng và yên tĩnh.'}</p>
          <button class="fh-btn-primary" onclick="saveFavorite(${room.id})">Lưu phòng yêu thích</button>
        </div>
      </div>`;
    const contact = document.getElementById('detailHotelContact');
    if(contact) contact.textContent = [room.hotelName, room.hotelPhone, room.hotelEmail].filter(Boolean).join(' · ') || 'Thông tin liên hệ đang cập nhật.';
  }catch(e){ page.innerHTML = '<div class="empty">Không thể tải dữ liệu chi tiết.</div>'; }
}
async function saveFavorite(roomTypeId){
  const res = await fetch(`/api/favorites?userId=${USER_ID}&roomTypeId=${roomTypeId}`, { method:'POST' });
  alert(res.ok ? 'Đã lưu phòng yêu thích' : 'Không thể lưu phòng yêu thích');
}
document.addEventListener('DOMContentLoaded', loadDetail);
