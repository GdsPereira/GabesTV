/**
 * GabesTV - Interactive Landing Page Logic
 * Features:
 * - Device Switcher (Android TV vs Mobile)
 * - Interactive D-Pad Remote & Keyboard Navigation
 * - Channel Zapping Simulation with Instant Buffer & HUD
 * - Installation Guide Tabs & One-Click Clipboard Copy
 * - FAQ Accordion
 */

document.addEventListener('DOMContentLoaded', () => {
  // --- Channels Mock Data ---
  const CHANNELS = [
    {
      id: 'sportv',
      name: 'SporTV HD',
      category: 'Esportes',
      resolution: '1080p 60fps',
      bitrate: '6.5 Mbps HLS',
      icon: '⚽',
      color1: '#002B49',
      color2: '#0072CE',
      desc: 'Brasileirão Série A • Transmissão Ao Vivo'
    },
    {
      id: 'espn',
      name: 'ESPN Brasil',
      category: 'Esportes',
      resolution: '1080p 60fps',
      bitrate: '7.2 Mbps HLS',
      icon: '🏆',
      color1: '#4A0D15',
      color2: '#CC0000',
      desc: 'Premier League • Manchester City vs Arsenal'
    },
    {
      id: 'globo',
      name: 'Globo SP HD',
      category: 'Variedades',
      resolution: '1080p 60fps',
      bitrate: '8.0 Mbps MPEG-TS',
      icon: '📺',
      color1: '#1A237E',
      color2: '#E91E63',
      desc: 'Jornal Nacional • Ao Vivo com Sinal Digital'
    },
    {
      id: 'hbo',
      name: 'HBO Signature',
      category: 'Filmes',
      resolution: '4K Ultra HD',
      bitrate: '14.0 Mbps HLS',
      icon: '🎬',
      color1: '#120A2A',
      color2: '#5E35B1',
      desc: 'Cinema em Casa • Áudio Dolby Atmos 5.1'
    },
    {
      id: 'premiere',
      name: 'Premiere Clubes',
      category: 'Esportes',
      resolution: '1080p 60fps',
      bitrate: '6.8 Mbps HLS',
      icon: '⚽',
      color1: '#003300',
      color2: '#009933',
      desc: 'Campeonato Paulista • Cobertura Exclusiva'
    },
    {
      id: 'cnn',
      name: 'CNN Brasil',
      category: 'Notícias',
      resolution: '1080p 60fps',
      bitrate: '5.5 Mbps HLS',
      icon: '📰',
      color1: '#3D0C11',
      color2: '#B71C1C',
      desc: 'CNN 360° • Notícias do Brasil e do Mundo'
    },
    {
      id: 'discovery',
      name: 'Discovery Channel',
      category: 'Documentários',
      resolution: '1080p 60fps',
      bitrate: '6.0 Mbps HLS',
      icon: '🌍',
      color1: '#004D40',
      color2: '#00B4D8',
      desc: 'Planeta Selvagem • Especial 4K HDR'
    },
    {
      id: 'cartoon',
      name: 'Cartoon Network',
      category: 'Infantil',
      resolution: '1080p 60fps',
      bitrate: '5.0 Mbps HLS',
      icon: '⚡',
      color1: '#212121',
      color2: '#7C4DFF',
      desc: 'Animações 24 horas • Hora de Aventura'
    }
  ];

  let currentChannelIndex = 0;
  let isPlaying = true;
  let isHudVisible = true;
  let hudTimeout = null;

  // --- Elements ---
  const tvContainer = document.getElementById('tvStage');
  const mobileContainer = document.getElementById('mobileStage');
  const tabButtons = document.querySelectorAll('.device-tab');
  
  const tvChannelsGrid = document.getElementById('tvChannelsGrid');
  const tvVideoBg = document.getElementById('tvVideoBg');
  const tvHud = document.getElementById('tvHud');
  const hudChannelName = document.getElementById('hudChannelName');
  const hudChannelDesc = document.getElementById('hudChannelDesc');
  const hudCategory = document.getElementById('hudCategory');
  const hudResolution = document.getElementById('hudResolution');

  // D-Pad buttons
  const btnUp = document.getElementById('dpadUp');
  const btnDown = document.getElementById('dpadDown');
  const btnLeft = document.getElementById('dpadLeft');
  const btnRight = document.getElementById('dpadRight');
  const btnCenter = document.getElementById('dpadCenter');
  const btnInfo = document.getElementById('remoteInfo');
  const btnPlayPause = document.getElementById('remotePlayPause');
  const btnBack = document.getElementById('remoteBack');

  // --- Device Switcher ---
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      tabButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      const target = btn.dataset.device;
      if (target === 'tv') {
        tvContainer.style.display = 'block';
        mobileContainer.style.display = 'none';
      } else {
        tvContainer.style.display = 'none';
        mobileContainer.style.display = 'flex';
      }
    });
  });

  // --- Render TV Grid Cards ---
  function renderTvCards() {
    if (!tvChannelsGrid) return;
    tvChannelsGrid.innerHTML = '';

    CHANNELS.forEach((ch, idx) => {
      const card = document.createElement('div');
      card.className = `tv-channel-card ${idx === currentChannelIndex ? 'focused' : ''}`;
      card.innerHTML = `
        <div class="ch-icon">${ch.icon}</div>
        <div class="ch-name">${ch.name}</div>
        <div class="ch-tag">${ch.category}</div>
      `;
      card.addEventListener('click', () => {
        selectChannel(idx);
      });
      tvChannelsGrid.appendChild(card);
    });
  }

  // --- Channel Selection / Zapping ---
  function selectChannel(idx) {
    if (idx < 0) idx = CHANNELS.length - 1;
    if (idx >= CHANNELS.length) idx = 0;

    currentChannelIndex = idx;
    const channel = CHANNELS[currentChannelIndex];

    // Update focused card
    const cards = tvChannelsGrid.querySelectorAll('.tv-channel-card');
    cards.forEach((c, i) => {
      c.classList.toggle('focused', i === currentChannelIndex);
      if (i === currentChannelIndex) {
        c.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      }
    });

    // Update simulated video canvas
    if (tvVideoBg) {
      tvVideoBg.style.background = `radial-gradient(circle at center, ${channel.color2} 0%, ${channel.color1} 70%, #06050B 100%)`;
    }

    // Update HUD
    if (hudChannelName) hudChannelName.textContent = channel.name;
    if (hudChannelDesc) hudChannelDesc.textContent = channel.desc;
    if (hudCategory) hudCategory.textContent = channel.category;
    if (hudResolution) hudResolution.textContent = `${channel.resolution} • ${channel.bitrate}`;

    showHudTemporary();
  }

  // --- HUD Control ---
  function showHudTemporary() {
    isHudVisible = true;
    if (tvHud) tvHud.classList.remove('hidden');

    if (hudTimeout) clearTimeout(hudTimeout);
    hudTimeout = setTimeout(() => {
      if (isPlaying) {
        isHudVisible = false;
        if (tvHud) tvHud.classList.add('hidden');
      }
    }, 4500);
  }

  function toggleHud() {
    isHudVisible = !isHudVisible;
    if (tvHud) {
      tvHud.classList.toggle('hidden', !isHudVisible);
    }
  }

  function togglePlayPause() {
    isPlaying = !isPlaying;
    if (btnCenter) {
      btnCenter.textContent = isPlaying ? 'OK' : 'PAUSED';
    }
    showHudTemporary();
  }

  // --- Interactive Remote Event Listeners ---
  function addPressEffect(element) {
    if (!element) return;
    element.classList.add('pressed');
    setTimeout(() => element.classList.remove('pressed'), 160);
  }

  if (btnLeft) {
    btnLeft.addEventListener('click', () => {
      addPressEffect(btnLeft);
      selectChannel(currentChannelIndex - 1);
    });
  }

  if (btnRight) {
    btnRight.addEventListener('click', () => {
      addPressEffect(btnRight);
      selectChannel(currentChannelIndex + 1);
    });
  }

  if (btnUp) {
    btnUp.addEventListener('click', () => {
      addPressEffect(btnUp);
      toggleHud();
    });
  }

  if (btnDown) {
    btnDown.addEventListener('click', () => {
      addPressEffect(btnDown);
      toggleHud();
    });
  }

  if (btnCenter) {
    btnCenter.addEventListener('click', () => {
      addPressEffect(btnCenter);
      togglePlayPause();
    });
  }

  if (btnInfo) {
    btnInfo.addEventListener('click', () => {
      toggleHud();
    });
  }

  if (btnPlayPause) {
    btnPlayPause.addEventListener('click', () => {
      togglePlayPause();
    });
  }

  if (btnBack) {
    btnBack.addEventListener('click', () => {
      if (tvHud) tvHud.classList.add('hidden');
      isHudVisible = false;
    });
  }

  // --- Keyboard Remote Simulation ---
  window.addEventListener('keydown', (e) => {
    // Only react if TV preview is visible and not inside an input
    if (tvContainer && tvContainer.style.display !== 'none' && !['INPUT', 'TEXTAREA'].includes(e.target.tagName)) {
      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        addPressEffect(btnLeft);
        selectChannel(currentChannelIndex - 1);
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        addPressEffect(btnRight);
        selectChannel(currentChannelIndex + 1);
      } else if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
        e.preventDefault();
        addPressEffect(e.key === 'ArrowUp' ? btnUp : btnDown);
        toggleHud();
      } else if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        addPressEffect(btnCenter);
        togglePlayPause();
      } else if (e.key === 'Escape' || e.key === 'Backspace') {
        e.preventDefault();
        if (tvHud) tvHud.classList.add('hidden');
      }
    }
  });

  // --- Installation Guide Tabs ---
  const installTabBtns = document.querySelectorAll('.install-tab-btn');
  const installTabContents = document.querySelectorAll('.install-tab-content');

  installTabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      installTabBtns.forEach(b => b.classList.remove('active'));
      installTabContents.forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const targetId = btn.dataset.tab;
      const targetContent = document.getElementById(targetId);
      if (targetContent) {
        targetContent.classList.add('active');
      }
    });
  });

  // --- Copy to Clipboard ---
  const copyBtns = document.querySelectorAll('.copy-btn');
  copyBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const codeText = btn.dataset.code || btn.parentElement.querySelector('code')?.innerText;
      if (codeText) {
        navigator.clipboard.writeText(codeText.trim()).then(() => {
          const originalText = btn.innerHTML;
          btn.innerHTML = '✓ Copiado!';
          btn.classList.add('copied');
          setTimeout(() => {
            btn.innerHTML = originalText;
            btn.classList.remove('copied');
          }, 2000);
        }).catch(err => {
          console.error('Falha ao copiar:', err);
        });
      }
    });
  });

  // --- FAQ Accordion ---
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const questionBtn = item.querySelector('.faq-question');
    if (questionBtn) {
      questionBtn.addEventListener('click', () => {
        const isOpen = item.classList.contains('open');
        // Close all others
        faqItems.forEach(i => i.classList.remove('open'));
        if (!isOpen) {
          item.classList.add('open');
        }
      });
    }
  });

  // --- Initialize Default Channel ---
  renderTvCards();
  selectChannel(0);
});
