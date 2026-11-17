import re

preview_path = r"C:/Users/ymcad15/.gemini/antigravity/brain/e7eb441c-9823-4a3c-aaab-3635d2edd850/kenkashi_ui_preview.html"

with open(preview_path, "r", encoding="utf-8") as f:
    html = f.read()

# 1. Update controls-toolbar buttons
old_toolbar = '''    <button class="theme-btn" onclick="openBassDialog()">Bass Dialog</button>
    <button class="theme-btn" onclick="openCustomEq()">EQ Sheet</button>
    <button class="theme-btn" onclick="openFitTest()">Fit Test Sheet</button>'''

new_toolbar = '''    <button class="theme-btn" onclick="openBassDialog()">Bass Dialog</button>
    <button class="theme-btn" style="border-color:#2196F3; color:#2196F3;" onclick="openEqMode()">EQ Mode Page (New)</button>
    <button class="theme-btn" style="border-color:#2196F3; color:#2196F3;" onclick="openCustomEq()">Custom EQ Flyout (New)</button>
    <button class="theme-btn" onclick="openFitTest()">Fit Test Sheet</button>'''

if old_toolbar in html:
    html = html.replace(old_toolbar, new_toolbar)
else:
    print("Warning: old_toolbar not found")

# 2. Update SoundTab Row 5 (Equalizer row) to call openEqMode() and remove separate Custom1 row if present
old_eq_row = '''        <!-- Row 5: Equalizer -->
        <div class="setting-row" onclick="openCustomEq()">
          <div>
            <div class="serif-title" style="font-size: 15px;">Equalizer</div>
            <div class="mono-caption">CUSTOM1 active • 6-band tuning</div>
          </div>
          <div class="mono-caption" style="font-weight:bold; color:var(--text-primary);">CUSTOM1 &gt;</div>
        </div>'''

new_eq_row = '''        <!-- Row 5: Equalizer (Opens EQ Mode Page) -->
        <div class="setting-row" onclick="openEqMode()" style="cursor:pointer;">
          <div>
            <div class="serif-title" style="font-size: 15px;">Equalizer</div>
            <div class="mono-caption" id="soundTabEqSubtitle">Sound effects & 6-band tuning</div>
          </div>
          <div class="mono-caption" id="soundTabEqBadge" style="font-weight:bold; color:var(--text-primary);">CUSTOM1 &gt;</div>
        </div>'''

if old_eq_row in html:
    html = html.replace(old_eq_row, new_eq_row)
else:
    print("Warning: old_eq_row not found directly, checking regex")
    html = re.sub(
        r'<div class="setting-row" onclick="openCustomEq\(\)">\s*<div>\s*<div class="serif-title" style="font-size: 15px;">Equalizer</div>.*?</div>\s*</div>',
        new_eq_row,
        html,
        flags=re.DOTALL
    )

