const USER_ID = 1;

const state = {
  keyword: '',
  minPrice: '',
  maxPrice: '',
  capacity: '',
  bed: '',
  hasWifi: false,
  hasBathtub: false,
  hasBalcony: false
};

function money(value){
  if(value === null || value === undefined) return 'Liên hệ';
  return new Intl.NumberFormat('vi-VN').format(value) + ' đ/đêm';
}

function imageOf(room){
  if(room.thumbnail && room.thumbnail !== '/img') return room.thumbnail;
  if(room.images && room.images.length) return room.images[0];
  return 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80';
}

function buildQuery(){
  const params = new URLSearchParams();
  Object.entries(state).forEach(([key, value]) => {
    if(value !== '' && value !== false && value !== null && value !== undefined){
      params.append(key, value);
    }
  });
  return params.toString();
}

async function loadRooms(){
  const grid = document.getElementById('roomGrid');
  if(!grid) return;
  grid.innerHTML = '<div class="empty">Đang tải danh sách phòng...</div>';
  try{
    const query = buildQuery();
    const res = await fetch('/api/public/rooms' + (query ? '?' + query : ''));
    const rooms = await res.json();
    if(!rooms.length){
      grid.innerHTML = '<div class="empty">Không tìm thấy phòng phù hợp.</div>';
      return;
    }
    grid.innerHTML = rooms.map(room => `
      <article class="room-card">
        <div class="room-img"><img src="${imageOf(room)}" alt="${room.nameType || 'Phòng nghỉ'}"></div>
        <div class="room-body">
          <h3 class="room-title">${room.nameType || 'Phòng nghỉ'}</h3>
          <div class="room-meta">
            <span><i class="bi bi-people"></i> ${room.capacity || 1} khách</span>
            <span><i class="bi bi-bed"></i> ${room.bed || 'Tiêu chuẩn'}</span>
            ${room.area ? `<span><i class="bi bi-aspect-ratio"></i> ${room.area} m²</span>` : ''}
          </div>
          <div class="room-price">${money(room.price)}</div>
          <p class="room-desc">${room.description || 'Không gian nghỉ dưỡng tiện nghi, phù hợp cho kỳ nghỉ của bạn.'}</p>
          <div class="room-actions">
            <a href="/room-detail.html?id=${room.id}">Chi tiết</a>
            <button onclick="saveFavorite(${room.id})"><i class="bi bi-heart"></i> Lưu</button>
          </div>
        </div>
      </article>
    `).join('');
  }catch(e){
    grid.innerHTML = '<div class="empty">Không thể tải dữ liệu phòng. Kiểm tra API hoặc database.</div>';
  }
}

async function saveFavorite(roomTypeId){
  try{
    const res = await fetch(`/api/favorites?userId=${USER_ID}&roomTypeId=${roomTypeId}`, { method:'POST' });
    if(res.ok) alert('Đã lưu phòng yêu thích');
    else alert('Không thể lưu phòng yêu thích');
  }catch(e){
    alert('Lỗi kết nối API yêu thích');
  }
}

function syncFilters(){
  state.keyword = document.getElementById('keyword')?.value || '';
  state.minPrice = document.getElementById('minPrice')?.value || '';
  state.maxPrice = document.getElementById('maxPrice')?.value || '';
  state.capacity = document.getElementById('capacity')?.value || document.getElementById('guestCount')?.value || '';
  state.bed = document.getElementById('bed')?.value || '';
  state.hasWifi = document.getElementById('hasWifi')?.checked || false;
  state.hasBathtub = document.getElementById('hasBathtub')?.checked || false;
  state.hasBalcony = document.getElementById('hasBalcony')?.checked || false;
}

document.addEventListener('DOMContentLoaded', () => {
  const ham = document.getElementById('fh-hamburger');
  const nav = document.getElementById('fh-nav');
  ham?.addEventListener('click', () => nav?.classList.toggle('open'));

  document.getElementById('quickSearchForm')?.addEventListener('submit', e => {
    e.preventDefault();
    document.getElementById('capacity').value = document.getElementById('guestCount').value;
    syncFilters();
    loadRooms();
    document.getElementById('rooms-section')?.scrollIntoView({behavior:'smooth'});
  });

  ['keyword','minPrice','maxPrice','capacity','bed','hasWifi','hasBathtub','hasBalcony'].forEach(id => {
    document.getElementById(id)?.addEventListener('input', () => { syncFilters(); loadRooms(); });
    document.getElementById(id)?.addEventListener('change', () => { syncFilters(); loadRooms(); });
  });

  document.getElementById('clearFilter')?.addEventListener('click', () => {
    ['keyword','minPrice','maxPrice','capacity','bed'].forEach(id => { const el = document.getElementById(id); if(el) el.value=''; });
    ['hasWifi','hasBathtub','hasBalcony'].forEach(id => { const el = document.getElementById(id); if(el) el.checked=false; });
    syncFilters();
    loadRooms();
  });

  syncFilters();
  loadRooms();
});
