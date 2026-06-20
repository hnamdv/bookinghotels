const USER_ID = 1;

const state = {
  keyword: '',
  minPrice: '',
  maxPrice: '',
  capacity: '',
  bed: '',
  hasWifi: false,
  hasBathtub: false,
  hasBalcony: false,
  hotelId: ''
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

let heroSliderTimer = null;
let heroSliderIndex = 0;
let currentSlides = [];

function setBrandLogo(headerLogo, circleLogo, siteName){
  document.querySelectorAll('#siteName,.footer-site-name').forEach(el => el.textContent = siteName || 'FEELHOME');

  const headerImage = document.getElementById('siteLogo');
  const headerFallback = document.getElementById('siteLogoFallback');
  if(headerImage){
    if(headerLogo){ headerImage.src = headerLogo; headerImage.hidden = false; if(headerFallback) headerFallback.hidden = true; }
    else { headerImage.hidden = true; if(headerFallback) headerFallback.hidden = false; }
  }

  document.querySelectorAll('.footer-logo-image').forEach(el => {
    if(headerLogo){ el.src = headerLogo; el.hidden = false; }
    else el.hidden = true;
  });
  document.querySelectorAll('.footer-logo-fallback').forEach(el => el.hidden = Boolean(headerLogo));

  const circleWrap = document.getElementById('heroBranchLogoWrap');
  const circleImage = document.getElementById('heroBranchLogo');
  if(circleWrap && circleImage){
    if(circleLogo){ circleImage.src = circleLogo; circleWrap.hidden = false; }
    else circleWrap.hidden = true;
  }
}

function showHeroSlide(index){
  const slides = document.querySelectorAll('.hero__slide');
  const dots = document.querySelectorAll('.hero__dot');
  if(!slides.length) return;
  heroSliderIndex = (index + slides.length) % slides.length;
  slides.forEach((slide, i) => slide.classList.toggle('active', i === heroSliderIndex));
  dots.forEach((dot, i) => dot.classList.toggle('active', i === heroSliderIndex));
}

function startHeroSlider(){
  if(heroSliderTimer) clearInterval(heroSliderTimer);
  if(currentSlides.length > 1){
    heroSliderTimer = setInterval(() => showHeroSlide(heroSliderIndex + 1), 5500);
  }
}

function renderHeroSlides(slides, fallbackBanner){
  currentSlides = Array.isArray(slides) && slides.length ? slides.filter(Boolean) : (fallbackBanner ? [fallbackBanner] : []);
  const container = document.getElementById('heroSlides');
  const dots = document.getElementById('heroDots');
  const prev = document.getElementById('heroPrev');
  const next = document.getElementById('heroNext');
  if(!container || !dots) return;

  if(!currentSlides.length){
    container.innerHTML = '';
    dots.innerHTML = '';
    if(prev) prev.hidden = true;
    if(next) next.hidden = true;
    return;
  }

  container.innerHTML = currentSlides.map((url, index) =>
    `<div class="hero__slide ${index === 0 ? 'active' : ''}" style="background-image:url('${String(url).replace(/'/g, "\\'")}')"></div>`
  ).join('');
  dots.innerHTML = currentSlides.map((_, index) =>
    `<button class="hero__dot ${index === 0 ? 'active' : ''}" type="button" data-index="${index}" aria-label="Đến ảnh ${index + 1}"></button>`
  ).join('');
  heroSliderIndex = 0;
  const showControls = currentSlides.length > 1;
  if(prev) prev.hidden = !showControls;
  if(next) next.hidden = !showControls;
  dots.querySelectorAll('.hero__dot').forEach(dot => dot.addEventListener('click', () => {
    showHeroSlide(Number(dot.dataset.index)); startHeroSlider();
  }));
  startHeroSlider();
}

async function loadSiteConfiguration(){
  try{
    const response = await fetch('/api/public/site');
    if(!response.ok) return;
    const site = await response.json();
    const siteName = site.siteName || 'FEELHOME';

    const applyBrand = hotel => {
      const headerLogo = hotel?.headerLogo || site.headerLogo || '';
      const circleLogo = hotel?.circleLogo || site.circleLogo || headerLogo;
      const slides = hotel?.slides?.length ? hotel.slides : site.slides;
      const fallbackBanner = hotel?.banner || site.banner || '';
      setBrandLogo(headerLogo, circleLogo, hotel?.name || siteName);
      renderHeroSlides(slides, fallbackBanner);
      const eyebrow = document.querySelector('.hero__eyebrow');
      if(eyebrow) eyebrow.textContent = hotel ? `Chào mừng đến ${hotel.name}` : `Chào mừng đến ${siteName}`;
    };

    applyBrand(null);

    const select = document.getElementById('hotelId');
    if(select && Array.isArray(site.hotels)){
      site.hotels.forEach(h => {
        const option = document.createElement('option');
        option.value = h.id;
        option.textContent = h.name;
        select.appendChild(option);
      });
      select.addEventListener('change', () => {
        state.hotelId = select.value;
        const hotel = site.hotels.find(h => String(h.id) === String(select.value));
        applyBrand(hotel || null);
        loadRooms();
      });
    }
  }catch(e){ console.warn('Không tải được cấu hình giao diện', e); }
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('heroPrev')?.addEventListener('click', () => { showHeroSlide(heroSliderIndex - 1); startHeroSlider(); });
  document.getElementById('heroNext')?.addEventListener('click', () => { showHeroSlide(heroSliderIndex + 1); startHeroSlider(); });
  document.getElementById('heroSlider')?.addEventListener('mouseenter', () => { if(heroSliderTimer) clearInterval(heroSliderTimer); });
  document.getElementById('heroSlider')?.addEventListener('mouseleave', startHeroSlider);
  loadSiteConfiguration();
});
