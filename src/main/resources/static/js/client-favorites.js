(function () {
  'use strict';
  const STORAGE_KEY = 'feelhomeFavoriteRoomIds';

  function readIds() {
    try {
      const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
      return new Set((Array.isArray(raw) ? raw : []).map(Number).filter(Number.isFinite));
    } catch (_) {
      return new Set();
    }
  }

  function writeIds(ids) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(ids)));
  }

  function iconFor(active) {
    return active ? 'bi bi-heart-fill' : 'bi bi-heart';
  }

  function showToast(message) {
    const toast = document.getElementById('favoriteToast');
    if (!toast) return;
    const span = toast.querySelector('span');
    if (span) span.textContent = message;
    toast.classList.add('show');
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove('show'), 2200);
  }

  function syncButtons(ids) {
    document.querySelectorAll('[data-favorite-toggle]').forEach(btn => {
      const id = Number(btn.getAttribute('data-favorite-toggle'));
      const active = ids.has(id);
      btn.classList.toggle('is-active', active);
      btn.setAttribute('aria-pressed', active ? 'true' : 'false');
      btn.setAttribute('title', active ? 'Bỏ khỏi yêu thích' : 'Thêm vào yêu thích');
      const icon = btn.querySelector('i');
      if (icon) icon.className = iconFor(active);
    });
  }

  function syncCounters(ids) {
    document.querySelectorAll('#favoriteNavCount,.home-favorite-count').forEach(el => {
      el.textContent = String(ids.size);
      el.style.display = ids.size ? '' : 'none';
    });
    const hero = document.getElementById('favoriteHeroCount');
    if (hero) hero.textContent = String(ids.size);
  }

  function syncFavoritePage(ids) {
    const cards = Array.from(document.querySelectorAll('[data-favorite-card]'));
    if (!cards.length) return;

    let visible = 0;
    let promos = 0;
    cards.forEach(card => {
      const id = Number(card.getAttribute('data-favorite-card'));
      const show = ids.has(id);
      card.hidden = !show;
      if (show) {
        visible++;
        if (card.getAttribute('data-has-promotion') === 'true') promos++;
      }
    });

    const empty = document.getElementById('favoriteEmpty');
    if (empty) empty.hidden = visible > 0;
    const summary = document.getElementById('favoriteSummary');
    if (summary) summary.textContent = visible
      ? `Bạn đang lưu ${visible} phòng. Có ${promos} phòng đang được áp dụng ưu đãi.`
      : 'Chưa có phòng nào được lưu từ trang chủ.';
    const promoCount = document.getElementById('favoritePromoCount');
    if (promoCount) promoCount.textContent = String(promos);
    const clearBtn = document.getElementById('clearFavoritesBtn');
    if (clearBtn) clearBtn.disabled = visible === 0;
  }

  function syncAll(ids) {
    syncButtons(ids);
    syncCounters(ids);
    syncFavoritePage(ids);
  }

  function toggle(id) {
    const ids = readIds();
    if (ids.has(id)) {
      ids.delete(id);
      showToast('Đã bỏ phòng khỏi danh sách yêu thích');
    } else {
      ids.add(id);
      showToast('Đã thêm phòng vào danh sách yêu thích');
    }
    writeIds(ids);
    syncAll(ids);
  }

  document.addEventListener('click', function (event) {
    const toggleBtn = event.target.closest('[data-favorite-toggle]');
    if (toggleBtn) {
      event.preventDefault();
      event.stopPropagation();
      toggle(Number(toggleBtn.getAttribute('data-favorite-toggle')));
      return;
    }

    const removeBtn = event.target.closest('[data-remove-favorite]');
    if (removeBtn) {
      event.preventDefault();
      const id = Number(removeBtn.getAttribute('data-remove-favorite'));
      const ids = readIds();
      ids.delete(id);
      writeIds(ids);
      syncAll(ids);
      showToast('Đã bỏ phòng khỏi danh sách yêu thích');
      return;
    }

    if (event.target.closest('#clearFavoritesBtn')) {
      if (!readIds().size) return;
      if (window.confirm('Xóa toàn bộ phòng khỏi danh sách yêu thích?')) {
        localStorage.removeItem(STORAGE_KEY);
        syncAll(new Set());
        showToast('Đã xóa toàn bộ danh sách yêu thích');
      }
    }
  });

  window.addEventListener('storage', function (event) {
    if (event.key === STORAGE_KEY) syncAll(readIds());
  });

  document.addEventListener('DOMContentLoaded', function () {
    syncAll(readIds());
  });
})();
