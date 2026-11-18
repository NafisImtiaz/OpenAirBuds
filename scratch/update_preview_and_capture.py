import os
import subprocess

brain_dir = r"C:/Users/ymcad15/.gemini/antigravity/brain/e7eb441c-9823-4a3c-aaab-3635d2edd850"
edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
preview_html_path = os.path.join(brain_dir, "kenkashi_ui_preview.html")

with open(preview_html_path, "r", encoding="utf-8") as f:
    html = f.read()

# 1. Add toolbar button
toolbar_target = '<button class="theme-btn" onclick="openFitTest()">Fit Test Sheet</button>'
toolbar_replace = toolbar_target + '\n    <button class="theme-btn" onclick="openSetupScreen()">Permissions & Setup</button>'
if 'openSetupScreen()' not in html:
    html = html.replace(toolbar_target, toolbar_replace, 1)

# 2. Add setupScreenPage overlay before Tab 0
setup_overlay = """
    <!-- Permissions & Setup Screen Page Overlay -->
    <div id="setupScreenPage" style="display:none; position:absolute; top:36px; left:0; right:0; bottom:0; background:var(--bg); z-index:45; flex-direction:column; overflow-y:auto;">
      <!-- Top Bar -->
      <div style="height:56px; border-bottom:1px solid var(--hairline); display:flex; align-items:center; padding:0 16px; gap:16px;">
        <button onclick="closeSetupScreen()" style="width:34px; height:34px; background:transparent; border:1px solid var(--hairline); color:var(--text-primary); cursor:pointer; display:flex; align-items:center; justify-content:center; padding:0;">&larr;</button>
        <span style="font-family:var(--font-mono); font-size:12px; font-weight:bold; letter-spacing:2px; color:var(--text-primary);">SYSTEM ACCESS</span>
      </div>

      <div style="padding:20px 20px 30px 20px; display:flex; flex-direction:column; gap:20px;">
        <!-- Header -->
        <div>
          <div class="serif-title" style="font-size:26px; font-weight:normal; color:var(--text-primary);">Device Permissions</div>
          <div class="mono-caption" style="font-size:12px; color:var(--text-secondary); margin-top:6px; line-height:17px;">Configure hardware access so OpenAirBuds can detect, sync, and control your earbuds.</div>
        </div>

        <!-- 1. Bluetooth Card (Essential) -->
        <div style="border:1px solid var(--hairline); background:var(--surface); padding:16px; display:flex; flex-direction:column; gap:12px;">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <span style="font-family:var(--font-mono); font-size:10px; font-weight:bold; letter-spacing:1px; border:1px solid var(--text-primary); padding:2px 6px; color:var(--text-primary);">ESSENTIAL</span>
            <div id="setupBtStatus" style="display:flex; align-items:center; gap:4px; font-family:var(--font-mono); font-size:11px; font-weight:bold; color:#2E7D32;">
              <span>&#10003;</span> GRANTED
            </div>
          </div>
          <div style="display:flex; gap:12px; align-items:flex-start;">
            <div style="width:36px; height:36px; border:1px solid var(--hairline); background:var(--bg); display:flex; align-items:center; justify-content:center; font-size:16px;">📶</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px; font-weight:bold;">Nearby Devices & Bluetooth</div>
              <div class="mono-caption" style="font-size:11px; color:var(--text-secondary); margin-top:4px; line-height:15px;">Required to scan, discover, pair, and send real-time noise control commands to your OpenAirBuds hardware.</div>
            </div>
          </div>
        </div>

        <!-- 2. Phone Calls Card (Optional) -->
        <div style="border:1px solid var(--hairline); background:var(--surface); padding:16px; display:flex; flex-direction:column; gap:12px;">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <span style="font-family:var(--font-mono); font-size:10px; font-weight:bold; letter-spacing:1px; border:1px solid var(--text-secondary); padding:2px 6px; color:var(--text-secondary);">OPTIONAL</span>
            <div id="setupPhoneStatus" style="display:flex; align-items:center; gap:4px; font-family:var(--font-mono); font-size:11px; font-weight:bold; color:#2E7D32;">
              <span>&#10003;</span> GRANTED
            </div>
          </div>
          <div style="display:flex; gap:12px; align-items:flex-start;">
            <div style="width:36px; height:36px; border:1px solid var(--hairline); background:var(--bg); display:flex; align-items:center; justify-content:center; font-size:16px;">📞</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px; font-weight:bold;">Phone Call Management</div>
              <div class="mono-caption" style="font-size:11px; color:var(--text-secondary); margin-top:4px; line-height:15px;">Used exclusively by the Auto-Answer feature to seamlessly accept incoming phone calls when placing an earbud into your ear.</div>
            </div>
          </div>
        </div>

        <!-- 3. Notifications Card (Optional) -->
        <div style="border:1px solid var(--hairline); background:var(--surface); padding:16px; display:flex; flex-direction:column; gap:12px;">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <span style="font-family:var(--font-mono); font-size:10px; font-weight:bold; letter-spacing:1px; border:1px solid var(--text-secondary); padding:2px 6px; color:var(--text-secondary);">OPTIONAL</span>
            <div id="setupNotifStatus" style="display:flex; align-items:center; gap:4px; font-family:var(--font-mono); font-size:11px; font-weight:bold; color:#2E7D32;">
              <span>&#10003;</span> GRANTED
            </div>
          </div>
          <div style="display:flex; gap:12px; align-items:flex-start;">
            <div style="width:36px; height:36px; border:1px solid var(--hairline); background:var(--bg); display:flex; align-items:center; justify-content:center; font-size:16px;">🔔</div>
            <div style="flex:1;">
              <div class="serif-title" style="font-size:15px; font-weight:bold;">Status & Quick Settings</div>
              <div class="mono-caption" style="font-size:11px; color:var(--text-secondary); margin-top:4px; line-height:15px;">Enables background connection persistence and synchronization with the Quick Settings 1-tap ANC tile.</div>
            </div>
          </div>
        </div>

        <!-- Privacy Box -->
        <div style="border:1px solid var(--hairline); background:var(--elevated); padding:12px; display:flex; flex-direction:column; gap:4px;">
          <div style="font-family:var(--font-mono); font-size:9px; font-weight:bold; letter-spacing:1px; color:var(--text-primary);">LOCAL PROCESSING GUARANTEE</div>
          <div class="mono-caption" style="font-size:10px; color:var(--text-secondary); line-height:14px;">All telemetry and commands are processed entirely on-device via Bluetooth. No personal data, audio, or call info is ever transmitted to remote servers.</div>
        </div>

        <!-- Continue Button -->
        <button onclick="closeSetupScreen()" style="width:100%; height:48px; background:var(--text-primary); color:var(--bg); border:1px solid var(--text-primary); font-family:var(--font-mono); font-size:12px; font-weight:bold; letter-spacing:2px; cursor:pointer; margin-top:10px;">CONTINUE TO APP</button>
      </div>
    </div>
"""
tab0_target = '    <!-- Tab 0: Sound & ANC -->'
if 'id="setupScreenPage"' not in html:
    html = html.replace(tab0_target, setup_overlay + "\n" + tab0_target, 1)

