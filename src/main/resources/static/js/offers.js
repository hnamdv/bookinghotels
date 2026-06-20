function offerMoney(value){
  if(value === null || value === undefined) return 'Liên hệ';
  return new Intl.NumberFormat('vi-VN').format(Math.round(value)) + ' đ';
}
function offerImage(item){
  return item.image || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80';
}
function offerDate(value){
  if(!value) return 'Không giới hạn';
  const [y,m,d] = value.split('-');
  return `${d}/${m}/${y}`;
}
function offerCard(item){
  return `
    <article class="offer-card">
      <div class="offer-card__image">
        <img src="${offerImage(item)}" alt="${item.roomName || 'Phòng ưu đãi'}" loading="lazy">
        <span class="offer-badge">-${Number(item.discountPercent || 0).toFixed(0)}%</span>
      </div>
      <div class="offer-card__body">
        <p class="offer-kicker">${item.promotionName || 'Ưu đãi đặc biệt'}</p>
        <h3>${item.roomName || 'Phòng nghỉ FeelHome'}</h3>
        <div class="offer-meta">
          ${item.hotelName ? `<span><i class="bi bi-building"></i> ${item.hotelName}</span>` : ''}
          ${item.capacity ? `<span><i class="bi bi-people"></i> ${item.capacity} khách</span>` : ''}
          ${item.bed ? `<span><i class="bi bi-bed"></i> ${item.bed}</span>` : ''}
        </div>
        <p class="offer-description">${item.description || 'Tận hưởng kỳ nghỉ tiện nghi với mức giá ưu đãi dành riêng cho bạn.'}</p>
        <div class="offer-price"><del>${offerMoney(item.originalPrice)}</del><strong>${offerMoney(item.discountedPrice)}<small>/đêm</small></strong></div>
        <div class="offer-valid"><i class="bi bi-calendar3"></i> ${offerDate(item.startDate)} - ${offerDate(item.endDate)}</div>
        <a class="offer-link" href="/room-detail.html?id=${item.roomTypeId}">Xem phòng <i class="bi bi-arrow-right"></i></a>
      </div>
    </article>`;
}
async function loadOffers(){
  const targets = [document.getElementById('homeOffersGrid'), document.getElementById('offersGrid')].filter(Boolean);
  if(!targets.length) return;
  try{
    const response = await fetch('/api/public/offers');
    if(!response.ok) throw new Error('HTTP ' + response.status);
    const offers = await response.json();
    document.getElementById('offerCount') && (document.getElementById('offerCount').textContent = offers.length);
    const homeItems = offers.slice(0, 3);
    targets.forEach(target => {
      const items = target.id === 'homeOffersGrid' ? homeItems : offers;
      target.innerHTML = items.length ? items.map(offerCard).join('') : '<div class="empty">Hiện chưa có chương trình ưu đãi còn hiệu lực.</div>';
    });
  }catch(error){
    targets.forEach(target => target.innerHTML = '<div class="empty">Không thể tải ưu đãi. Vui lòng thử lại sau.</div>');
  }
}
document.addEventListener('DOMContentLoaded', () => {
  const ham = document.getElementById('fh-hamburger');
  const nav = document.getElementById('fh-nav');
  ham?.addEventListener('click', () => nav?.classList.toggle('open'));
  loadOffers().then(() => { if(window.gsap){ gsap.registerPlugin(ScrollTrigger); gsap.from('.page-hero > div',{y:30,opacity:0,duration:.8}); gsap.from('.offer-card',{scrollTrigger:{trigger:'.offers-grid',start:'top 85%'},y:35,opacity:0,duration:.65,stagger:.1,ease:'power3.out'}); } });
});