# 3. Add EQ Mode Page overlay inside device-frame right after error-banner
eq_mode_html = '''
    <!-- EQ Mode Full Screen Page Overlay -->
    <div id="eqModePage" style="display:none; position:absolute; top:36px; left:0; right:0; bottom:0; background:var(--bg); z-index:40; flex-direction:column; overflow-y:auto;">
      <!-- EQ Mode Top Bar -->
      <div style="height:56px; border-bottom:1px solid var(--hairline); display:flex; align-items:center; padding:0 12px; gap:8px;">
        <button onclick="closeEqMode()" style="background:transparent; border:none; color:var(--text-primary); font-size:18px; cursor:pointer; padding:6px 10px; font-weight:bold;">&larr;</button>
        <span class="serif-title" style="font-size:19px; font-weight:normal;">EQ mode</span>
      </div>

      <div style="padding:18px 16px;">
        <!-- Section 1: Sound Effects -->
        <div class="mono-caption" style="font-weight:bold; letter-spacing:1px; margin-bottom:10px; color:var(--text-secondary);">SOUND EFFECTS</div>
        
        <div style="border:1px solid var(--hairline); background:var(--surface);">
          <!-- 1. Clear Vocals -->
          <div class="eq-preset-item" onclick="selectEqPreset('CLEAR_VOCALS')" style="display:flex; align-items:center; padding:12px 14px; cursor:pointer;">
            <div style="width:34px; height:34px; border-radius:50%; background:var(--elevated); display:flex; align-items:center; justify-content:center; margin-right:14px; font-size:16px;">🎤</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px;">Clear Vocals</div>
              <div class="mono-caption" style="font-size:10px; color:var(--text-secondary);">Enhanced vocal clarity and presence</div>
            </div>
            <div class="eq-radio" id="radio_CLEAR_VOCALS" style="width:20px; height:20px; border-radius:50%; border:1.5px solid var(--hairline); display:flex; align-items:center; justify-content:center;"></div>
          </div>
          <div class="hairline-divider"></div>

          <!-- 2. Nature Balance -->
          <div class="eq-preset-item" onclick="selectEqPreset('BALANCED')" style="display:flex; align-items:center; padding:12px 14px; cursor:pointer;">
            <div style="width:34px; height:34px; border-radius:50%; background:var(--elevated); display:flex; align-items:center; justify-content:center; margin-right:14px; font-size:16px;">🎚️</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px;">Nature Balance</div>
              <div class="mono-caption" style="font-size:10px; color:var(--text-secondary);">Faithful neutral frequency reproduction</div>
            </div>
            <div class="eq-radio" id="radio_BALANCED" style="width:20px; height:20px; border-radius:50%; border:1.5px solid var(--hairline); display:flex; align-items:center; justify-content:center;"></div>
          </div>
          <div class="hairline-divider"></div>

          <!-- 3. Clear Bass -->
          <div class="eq-preset-item" onclick="selectEqPreset('CLEAR_BASS')" style="display:flex; align-items:center; padding:12px 14px; cursor:pointer;">
            <div style="width:34px; height:34px; border-radius:50%; background:var(--elevated); display:flex; align-items:center; justify-content:center; margin-right:14px; font-size:16px;">🔊</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px;">Clear Bass</div>
              <div class="mono-caption" style="font-size:10px; color:var(--text-secondary);">Tight sub-bass with pristine mid clarity</div>
            </div>
            <div class="eq-radio" id="radio_CLEAR_BASS" style="width:20px; height:20px; border-radius:50%; border:1.5px solid var(--hairline); display:flex; align-items:center; justify-content:center;"></div>
          </div>
          <div class="hairline-divider"></div>

          <!-- 4. Bass Boost -->
          <div class="eq-preset-item" onclick="selectEqPreset('BASS_BOOST')" style="display:flex; align-items:center; padding:12px 14px; cursor:pointer;">
            <div style="width:34px; height:34px; border-radius:50%; background:var(--elevated); display:flex; align-items:center; justify-content:center; margin-right:14px; font-size:16px;">📢</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px;">Bass Boost</div>
              <div class="mono-caption" style="font-size:10px; color:var(--text-secondary);">Amplified low-frequency acoustic slam</div>
            </div>
            <div class="eq-radio" id="radio_BASS_BOOST" style="width:20px; height:20px; border-radius:50%; border:1.5px solid var(--hairline); display:flex; align-items:center; justify-content:center;"></div>
          </div>
        </div>

        <div style="height:24px;"></div>

        <!-- Section 2: Custom Equalizer -->
        <div class="mono-caption" style="font-weight:bold; letter-spacing:1px; margin-bottom:10px; color:var(--text-secondary);">CUSTOM EQUALIZER</div>
        
        <div style="border:1px solid var(--hairline); background:var(--surface);">
          <!-- Custom1 Row -->
          <div style="display:flex; align-items:center; padding:12px 14px; cursor:pointer;" onclick="selectEqPreset('CUSTOM')">
            <div style="width:34px; height:34px; border-radius:50%; background:var(--elevated); display:flex; align-items:center; justify-content:center; margin-right:14px; font-size:16px;">📈</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px;" id="eqPageCustomTitle">Custom1</div>
              <div class="mono-caption" id="eqPageGainsSummary" style="font-size:10px; color:var(--text-secondary);">6-band curve: [0, 2, -3, 6, -6, 1 dB]</div>
            </div>
            <button onclick="event.stopPropagation(); openCustomEq();" style="background:transparent; border:none; color:var(--text-secondary); cursor:pointer; font-size:16px; padding:6px; margin-right:6px;" title="Edit curve">&#9998;</button>
            <div class="eq-radio" id="radio_CUSTOM" style="width:20px; height:20px; border-radius:50%; border:1.5px solid var(--hairline); display:flex; align-items:center; justify-content:center;">
              <div style="width:10px; height:10px; border-radius:50%; background:var(--text-primary);"></div>
            </div>
          </div>
          <div class="hairline-divider"></div>

          <!-- + Add Custom EQ Action Row -->
          <div onclick="selectEqPreset('CUSTOM'); openCustomEq();" style="display:flex; align-items:center; padding:12px 14px; cursor:pointer;">
            <div style="width:34px; height:34px; border-radius:50%; border:1px dashed var(--hairline); display:flex; align-items:center; justify-content:center; margin-right:14px; font-size:18px; color:#2196F3;">+</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px; color:#2196F3;">+ Add custom EQ</div>
              <div class="mono-caption" style="font-size:10px; color:var(--text-secondary);">Customize 6-band response curve</div>
            </div>
          </div>
        </div>
      </div>
    </div>
'''