# 3. Add Permissions & Setup row in SettingsTab (tab 2) under Diagnostics
fit_test_row = """      <div class="setting-row" onclick="openFitTest()" style="cursor: pointer;">
        <div class="setting-left">
          <div class="setting-title">Earbud Fit Test</div>
          <div class="setting-subtitle">Check acoustic seal & audio frequency leakage</div>
        </div>
        <span class="badge">TEST</span>
      </div>"""

perm_row = """      <div class="setting-row" onclick="openFitTest()" style="cursor: pointer;">
        <div class="setting-left">
          <div class="setting-title">Earbud Fit Test</div>
          <div class="setting-subtitle">Check acoustic seal & audio frequency leakage</div>
        </div>
        <span class="badge">TEST</span>
      </div>
      <div class="setting-row" onclick="openSetupScreen()" style="cursor: pointer;">
        <div class="setting-left">
          <div class="setting-title">Permissions & Setup</div>
          <div class="setting-subtitle">Review system access, Bluetooth, and phone call permissions</div>
        </div>
        <span class="badge">REVIEW</span>
      </div>"""

if 'Permissions & Setup' not in html:
    html = html.replace(fit_test_row, perm_row, 1)

# 4. Add JavaScript handlers
js_handlers = """
    function openSetupScreen() {
      document.getElementById('setupScreenPage').style.display = 'flex';
    }
    function closeSetupScreen() {
      document.getElementById('setupScreenPage').style.display = 'none';
    }
"""
if 'function openSetupScreen()' not in html:
    html = html.replace('function openEqMode() {', js_handlers + '\n    function openEqMode() {', 1)

