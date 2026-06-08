const USER_ID = 1;
function money(value){ return value === null || value === undefined ? 'Liên hệ' : new Intl.NumberFormat('vi-VN').format(value) + ' đ/đêm'; }
function img(room){ return room.thumbnail && room.thumbnail !== '/img' ? room.thumbnail : 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80'; }
async function loadFavorites(){
  const grid = document.getElementById('favoriteGrid');
  grid.innerHTML = '<div class="empty">Đang tải danh sách yêu thích...</div>';
  try{
    const res = await fetch('/api/favorites/' + USER_ID);
    const rooms = await res.json();
    if(!rooms.length){ grid.innerHTML = '<div class="empty">Chưa có phòng yêu thích.</div>'; return; }
    grid.innerHTML = rooms.map(room => `<article class="room-card"><div class="room-img"><img src="${img(room)}"></div><div class="room-body"><h3 class="room-title">${room.nameType}</h3><div class="room-price">${money(room.price)}</div><p class="room-desc">${room.description || ''}</p><div class="room-actions"><a href="/room-detail.html?id=${room.id}">Chi tiết</a><button onclick="removeFavorite(${room.id})">Hủy lưu</button></div></div></article>`).join('');
  }catch(e){ grid.innerHTML = '<div class="empty">Không thể tải danh sách yêu thích.</div>'; }
}
async function removeFavorite(roomTypeId){
  await fetch(`/api/favorites?userId=${USER_ID}&roomTypeId=${roomTypeId}`, { method:'DELETE' });
  loadFavorites();
}
document.addEventListener('DOMContentLoaded', loadFavorites);