if 'id="eqModePage"' not in html:
    html = html.replace('<!-- Tab 0: Sound & ANC -->', eq_mode_html + '\n    <!-- Tab 0: Sound & ANC -->')

# 4. Replace customEqOverlay with realme Link matching curve bottom sheet
old_custom_eq_pattern = re.compile(
    r'<!-- 2\. Custom EQ Bottom Sheet Overlay -->\s*<div class="kenkashi-bottom-sheet-overlay" id="customEqOverlay".*?</div>\s*</div>\s*</div>',
    re.DOTALL
)

new_custom_eq_overlay = '''<!-- 2. Custom EQ Flyout Bottom Sheet (Realme Link Matching) -->
    <div class="kenkashi-bottom-sheet-overlay" id="customEqOverlay" onclick="closeCustomEq(event)">
      <div class="kenkashi-sheet-box" onclick="event.stopPropagation()" style="padding:16px 20px 24px 20px;">
        <!-- Flyout Top Bar: Back Arrow, Centered Custom1 with edit icon, Blue Reset text -->
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <button style="background:transparent; border:none; color:var(--text-primary); font-size:18px; cursor:pointer; padding:4px;" onclick="closeCustomEq()">&larr;</button>
          
          <div style="display:flex; align-items:center; gap:6px;">
            <span class="serif-title" id="flyoutPresetName" style="font-size:17px; font-weight:bold;">Custom1</span>
            <span style="font-size:13px; color:var(--text-secondary); cursor:pointer;" onclick="renameCustomPreset()">&#9998;</span>
          </div>

          <button style="background:transparent; border:none; color:#2196F3; font-family:var(--font-mono); font-size:14px; font-weight:bold; cursor:pointer;" onclick="resetCustomEq()">Reset</button>
        </div>

        <div style="font-family:var(--font-mono); font-size:12px; color:var(--text-secondary); margin-top:4px;">Equalizer</div>

        <!-- Interactive 6-Band Frequency Response Curve Canvas / SVG -->
        <div style="position:relative; width:100%; height:200px; margin-top:14px;">
          <!-- Vertical scale labels on left -->
          <div style="position:absolute; left:0; top:12px; font-family:var(--font-mono); font-size:10px; color:var(--text-secondary);">+6db</div>
          <div style="position:absolute; left:0; top:92px; font-family:var(--font-mono); font-size:10px; color:var(--text-secondary);">0db</div>
          <div style="position:absolute; left:0; bottom:12px; font-family:var(--font-mono); font-size:10px; color:var(--text-secondary);">-6db</div>

          <!-- Canvas for interactive curve and draggable nodes -->
          <canvas id="eqCanvas" width="300" height="200" style="position:absolute; left:36px; right:0; top:0; bottom:0; width:calc(100% - 40px); height:100%; cursor:pointer; touch-action:none;"></canvas>
        </div>

        <!-- Frequency Labels Row -->
        <div style="display:flex; justify-content:space-between; padding-left:36px; margin-top:8px; font-family:var(--font-mono); font-size:11px; color:var(--text-primary); font-weight:500;">
          <span style="flex:1; text-align:center;">62</span>
          <span style="flex:1; text-align:center;">250</span>
          <span style="flex:1; text-align:center;">1k</span>
          <span style="flex:1; text-align:center;">4k</span>
          <span style="flex:1; text-align:center;">8k</span>
          <span style="flex:1; text-align:center;">16k</span>
        </div>

        <!-- Low freq / High freq Row -->
        <div style="display:flex; justify-content:space-between; padding-left:36px; margin-top:4px; font-family:var(--font-mono); font-size:10px; color:var(--text-secondary);">
          <span>Low freq</span>
          <span>High freq</span>
        </div>

        <div style="height:12px;"></div>
      </div>
    </div>'''

if 'id="customEqOverlay"' in html:
    html = re.sub(
        r'<div class="kenkashi-bottom-sheet-overlay" id="customEqOverlay".*?<!-- 3\. Fit Test Bottom Sheet Overlay -->',
        new_custom_eq_overlay + '\n\n    <!-- 3. Fit Test Bottom Sheet Overlay -->',
        html,
        flags=re.DOTALL
    )

