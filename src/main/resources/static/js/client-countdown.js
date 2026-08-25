(function () {
  'use strict';
  function pad(v){ return String(v).padStart(2,'0'); }
  function render(el){
    const raw = el.getAttribute('data-promo-end');
    if(!raw) return;
    const end = new Date(raw);
    if(Number.isNaN(end.getTime())) return;
    const target = el.querySelector('span') || el;
    const diff = end.getTime() - Date.now();
    if(diff <= 0){
      target.textContent = 'Ưu đãi vừa hết hạn';
      el.classList.add('expired');
      return;
    }
    const days = Math.floor(diff / 86400000);
    const hours = Math.floor((diff % 86400000) / 3600000);
    const mins = Math.floor((diff % 3600000) / 60000);
    const secs = Math.floor((diff % 60000) / 1000);
    target.textContent = 'Còn ' + (days ? days + ' ngày ' : '') + pad(hours) + ':' + pad(mins) + ':' + pad(secs);
  }
  function tick(){ document.querySelectorAll('.js-promo-countdown').forEach(render); }
  document.addEventListener('DOMContentLoaded', function(){ tick(); setInterval(tick, 1000); });
})();
