const USER_ID = 1;
function money(value){ return value === null || value === undefined ? 'Liên hệ' : new Intl.NumberFormat('vi-VN').format(value) + ' đ/đêm'; }
function img(room){ return room.thumbnail && room.thumbnail !== '/img' ? room.thumbnail : (room.images?.[0] || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80'); }
function savedIds(){ return new Set(JSON.parse(localStorage.getItem('feelhomeFavoriteRoomIds') || '[]').map(Number)); }
async function loadFavorites(){
  const grid = document.getElementById('favoriteGrid');
  grid.innerHTML = '<div class="empty">Đang tải danh sách yêu thích...</div>';
  try{
    let rooms=[];
    const res=await fetch('/api/favorites/' + USER_ID);
    if(res.ok) rooms=await res.json();
    if(!rooms.length){
      const all=await fetch('/api/public/rooms',{cache:'no-store'}).then(r=>r.ok?r.json():[]);
      const ids=savedIds(); rooms=(Array.isArray(all)?all:[]).filter(room=>ids.has(Number(room.id)));
    }
    if(!rooms.length){ grid.innerHTML = '<div class="empty">Chưa có phòng yêu thích.</div>'; return; }
    grid.innerHTML = rooms.map(room => `<article class="room-card"><div class="room-img"><img src="${img(room)}"></div><div class="room-body"><h3 class="room-title">${room.nameType}</h3><div class="room-price">${money(room.price)}</div><p class="room-desc">${room.description || ''}</p><div class="room-actions"><a href="/room-detail.html?id=${room.id}">Chi tiết</a><button onclick="openRemoveFavorite(${room.id})">Hủy lưu</button></div></div></article>`).join('');
  }catch(e){ grid.innerHTML = '<div class="empty">Không thể tải danh sách yêu thích.</div>'; }
}
let pendingRemoveId=null;
function openRemoveFavorite(roomTypeId){
  pendingRemoveId=Number(roomTypeId); const modal=document.getElementById('favConfirm');
  document.getElementById('favConfirmIcon').textContent='☹';
  document.getElementById('favConfirmTitle').textContent='Bạn muốn hủy lưu phòng này?';
  document.getElementById('favConfirmText').textContent='Phòng sẽ bị xóa khỏi danh sách yêu thích của bạn.';
  document.getElementById('favConfirmActions').innerHTML='<button type="button" class="fav-confirm__cancel" data-fav-close>Giữ lại</button><button type="button" class="fav-confirm__danger" id="confirmRemoveFavorite">Xác nhận hủy</button>';
  modal.classList.add('show'); modal.setAttribute('aria-hidden','false'); bindModalButtons();
}
function closeRemoveFavorite(){ const modal=document.getElementById('favConfirm'); modal?.classList.remove('show'); modal?.setAttribute('aria-hidden','true'); pendingRemoveId=null; }
async function confirmRemoveFavorite(){
  if(pendingRemoveId==null)return; const id=pendingRemoveId;
  const ids=savedIds(); ids.delete(id); localStorage.setItem('feelhomeFavoriteRoomIds',JSON.stringify([...ids]));
  try{await fetch(`/api/favorites?userId=${USER_ID}&roomTypeId=${id}`,{method:'DELETE'});}catch(e){}
  document.getElementById('favConfirmIcon').textContent='✕';
  document.getElementById('favConfirmTitle').textContent='Đã hủy lưu thành công';
  document.getElementById('favConfirmText').textContent='Phòng đã được xóa khỏi danh sách yêu thích.';
  document.getElementById('favConfirmActions').innerHTML='<button type="button" class="fav-confirm__danger" data-fav-close>OK</button>';
  pendingRemoveId=null; bindModalButtons(); loadFavorites();
}
function bindModalButtons(){ document.querySelectorAll('[data-fav-close]').forEach(x=>x.onclick=closeRemoveFavorite); const c=document.getElementById('confirmRemoveFavorite'); if(c)c.onclick=confirmRemoveFavorite; }
document.addEventListener('DOMContentLoaded',()=>{ loadFavorites(); bindModalButtons(); });