# 5. Add JavaScript functions for EQ mode page, radio selection, and interactive SVG/Canvas spline
js_additions = '''
    // --- EQUALIZER & CUSTOM EQ STATE ---
    let currentEqPreset = 'CUSTOM';
    let customEqGains = [0, 2, -3, 6, -6, 1];
    let customEqName = 'Custom1';
    let draggingNodeIdx = -1;

    function openEqMode() {
      const page = document.getElementById('eqModePage');
      if (page) {
        page.style.display = 'flex';
        updateEqRadioUI();
      }
    }

    function closeEqMode() {
      const page = document.getElementById('eqModePage');
      if (page) page.style.display = 'none';
    }

    function selectEqPreset(preset) {
      currentEqPreset = preset;
      updateEqRadioUI();
      const badge = document.getElementById('soundTabEqBadge');
      if (badge) {
        const names = {
          'CLEAR_VOCALS': 'CLEAR VOCALS >',
          'BALANCED': 'NATURE BALANCE >',
          'CLEAR_BASS': 'CLEAR BASS >',
          'BASS_BOOST': 'BASS BOOST >',
          'CUSTOM': 'CUSTOM1 >'
        };
        badge.innerText = names[preset] || 'CUSTOM1 >';
      }
    }

    function updateEqRadioUI() {
      const presets = ['CLEAR_VOCALS', 'BALANCED', 'CLEAR_BASS', 'BASS_BOOST', 'CUSTOM'];
      presets.forEach(p => {
        const el = document.getElementById('radio_' + p);
        if (el) {
          if (p === currentEqPreset) {
            el.innerHTML = '<div style="width:10px; height:10px; border-radius:50%; background:var(--text-primary);"></div>';
            el.style.borderColor = 'var(--text-primary)';
          } else {
            el.innerHTML = '';
            el.style.borderColor = 'var(--hairline)';
          }
        }
      });
      const summaryEl = document.getElementById('eqPageGainsSummary');
      if (summaryEl) {
        summaryEl.innerText = '6-band curve: [' + customEqGains.join(', ') + ' dB]';
      }
    }

    function renameCustomPreset() {
      const newName = prompt('Enter name for custom preset:', customEqName);
      if (newName && newName.trim()) {
        customEqName = newName.trim();
        document.getElementById('flyoutPresetName').innerText = customEqName;
        document.getElementById('eqPageCustomTitle').innerText = customEqName;
      }
    }

    function resetCustomEq() {
      customEqGains = [0, 0, 0, 0, 0, 0];
      drawEqCanvas();
      updateEqRadioUI();
    }

    // --- CANVAS INTERACTIVE SPLINE CURVE ---
    function initEqCanvas() {
      const canvas = document.getElementById('eqCanvas');
      if (!canvas) return;
      const ctx = canvas.getContext('2d');

      function resize() {
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * (window.devicePixelRatio || 1);
        canvas.height = rect.height * (window.devicePixelRatio || 1);
        ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
        drawEqCanvas();
      }
      window.addEventListener('resize', resize);
      setTimeout(resize, 50);

      // Mouse and touch dragging
      function getPos(e) {
        const rect = canvas.getBoundingClientRect();
        const clientX = e.touches ? e.touches[0].clientX : e.clientX;
        const clientY = e.touches ? e.touches[0].clientY : e.clientY;
        return {
          x: clientX - rect.left,
          y: clientY - rect.top,
          w: rect.width,
          h: rect.height
        };
      }

      function onPointerDown(e) {
        const pos = getPos(e);
        const colW = pos.w / 6;
        let bestIdx = -1;
        let minD = 999;
        for (let i = 0; i < 6; i++) {
          const ptX = (i + 0.5) * colW;
          const d = Math.abs(pos.x - ptX);
          if (d < colW * 0.7 && d < minD) {
            minD = d;
            bestIdx = i;
          }
        }
        if (bestIdx !== -1) {
          draggingNodeIdx = bestIdx;
          updateGainFromY(bestIdx, pos.y, pos.h);
          e.preventDefault();
        }
      }

      function onPointerMove(e) {
        if (draggingNodeIdx === -1) return;
        const pos = getPos(e);
        updateGainFromY(draggingNodeIdx, pos.y, pos.h);
        e.preventDefault();
      }

      function onPointerUp() {
        draggingNodeIdx = -1;
      }

      function updateGainFromY(idx, y, h) {
        const padY = 20;
        const usableH = h - padY * 2;
        const clampedY = Math.max(padY, Math.min(h - padY, y));
        const fraction = 1 - (clampedY - padY) / usableH; // 0 at bottom, 1 at top
        const gain = Math.round(fraction * 12 - 6);
        customEqGains[idx] = Math.max(-6, Math.min(6, gain));
        drawEqCanvas();
        updateEqRadioUI();
      }

      canvas.addEventListener('mousedown', onPointerDown);
      window.addEventListener('mousemove', onPointerMove);
      window.addEventListener('mouseup', onPointerUp);

      canvas.addEventListener('touchstart', onPointerDown, { passive: false });
      window.addEventListener('touchmove', onPointerMove, { passive: false });
      window.addEventListener('touchend', onPointerUp);
    }

    function drawEqCanvas() {
      const canvas = document.getElementById('eqCanvas');
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      const rect = canvas.getBoundingClientRect();
      const w = rect.width;
      const h = rect.height;
      if (!w || !h) return;

      ctx.clearRect(0, 0, w, h);

      const padY = 20;
      const usableH = h - padY * 2;
      const topY = padY;
      const midY = h / 2;
      const botY = h - padY;

      const isDark = document.body.classList.contains('dark-mode') || document.body.classList.contains('amoled-mode');
      const gridColor = isDark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.1)';
      const accentBlue = '#2196F3';

      // Horizontal guidelines (+6, 0, -6)
      ctx.lineWidth = 1;
      ctx.strokeStyle = gridColor;

      ctx.beginPath();
      ctx.moveTo(0, topY); ctx.lineTo(w, topY);
      ctx.moveTo(0, midY); ctx.lineTo(w, midY);
      ctx.moveTo(0, botY); ctx.lineTo(w, botY);
      ctx.stroke();

      // Vertical guide lines for each band
      const colW = w / 6;
      ctx.strokeStyle = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.05)';
      ctx.beginPath();
      for (let i = 0; i < 6; i++) {
        const x = (i + 0.5) * colW;
        ctx.moveTo(x, topY);
        ctx.lineTo(x, botY);
      }
      ctx.stroke();

      // Compute 6 points
      const points = [];
      for (let i = 0; i < 6; i++) {
        const gain = customEqGains[i];
        const norm = 1 - (gain + 6) / 12;
        const x = (i + 0.5) * colW;
        const y = topY + norm * usableH;
        points.push({ x, y });
      }

      // Smooth Cubic Bezier Spline
      ctx.save();
      ctx.beginPath();
      ctx.moveTo(0, points[0].y);
      ctx.bezierCurveTo(points[0].x * 0.5, points[0].y, points[0].x * 0.5, points[0].y, points[0].x, points[0].y);

      for (let i = 0; i < points.length - 1; i++) {
        const p0 = points[i];
        const p1 = points[i + 1];
        const midX = (p0.x + p1.x) / 2;
        ctx.bezierCurveTo(midX, p0.y, midX, p1.y, p1.x, p1.y);
      }
      const last = points[points.length - 1];
      ctx.bezierCurveTo((last.x + w) / 2, last.y, (last.x + w) / 2, last.y, w, last.y);

      // Stroke curve path
      ctx.strokeStyle = accentBlue;
      ctx.lineWidth = 3;
      ctx.lineCap = 'round';
      ctx.stroke();

      // Gradient Fill under curve down to botY
      ctx.lineTo(w, botY);
      ctx.lineTo(0, botY);
      ctx.closePath();

      const grad = ctx.createLinearGradient(0, topY, 0, botY);
      grad.addColorStop(0, 'rgba(33, 150, 243, 0.35)');
      grad.addColorStop(0.6, 'rgba(33, 150, 243, 0.12)');
      grad.addColorStop(1, 'rgba(33, 150, 243, 0.0)');
      ctx.fillStyle = grad;
      ctx.fill();
      ctx.restore();

      // Draw draggable node handles
      points.forEach((pt, i) => {
        // Outer halo
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 11, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(33, 150, 243, 0.22)';
        ctx.fill();

        // Blue circle
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 6.5, 0, Math.PI * 2);
        ctx.fillStyle = accentBlue;
        ctx.fill();

        // Inner white dot
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 3, 0, Math.PI * 2);
        ctx.fillStyle = '#FFFFFF';
        ctx.fill();
      });
    }

    // Initialize canvas on load and when flyout opens
    window.addEventListener('DOMContentLoaded', () => {
      initEqCanvas();
    });

    const origOpenCustomEq = openCustomEq;
    openCustomEq = function() {
      origOpenCustomEq();
      setTimeout(drawEqCanvas, 50);
    };
'''

if 'function openEqMode' not in html:
    html = html.replace('function openCustomEq() {', js_additions + '\n    function openCustomEq() {')

with open(preview_path, "w", encoding="utf-8") as f:
    f.write(html)

print("Updated kenkashi_ui_preview.html successfully!")
