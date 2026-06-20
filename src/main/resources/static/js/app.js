const USER_ID = 1;
const state = { keyword:'', minPrice:'', maxPrice:'', capacity:'', bed:'', hasWifi:false, hasBathtub:false, hasBalcony:false, hotelId:'' };
let activeOffers = new Map();
let siteConfig = null;
let heroSliderTimer = null;
let heroSliderIndex = 0;
let currentSlides = [];
let showcaseIndex = 0;
let allRooms = [];
let filtersTouched = false;

const money = value => value == null ? 'Liên hệ' : new Intl.NumberFormat('vi-VN').format(Math.round(value)) + ' đ/đêm';
const esc = value => String(value ?? '').replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));
const imageOf = room => room.thumbnail && room.thumbnail !== '/img'
  ? room.thumbnail
  : (room.images?.[0] || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80');

function buildQuery(){
  const params = new URLSearchParams();
  Object.entries(state).forEach(([key,value]) => {
    if(value !== '' && value !== false && value != null) params.append(key,value);
  });
  return params.toString();
}

function bestOffersByRoom(data){
  const map = new Map();
  for(const offer of Array.isArray(data) ? data : []){
    const key = Number(offer.roomTypeId);
    const current = map.get(key);
    if(!current || Number(offer.discountPercent || 0) > Number(current.discountPercent || 0)) map.set(key, offer);
  }
  return map;
}

async function loadOfferMap(){
  try{
    const response = await fetch('/api/public/offers', {cache:'no-store'});
    if(!response.ok) return;
    activeOffers = bestOffersByRoom(await response.json());
  }catch(error){ console.warn('Không tải được ưu đãi', error); }
}

function priceBlock(room){
  const offer = activeOffers.get(Number(room.id));
  if(!offer) return `<div class="flip-price"><strong>${money(room.price)}</strong></div>`;
  return `<div class="flip-price flip-price--promo"><del>${money(offer.originalPrice)}</del><strong>${money(offer.discountedPrice)}</strong><small>Đã giảm ${Math.round(offer.discountPercent)}%</small></div>`;
}

function promoFlag(room){
  const offer = activeOffers.get(Number(room.id));
  return offer ? `<span class="promo-flag">-${Math.round(offer.discountPercent)}%</span>` : '';
}

function roomCard(room){
  const image = esc(imageOf(room));
  const title = esc(room.nameType || 'Phòng nghỉ');
  const hotel = esc(room.hotelName || 'FeelHome Hotel');
  const description = esc(room.description || 'Không gian nghỉ dưỡng tiện nghi, riêng tư và được chăm chút cho kỳ nghỉ của bạn.');
  return `<article class="fh-flip-card reveal-card" data-room-id="${room.id}" tabindex="0" aria-label="${title}">
    <div class="fh-flip-card__inner">
      <div class="fh-flip-card__front">
        <img src="${image}" alt="${title}" loading="lazy" onerror="this.onerror=null;this.src='https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&amp;fit=crop&amp;w=900&amp;q=80'">
        <span class="fh-flip-card__shade"></span>
        ${promoFlag(room)}
        <div class="fh-flip-card__front-copy"><small>${hotel}</small><h3>${title}</h3><span>Di chuột để xem chi tiết <i class="bi bi-arrow-repeat"></i></span></div>
      </div>
      <div class="fh-flip-card__back">
        <div class="fh-flip-card__glow"></div>
        <div class="fh-flip-card__back-content">
          <small class="flip-hotel">${hotel}</small>
          <h3>${title}</h3>
          <div class="flip-meta">
            <span><i class="bi bi-people"></i>${room.capacity || 1} khách</span>
            <span><i class="bi bi-bed"></i>${esc(room.bed || 'Tiêu chuẩn')}</span>
            ${room.area ? `<span><i class="bi bi-aspect-ratio"></i>${room.area} m²</span>` : ''}
          </div>
          ${priceBlock(room)}
          <p>${description}</p>
          <div class="flip-actions">
            <a class="flip-detail-link" href="/room-detail.html?id=${room.id}" data-room-id="${room.id}">Xem chi tiết <i class="bi bi-arrow-up-right"></i></a>
            <button class="flip-favorite-button" type="button" data-room-id="${room.id}"><i class="bi bi-heart"></i> Lưu</button>
          </div>
        </div>
      </div>
    </div>
  </article>`;
}

function uniqueRooms(rooms){
  const byId = new Map();
  for(const room of Array.isArray(rooms) ? rooms : []){
    if(room?.id != null && !byId.has(Number(room.id))) byId.set(Number(room.id), room);
  }
  return [...byId.values()];
}

function matchesRoomFilters(room){
  const keyword = String(state.keyword || '').trim().toLowerCase();
  const bed = String(state.bed || '').trim().toLowerCase();
  const price = Number(room.price || 0);
  if(state.hotelId && String(room.hotelId ?? room.hotelsId ?? '') !== String(state.hotelId)) return false;
  if(keyword){
    const haystack = [room.nameType, room.description, room.hotelName, room.hotelAddress, room.bed].filter(Boolean).join(' ').toLowerCase();
    if(!haystack.includes(keyword)) return false;
  }
  if(state.minPrice !== '' && price < Number(state.minPrice)) return false;
  if(state.maxPrice !== '' && price > Number(state.maxPrice)) return false;
  if(state.capacity !== '' && Number(room.capacity || 0) < Number(state.capacity)) return false;
  if(bed && !String(room.bed || '').toLowerCase().includes(bed)) return false;
  if(state.hasWifi && !room.hasWifi) return false;
  if(state.hasBathtub && !room.hasBathtub) return false;
  if(state.hasBalcony && !room.hasBalcony) return false;
  return true;
}

function bindRoomCards(grid){
  const touchMode = window.matchMedia('(hover: none), (pointer: coarse)').matches;
  grid.querySelectorAll('.fh-flip-card').forEach(card => {
    card.querySelectorAll('a,button').forEach(control => {
      control.addEventListener('pointerdown', event => event.stopPropagation());
      control.addEventListener('click', event => event.stopPropagation());
    });
    card.querySelector('.flip-favorite-button')?.addEventListener('click', event => {
      event.preventDefault();
      saveFavorite(Number(event.currentTarget.dataset.roomId));
    });
    if(touchMode){
      card.addEventListener('click', event => {
        if(event.target.closest('a,button')) return;
        card.classList.toggle('is-flipped');
      });
      card.addEventListener('keydown', event => {
        if((event.key === 'Enter' || event.key === ' ') && !event.target.closest('a,button')){
          event.preventDefault();
          card.classList.toggle('is-flipped');
        }
      });
    }
  });
}

function renderRooms(){
  const grid = document.getElementById('roomGrid');
  const count = document.getElementById('roomResultCount');
  if(!grid) return;
  const rooms = allRooms.filter(matchesRoomFilters);
  if(count) count.textContent = `${rooms.length} loại phòng phù hợp`;
  if(!rooms.length){ grid.innerHTML = '<div class="empty">Không tìm thấy phòng phù hợp.</div>'; return; }
  grid.replaceChildren();
  const fragment = document.createDocumentFragment();
  rooms.forEach(room => {
    const holder = document.createElement('div');
    holder.innerHTML = roomCard(room).trim();
    const card = holder.firstElementChild;
    if(card) fragment.appendChild(card);
  });
  grid.appendChild(fragment);
  bindRoomCards(grid);
  requestAnimationFrame(() => {
    grid.querySelectorAll('.fh-flip-card').forEach(card => {
      card.style.opacity = '1';
      card.style.visibility = 'visible';
    });
    animateDynamicCards();
  });
}

async function loadRooms(){
  const grid = document.getElementById('roomGrid');
  const count = document.getElementById('roomResultCount');
  if(!grid) return;
  grid.innerHTML = '<div class="empty">Đang tải danh sách phòng...</div>';
  if(count) count.textContent = 'Đang tải danh sách phòng...';
  try{
    const response = await fetch('/api/public/rooms', {cache:'no-store'});
    if(!response.ok) throw new Error('HTTP ' + response.status);
    allRooms = uniqueRooms(await response.json());
    renderRooms();
  }catch(error){
    console.error(error);
    if(count) count.textContent = 'Không thể tải danh sách phòng';
    grid.innerHTML = '<div class="empty">Không thể tải dữ liệu phòng.</div>';
  }
}

async function saveFavorite(id){
  try{
    const response = await fetch(`/api/favorites?userId=${USER_ID}&roomTypeId=${id}`, {method:'POST'});
    alert(response.ok ? 'Đã lưu phòng yêu thích' : 'Không thể lưu phòng yêu thích');
  }catch(error){ alert('Lỗi kết nối API yêu thích'); }
}

function syncFilters(){
  ['keyword','minPrice','maxPrice','capacity','bed'].forEach(id => state[id] = document.getElementById(id)?.value || '');
  state.capacity = state.capacity || document.getElementById('guestCount')?.value || '';
  ['hasWifi','hasBathtub','hasBalcony'].forEach(id => state[id] = document.getElementById(id)?.checked || false);
}

function setBrandLogo(logo, siteName){
  document.querySelectorAll('#siteName,.footer-site-name').forEach(element => element.textContent = siteName || 'FEELHOME');
  const headerImage = document.getElementById('siteLogo');
  const fallback = document.getElementById('siteLogoFallback');
  if(headerImage){
    if(logo){ headerImage.src = logo; headerImage.hidden = false; if(fallback) fallback.hidden = true; }
    else { headerImage.hidden = true; if(fallback) fallback.hidden = false; }
  }
  document.querySelectorAll('.footer-logo-image').forEach(element => { if(logo){element.src=logo;element.hidden=false}else element.hidden=true; });
  document.querySelectorAll('.footer-logo-fallback').forEach(element => element.hidden = !!logo);
  const wrap = document.getElementById('heroBranchLogoWrap');
  const image = document.getElementById('heroBranchLogo');
  if(wrap && image){ if(logo){image.src=logo;wrap.hidden=false}else wrap.hidden=true; }
}

function showHeroSlide(index){
  const slides = [...document.querySelectorAll('.hero__slide')];
  const dots = [...document.querySelectorAll('.hero__dot')];
  if(!slides.length) return;
  heroSliderIndex = (index + slides.length) % slides.length;
  slides.forEach((slide,i) => slide.classList.toggle('active', i === heroSliderIndex));
  dots.forEach((dot,i) => dot.classList.toggle('active', i === heroSliderIndex));
}
function startHeroSlider(){ clearInterval(heroSliderTimer); if(currentSlides.length > 1) heroSliderTimer = setInterval(() => showHeroSlide(heroSliderIndex + 1), 5500); }
function renderHeroSlides(slides, fallback){
  currentSlides = slides?.filter(Boolean)?.length ? slides.filter(Boolean) : (fallback ? [fallback] : []);
  const container = document.getElementById('heroSlides');
  const dots = document.getElementById('heroDots');
  if(!container || !dots) return;
  container.innerHTML = currentSlides.map((url,i) => `<div class="hero__slide ${i ? '' : 'active'}" style="background-image:url('${String(url).replace(/'/g,"\\'")}')"></div>`).join('');
  dots.innerHTML = currentSlides.map((_,i) => `<button class="hero__dot ${i ? '' : 'active'}" data-index="${i}" aria-label="Slide ${i+1}"></button>`).join('');
  dots.querySelectorAll('button').forEach(button => button.onclick = () => { showHeroSlide(+button.dataset.index); startHeroSlider(); });
  const prev = document.getElementById('heroPrev'), next = document.getElementById('heroNext');
  if(prev) prev.hidden = currentSlides.length < 2;
  if(next) next.hidden = currentSlides.length < 2;
  renderShowcase(currentSlides); renderStory(currentSlides); startHeroSlider();
}
function renderShowcase(slides){
  const track = document.getElementById('showcaseTrack'), dots = document.getElementById('showcaseDots');
  if(!track || !dots) return;
  const items = (slides?.length ? slides : []).slice(0,8);
  track.innerHTML = items.map((url,i) => `<article class="showcase-slide ${i===0?'active':''}"><img src="${esc(url)}" alt="Không gian FeelHome ${i+1}"><div><small>FEELHOME COLLECTION</small><h3>${['Nơi bình yên bắt đầu','Một góc nghỉ ngơi riêng tư','Chạm vào cảm giác thư thái','Mỗi chi tiết đều có câu chuyện'][i%4]}</h3></div></article>`).join('');
  dots.innerHTML = items.map((_,i) => `<button class="${i===0?'active':''}" data-i="${i}"></button>`).join('');
  dots.querySelectorAll('button').forEach(button => button.onclick = () => showShowcase(+button.dataset.i));
  showShowcase(0);
}
function showShowcase(index){
  const slides=[...document.querySelectorAll('.showcase-slide')], dots=[...document.querySelectorAll('.showcase-dots button')];
  if(!slides.length) return;
  showcaseIndex=(index+slides.length)%slides.length;
  slides.forEach((slide,i)=>slide.classList.toggle('active',i===showcaseIndex));
  dots.forEach((dot,i)=>dot.classList.toggle('active',i===showcaseIndex));
  if(window.gsap) gsap.fromTo(slides[showcaseIndex],{opacity:0,x:30},{opacity:1,x:0,duration:.65,ease:'power3.out'});
}
function renderStory(slides){
  const box=document.getElementById('storyStack'); if(!box)return;
  const fallback=['https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=900&q=80','https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=900&q=80','https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=900&q=80'];
  const items=[...(slides||[]),...fallback].slice(0,3);
  box.innerHTML=items.map((url,i)=>`<figure class="story-photo story-photo-${i+1}"><img src="${esc(url)}" alt="Câu chuyện FeelHome"></figure>`).join('');
}
async function loadSiteConfiguration(){
  try{
    const response=await fetch('/api/public/site',{cache:'no-store'}); if(!response.ok)return;
    siteConfig=await response.json();
    const apply=hotel=>{
      const logo=hotel?.circleLogo||hotel?.headerLogo||siteConfig.circleLogo||siteConfig.headerLogo||'';
      const slides=hotel?.slides?.length?hotel.slides:siteConfig.slides;
      setBrandLogo(logo,hotel?.name||siteConfig.siteName);
      renderHeroSlides(slides,hotel?.banner||siteConfig.banner);
      const eyebrow=document.getElementById('heroWelcomeText')||document.querySelector('.hero__eyebrow');
      if(eyebrow){
        eyebrow.textContent=hotel?.welcomeText||siteConfig.welcomeText||(hotel?`Chào mừng đến ${hotel.name}`:`Chào mừng đến ${siteConfig.siteName||'FeelHome'}`);
        eyebrow.style.setProperty('--welcome-color', hotel?.welcomeColor||siteConfig.welcomeColor||'#d7b34f');
        eyebrow.classList.remove('hero__welcome--shine','hero__welcome--glow','hero__welcome--none');
        eyebrow.classList.add(`hero__welcome--${hotel?.welcomeEffect||siteConfig.welcomeEffect||'shine'}`);
      }
    };
    apply(null);
    const select=document.getElementById('hotelId');
    siteConfig.hotels?.forEach(hotel=>select?.insertAdjacentHTML('beforeend',`<option value="${hotel.id}">${esc(hotel.name)}</option>`));
    select?.addEventListener('change',()=>{state.hotelId=select.value;apply(siteConfig.hotels.find(h=>String(h.id)===String(select.value))||null);renderRooms();});
  }catch(error){console.warn(error);}
}
function initAnimations(){
  if(!window.gsap)return;
  gsap.registerPlugin(ScrollTrigger);
  gsap.from('.hero__eyebrow',{y:18,opacity:0,duration:.7});
  gsap.from('.hero__title',{y:35,opacity:0,duration:1,delay:.1,ease:'power3.out'});
  gsap.from('.hero__sub,.search-box',{y:28,opacity:0,duration:.8,delay:.35,stagger:.12});
  document.querySelectorAll('.reveal').forEach(el=>gsap.from(el,{scrollTrigger:{trigger:el,start:'top 86%'},y:40,opacity:0,duration:.8,ease:'power3.out'}));
  gsap.to('.story-photo-1',{scrollTrigger:{trigger:'.story',start:'top bottom',end:'bottom top',scrub:1},y:-35,rotate:-3});
  gsap.to('.story-photo-3',{scrollTrigger:{trigger:'.story',start:'top bottom',end:'bottom top',scrub:1},y:35,rotate:4});
}
function animateDynamicCards(){
  document.querySelectorAll('#roomGrid .reveal-card').forEach((card,index) => {
    card.style.opacity = '1';
    card.style.visibility = 'visible';
    card.style.transform = 'none';
    card.style.animationDelay = `${Math.min(index * 60, 300)}ms`;
    card.classList.add('room-card-ready');
  });
}

document.addEventListener('DOMContentLoaded',async()=>{
  const hamburger=document.getElementById('fh-hamburger'), nav=document.getElementById('fh-nav');
  hamburger?.addEventListener('click',()=>{nav?.classList.toggle('open');hamburger.classList.toggle('active');});
  document.querySelectorAll('.fh-nav__link').forEach(link=>link.onclick=()=>nav?.classList.remove('open'));
  document.getElementById('quickSearchForm')?.addEventListener('submit',event=>{event.preventDefault();filtersTouched=true;document.getElementById('capacity').value=document.getElementById('guestCount').value;syncFilters();renderRooms();document.getElementById('rooms-section')?.scrollIntoView({behavior:'smooth'});});
  ['keyword','minPrice','maxPrice','capacity','bed','hasWifi','hasBathtub','hasBalcony'].forEach(id=>{
    document.getElementById(id)?.addEventListener('input',()=>{filtersTouched=true;syncFilters();renderRooms();});
    document.getElementById(id)?.addEventListener('change',()=>{filtersTouched=true;syncFilters();renderRooms();});
  });
  document.getElementById('clearFilter')?.addEventListener('click',()=>{filtersTouched=false;['keyword','minPrice','maxPrice','capacity','bed'].forEach(id=>document.getElementById(id).value='');['hasWifi','hasBathtub','hasBalcony'].forEach(id=>document.getElementById(id).checked=false);state.keyword='';state.minPrice='';state.maxPrice='';state.capacity='';state.bed='';state.hasWifi=false;state.hasBathtub=false;state.hasBalcony=false;renderRooms();});
  document.getElementById('heroPrev')?.addEventListener('click',()=>{showHeroSlide(heroSliderIndex-1);startHeroSlider();});
  document.getElementById('heroNext')?.addEventListener('click',()=>{showHeroSlide(heroSliderIndex+1);startHeroSlider();});
  document.getElementById('showcasePrev')?.addEventListener('click',()=>showShowcase(showcaseIndex-1));
  document.getElementById('showcaseNext')?.addEventListener('click',()=>showShowcase(showcaseIndex+1));
  await loadOfferMap();
  await loadSiteConfiguration();
  await loadRooms();
  initAnimations();
});