with open(preview_html_path, "w", encoding="utf-8") as f:
    f.write(html)

print("Updated kenkashi_ui_preview.html successfully.")

# Clean CSS for full-view screenshots
clean_css = """
<style>
header, .controls-toolbar { display: none !important; }
body { padding: 24px 0 !important; background: #0B0F17 !important; display: flex; justify-content: center; }
.device-frame { height: 960px !important; margin: 0 auto !important; }
.tab-viewport { overflow-y: visible !important; }
</style>
"""

# 5. Build and capture Setup Screen (Light & Dark)
setup_light_html = html.replace(
    "</head>", clean_css + "</head>"
).replace(
    "window.addEventListener('DOMContentLoaded', () => {",
    "window.addEventListener('DOMContentLoaded', () => {\n      openSetupScreen();"
)
setup_light_path = os.path.join(brain_dir, "preview_setup_page_light.html")
with open(setup_light_path, "w", encoding="utf-8") as f:
    f.write(setup_light_html)

setup_dark_html = html.replace(
    "</head>", clean_css + "</head>"
).replace(
    "window.addEventListener('DOMContentLoaded', () => {",
    "window.addEventListener('DOMContentLoaded', () => {\n      setThemeMode('dark');\n      openSetupScreen();"
)
setup_dark_path = os.path.join(brain_dir, "preview_setup_page_dark.html")
with open(setup_dark_path, "w", encoding="utf-8") as f:
    f.write(setup_dark_html)

# Capture setup screenshots
png_setup_light = os.path.join(brain_dir, "setup_page_light.png")
png_setup_dark = os.path.join(brain_dir, "setup_page_dark.png")

subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=460,1040",
    f"--screenshot={png_setup_light}",
    setup_light_path
], check=True)

subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=460,1040",
    f"--screenshot={png_setup_dark}",
    setup_dark_path
], check=True)

print("Setup screenshots captured:")
print("Light:", png_setup_light)
print("Dark:", png_setup_dark)

# Also refresh settings tab screenshots with the new "Permissions & Setup" row
settings_light_html = html.replace(
    "</head>", clean_css + "</head>"
).replace(
    "window.addEventListener('DOMContentLoaded', () => {",
    "window.addEventListener('DOMContentLoaded', () => {\n      setTab(2);\n      updateImages();"
)
settings_light_path = os.path.join(brain_dir, "preview_settings_tab.html")
with open(settings_light_path, "w", encoding="utf-8") as f:
    f.write(settings_light_html)

settings_dark_html = html.replace(
    "</head>", clean_css + "</head>"
).replace(
    "window.addEventListener('DOMContentLoaded', () => {",
    "window.addEventListener('DOMContentLoaded', () => {\n      setThemeMode('dark');\n      setTab(2);\n      updateImages();"
)
settings_dark_path = os.path.join(brain_dir, "preview_settings_tab_dark.html")
with open(settings_dark_path, "w", encoding="utf-8") as f:
    f.write(settings_dark_html)

png_settings_light = os.path.join(brain_dir, "settings_tab.png")
png_settings_dark = os.path.join(brain_dir, "settings_tab_dark.png")

subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=460,1040",
    f"--screenshot={png_settings_light}",
    settings_light_path
], check=True)

subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=460,1040",
    f"--screenshot={png_settings_dark}",
    settings_dark_path
], check=True)

print("Updated Settings tab screenshots captured:")
print("Light:", png_settings_light)
print("Dark:", png_settings_dark)
