const USER_ID = 1;
const money = value => value == null ? 'Liên hệ' : new Intl.NumberFormat('vi-VN').format(Math.round(value)) + ' đ/đêm';
const fallback = () => 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80';
const esc = value => String(value ?? '').replace(/[&<>'"]/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[ch]));


function promoDeadline(endDate,endTime){
  if(!endDate) return null;
  const target=new Date(`${endDate}T${endTime || '23:59:59'}`);
  return Number.isNaN(target.getTime())?null:target;
}
function formatCountdown(ms){
  const total=Math.max(0,Math.floor(ms/1000));
  const days=Math.floor(total/86400),hours=Math.floor((total%86400)/3600),minutes=Math.floor((total%3600)/60),seconds=total%60;
  if(days>0)return `Còn ${days} ngày ${hours} giờ`;
  if(hours>0)return `Còn ${hours} giờ ${minutes} phút`;
  return `Còn ${minutes} phút ${String(seconds).padStart(2,'0')} giây`;
}
function detailPromoCountdown(offer){
  if(!offer?.endDate)return '';
  return `<div class="promo-countdown detail-promo-countdown" data-promo-end="${esc(offer.endDate)}" data-promo-end-time="${esc(offer.endTime || '23:59:59')}"><i class="bi bi-clock-history"></i><span>Đang tính thời gian ưu đãi...</span></div>`;
}
function startDetailPromoCountdown(){
  const element=document.querySelector('.detail-promo-countdown[data-promo-end]');
  if(!element)return;
  const deadline=promoDeadline(element.dataset.promoEnd,element.dataset.promoEndTime),text=element.querySelector('span');
  if(!deadline||!text)return;
  const tick=()=>{
    const remaining=deadline.getTime()-Date.now();
    if(remaining<=0){
      text.textContent='Ưu đãi đã kết thúc';
      element.classList.add('is-expired');
      clearInterval(window.__detailPromoTimer);
      setTimeout(()=>location.reload(),700);
    }else text.textContent=formatCountdown(remaining);
  };
  clearInterval(window.__detailPromoTimer);tick();window.__detailPromoTimer=setInterval(tick,1000);
}
function ensureFavoriteModal(){
  let modal=document.getElementById('favoriteSuccessModal');
  if(modal)return modal;
  document.body.insertAdjacentHTML('beforeend',`<div class="favorite-success-modal" id="favoriteSuccessModal" aria-hidden="true"><div class="favorite-success-card" role="dialog" aria-modal="true"><button class="favorite-success-close" type="button" aria-label="Đóng">&times;</button><div class="favorite-success-check"><i class="bi bi-check-lg"></i></div><h3>Đã lưu phòng yêu thích thành công</h3><p>Phòng đã được thêm vào danh sách yêu thích của bạn.</p><button class="favorite-success-ok" type="button">OK</button></div></div>`);
  modal=document.getElementById('favoriteSuccessModal');
  const close=()=>{modal.classList.remove('show');modal.setAttribute('aria-hidden','true')};
  modal.querySelector('.favorite-success-close')?.addEventListener('click',close);modal.querySelector('.favorite-success-ok')?.addEventListener('click',close);modal.addEventListener('click',e=>{if(e.target===modal)close()});return modal;
}
function showFavoriteSuccess(){
  const modal=ensureFavoriteModal();modal.classList.add('show');modal.setAttribute('aria-hidden','false');
  if(window.gsap){gsap.fromTo(modal.querySelector('.favorite-success-card'),{scale:.72,rotateY:-90,opacity:0},{scale:1,rotateY:0,opacity:1,duration:.55,ease:'back.out(1.7)'});gsap.fromTo(modal.querySelector('.favorite-success-check'),{scale:0,rotate:-180},{scale:1,rotate:0,duration:.65,delay:.15,ease:'back.out(2)'});}
}
function rememberFavorite(id){const values=new Set(JSON.parse(localStorage.getItem('feelhomeFavoriteRoomIds')||'[]').map(Number));values.add(Number(id));localStorage.setItem('feelhomeFavoriteRoomIds',JSON.stringify([...values]));}

function selectedDates(){
  const params=new URLSearchParams(location.search);
  const checkin=params.get('checkin')||sessionStorage.getItem('bookingCheckin')||sessionStorage.getItem('feelhomeCheckIn');
  const checkout=params.get('checkout')||sessionStorage.getItem('bookingCheckout')||sessionStorage.getItem('feelhomeCheckOut');
  return {checkin,checkout};
}

async function fetchJson(url, timeoutMs = 9000){
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try{
    const response = await fetch(url, {cache:'no-store', signal:controller.signal});
    if(!response.ok) throw new Error('HTTP ' + response.status);
    return await response.json();
  }finally{
    clearTimeout(timer);
  }
}

async function fetchRoomDetail(id){
  try{
    return await fetchJson('/api/public/rooms/' + encodeURIComponent(id));
  }catch(primaryError){
    console.warn('Endpoint chi tiết lỗi, chuyển sang danh sách phòng', primaryError);
    const rooms = await fetchJson('/api/public/rooms');
    const room = (Array.isArray(rooms) ? rooms : []).find(item => Number(item.id) === Number(id));
    if(!room) throw primaryError;
    return room;
  }
}

function findBestOffer(offers,id){
  return (Array.isArray(offers)?offers:[])
    .filter(offer=>Number(offer.roomTypeId)===Number(id))
    .sort((a,b)=>Number(b.discountPercent||0)-Number(a.discountPercent||0))[0] || null;
}

async function applyDetailBrand(room){
  try{
    const response=await fetch('/api/public/site',{cache:'no-store'});
    if(!response.ok)return;
    const site=await response.json();
    const hotel=site.hotels?.find(h=>Number(h.id)===Number(room.hotelId)) || site.hotels?.find(h=>h.name===room.hotelName);
    const logo=site.circleLogo||site.headerLogo||hotel?.circleLogo||hotel?.headerLogo||room.hotelLogo||'';
    const image=document.getElementById('detailSiteLogo'), fallbackIcon=document.getElementById('detailLogoFallback'), name=document.getElementById('detailSiteName');
    if(name)name.textContent=hotel?.name||room.hotelName||site.siteName||'FEELHOME';
    if(image){if(logo){image.src=logo;image.hidden=false;if(fallbackIcon)fallbackIcon.hidden=true}else{image.hidden=true;if(fallbackIcon)fallbackIcon.hidden=false}}
  }catch(error){console.warn('Không tải được nhận diện',error);}
}

function bookingUrl(roomId){
  const params=new URLSearchParams({roomTypeId:String(roomId)});
  const {checkin,checkout}=selectedDates();
  if(checkin)params.set('checkin',checkin);
  if(checkout)params.set('checkout',checkout);
  return '/booking/check?'+params.toString();
}

async function loadDetail(){
  const page=document.getElementById('detailPage');
  const params = new URLSearchParams(location.search);
  let id = params.get('id');
  if(!id){
    const parts = location.pathname.split('/').filter(Boolean);
    if(parts[0] === 'roomdetail' && parts[1]) id = parts[1];
  }
  if(!id){page.innerHTML='<div class="empty">Thiếu mã loại phòng.</div>';return;}
  try{
    const room=await fetchRoomDetail(id);
    const {checkin,checkout}=selectedDates();
    let offers=[];
    const offerParams=new URLSearchParams(); if(checkin)offerParams.set('checkin',checkin); if(checkout)offerParams.set('checkout',checkout);
    try{offers=await fetchJson('/api/public/offers'+(offerParams.toString()?'?'+offerParams:''),7000);}catch(ignore){console.warn('Không tải được ưu đãi',ignore);}
    let availability=null;
    if(checkin&&checkout){try{availability=await fetchJson(`/api/public/rooms/${id}/availability?checkin=${encodeURIComponent(checkin)}&checkout=${encodeURIComponent(checkout)}`,7000);}catch(ignore){console.warn('Không tải được tình trạng phòng',ignore);}}
    const offer=findBestOffer(offers,id);
    const images=room.images?.filter(Boolean)?.length?room.images.filter(Boolean):[fallback()];
    const price=offer
      ? `<div class="detail-price"><del>${money(offer.originalPrice)}</del><strong>${money(offer.discountedPrice)}</strong><span>-${Math.round(offer.discountPercent)}%</span></div><p class="detail-price-note">Giá ưu đãi đã được áp dụng đồng nhất với trang danh sách.</p>`
      : `<div class="detail-price"><strong>${money(room.price)}</strong></div>`;
    page.innerHTML=`
      <div class="detail-breadcrumb"><a href="/home">Trang chủ</a><i class="bi bi-chevron-right"></i><span>${esc(room.nameType||'Chi tiết phòng')}</span></div>
      <div class="detail-box">
        <div class="detail-gallery">
          <div class="detail-main-img"><img id="mainImg" src="${esc(images[0])}" alt="${esc(room.nameType||'Phòng nghỉ')}">${offer?`<span class="detail-promo-flag">-${Math.round(offer.discountPercent)}%</span>`:''}</div>
          <div class="thumbs">${images.map((img,i)=>`<button class="thumb ${i?'':'active'}" data-src="${esc(img)}" type="button"><img src="${esc(img)}" alt="Ảnh phòng ${i+1}"></button>`).join('')}</div>
        </div>
        <div class="detail-info">
          <p class="detail-kicker">${esc(room.hotelName||'FeelHome Hotel')}</p>
          <h1>${esc(room.nameType||'Chi tiết phòng')}</h1>
          ${price}
          <div class="detail-facts"><span><i class="bi bi-people"></i>${room.capacity||1} khách</span><span><i class="bi bi-bed"></i>${esc(room.bed||'Tiêu chuẩn')}</span>${room.area?`<span><i class="bi bi-aspect-ratio"></i>${room.area} m²</span>`:''}</div>
          ${offer?`<div class="detail-offer"><i class="bi bi-gift"></i><div><b>${esc(offer.promotionName||'Ưu đãi hiện hành')}</b><small>Giảm ${Math.round(offer.discountPercent)}% từ ${money(offer.originalPrice)} còn ${money(offer.discountedPrice)}</small>${detailPromoCountdown(offer)}</div></div>`:''}
          <p class="detail-description">${esc(room.description||'Không gian nghỉ dưỡng tiện nghi, sang trọng và yên tĩnh.')}</p>
          <div class="amenities">${room.hasWifi?'<span><i class="bi bi-wifi"></i>Wifi</span>':''}${room.hasBathtub?'<span><i class="bi bi-droplet"></i>Bồn tắm</span>':''}${room.hasBalcony?'<span><i class="bi bi-sun"></i>Ban công</span>':''}${room.hasTv?'<span><i class="bi bi-tv"></i>TV</span>':''}</div>
          <div class="hotel-card"><h3>Thông tin khách sạn</h3><p><b>${esc(room.hotelName||'FeelHome Hotel')}</b></p><p>${esc(room.hotelAddress||'Đang cập nhật địa chỉ')}</p>${room.hotelPhone?`<p><i class="bi bi-telephone"></i> ${esc(room.hotelPhone)}</p>`:''}${room.hotelEmail?`<p><i class="bi bi-envelope"></i> ${esc(room.hotelEmail)}</p>`:''}</div>
          ${availability?`<div class="detail-availability ${availability.available?'is-available':'is-unavailable'}"><b>${availability.available?`Còn ${availability.availableRooms}/${availability.totalRooms} phòng`:'Đã hết phòng'}</b><small>${checkin} đến ${checkout}</small></div>`:`<div class="detail-availability is-pending"><b>Chọn ngày để kiểm tra phòng trống</b></div>`}
          <div class="detail-actions"><a class="fh-btn-primary detail-book ${availability&&availability.available===false?'is-disabled':''}" href="${availability&&availability.available===false?'#':bookingUrl(room.id)}"><i class="bi bi-calendar-check"></i> ${availability&&availability.available===false?'Đã hết phòng':'Đặt phòng'}</a><button class="favorite-sweep-button detail-favorite" type="button" onclick="saveFavorite(${room.id})" aria-label="Lưu phòng yêu thích"><span class="favorite-sweep-button__text">Lưu yêu thích</span><span class="favorite-sweep-button__icon"><i class="bi bi-heart"></i></span></button></div>
        </div>
      </div>`;
    document.querySelectorAll('.thumb').forEach(button=>button.onclick=()=>{const main=document.getElementById('mainImg');if(main)main.src=button.dataset.src;document.querySelectorAll('.thumb').forEach(item=>item.classList.remove('active'));button.classList.add('active');});
    const contact=document.getElementById('detailHotelContact');
    if(contact)contact.textContent=[room.hotelName,room.hotelPhone,room.hotelEmail].filter(Boolean).join(' · ')||'Thông tin liên hệ đang cập nhật.';
    await applyDetailBrand(room);
    startDetailPromoCountdown();
    if(window.gsap){
      gsap.fromTo('.detail-gallery',{x:-24,opacity:0},{x:0,opacity:1,duration:.65,ease:'power3.out',clearProps:'transform,opacity'});
      gsap.fromTo('.detail-info',{x:24,opacity:0},{x:0,opacity:1,duration:.65,delay:.08,ease:'power3.out',clearProps:'transform,opacity'});
    }
  }catch(error){console.error(error);page.innerHTML='<div class="empty"><b>Không thể tải dữ liệu chi tiết.</b><br>Vui lòng tải lại trang hoặc quay về <a href="/home#rooms-section">danh sách phòng</a>.</div>';}
}

async function saveFavorite(id){
  rememberFavorite(id);
  try{await fetch(`/api/favorites?userId=${USER_ID}&roomTypeId=${id}`,{method:'POST'});}catch(error){console.warn('API yêu thích chưa phản hồi, đã lưu tạm trên trình duyệt.',error);}
  showFavoriteSuccess();
}

document.addEventListener('DOMContentLoaded',()=>{
  const hamburger=document.getElementById('fh-hamburger'),nav=document.getElementById('fh-nav');
  hamburger?.addEventListener('click',()=>{nav?.classList.toggle('open');hamburger.classList.toggle('active');});
  loadDetail();
});
