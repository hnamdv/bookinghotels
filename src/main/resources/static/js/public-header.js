(function(){
  const page=location.pathname;
  const active=(href)=> page===href || (href==='/home' && (page==='/'||page==='/client-home.html')) ? ' active' : '';
  const header=document.querySelector('.fh-header');
  if(!header) return;
  header.innerHTML=`<div class="fh-header__inner">
    <a href="/home" class="fh-logo" aria-label="FeelHome - Trang chủ"><img class="fh-logo__image shared-site-logo" alt="Logo FeelHome" hidden><span class="fh-logo__icon shared-logo-fallback">◈</span><span class="fh-logo__text shared-site-name">FEELHOME</span></a>
    <nav class="fh-nav" id="fh-nav">
      <a href="/home" class="fh-nav__link${active('/home')}" data-text="Trang Chủ"><span data-text="Trang Chủ">Trang Chủ</span></a>
      <a href="/home#rooms-section" class="fh-nav__link" data-text="Phòng Nghỉ"><span data-text="Phòng Nghỉ">Phòng Nghỉ</span></a>
      <a href="/offers" class="fh-nav__link${active('/offers')}" data-text="Ưu Đãi"><span data-text="Ưu Đãi">Ưu Đãi</span></a>
      <a href="/home#story" class="fh-nav__link" data-text="Câu Chuyện"><span data-text="Câu Chuyện">Câu Chuyện</span></a>
      <a href="/favorites.html" class="fh-nav__link${active('/favorites.html')}" data-text="Yêu Thích"><span data-text="Yêu Thích">Yêu Thích</span></a>
    </nav>
    <div class="fh-header__actions">
      <a href="/home#rooms-section" class="header-booking-pill" aria-label="Đặt ngay">
        <span class="header-booking-pill__bg"><span class="header-booking-pill__layers"><span class="header-booking-pill__layer header-booking-pill__layer--1"></span><span class="header-booking-pill__layer header-booking-pill__layer--2"></span><span class="header-booking-pill__layer header-booking-pill__layer--3"></span></span></span>
        <span class="header-booking-pill__inner"><span class="header-booking-pill__static">Đặt Ngay</span><span class="header-booking-pill__hover">Đặt Ngay</span></span>
      </a>
      <button class="fh-hamburger" id="fh-hamburger" aria-label="Menu"><span></span><span></span><span></span></button>
    </div>
  </div>`;
  const burger=document.getElementById('fh-hamburger'), nav=document.getElementById('fh-nav');
  burger?.addEventListener('click',()=>nav?.classList.toggle('open'));
  fetch('/api/public/site',{cache:'no-store'}).then(r=>r.ok?r.json():null).then(site=>{
    if(!site)return; document.querySelectorAll('.shared-site-name').forEach(x=>x.textContent=site.siteName||'FEELHOME');
    document.querySelectorAll('.shared-site-logo').forEach(img=>{ if(site.headerLogo){img.src=site.headerLogo;img.hidden=false;} });
    if(site.headerLogo) document.querySelectorAll('.shared-logo-fallback').forEach(x=>x.hidden=true);
  }).catch(()=>{});
})();
