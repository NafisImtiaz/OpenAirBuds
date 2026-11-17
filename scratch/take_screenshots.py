import subprocess
import os

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
out_dir = r"C:/Users/ymcad15/.gemini/antigravity/brain/e7eb441c-9823-4a3c-aaab-3635d2edd850"

with open(os.path.join(out_dir, "kenkashi_ui_preview.html"), "r", encoding="utf-8") as f:
    base_html = f.read()

# Make sheet padding generous at bottom and canvas compact
base_html = base_html.replace('style="padding:16px 20px 44px 20px;"', 'style="padding:16px 20px 48px 20px;"')
base_html = base_html.replace("height:135px; margin-top:6px;", "height:120px; margin-top:4px;")
base_html = base_html.replace('height="135"', 'height="120"')
base_html = base_html.replace("top:60px;", "top:50px;")

with open(os.path.join(out_dir, "kenkashi_ui_preview.html"), "w", encoding="utf-8") as f:
    f.write(base_html)

# 1. Page with EQ Mode Light
html_eq = base_html.replace("window.addEventListener('DOMContentLoaded', () => {", "window.addEventListener('DOMContentLoaded', () => {\n  openEqMode();")
with open(os.path.join(out_dir, "preview_eq_mode.html"), "w", encoding="utf-8") as f:
    f.write(html_eq)

# 2. Page with Custom EQ Flyout Light
html_fly = base_html.replace("window.addEventListener('DOMContentLoaded', () => {", "window.addEventListener('DOMContentLoaded', () => {\n  openEqMode();\n  openCustomEq();")
with open(os.path.join(out_dir, "preview_flyout.html"), "w", encoding="utf-8") as f:
    f.write(html_fly)

# 3. Page with Custom EQ Flyout Dark
html_fly_dark = base_html.replace("window.addEventListener('DOMContentLoaded', () => {", "window.addEventListener('DOMContentLoaded', () => {\n  setThemeMode('dark');\n  openEqMode();\n  openCustomEq();")
with open(os.path.join(out_dir, "preview_flyout_dark.html"), "w", encoding="utf-8") as f:
    f.write(html_fly_dark)

# Take screenshots with 1100px height
subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=500,1100",
    f"--screenshot={os.path.join(out_dir, 'eq_mode_page.png')}",
    os.path.join(out_dir, "preview_eq_mode.html")
])

subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=500,1100",
    f"--screenshot={os.path.join(out_dir, 'custom_eq_flyout.png')}",
    os.path.join(out_dir, "preview_flyout.html")
])

subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=500,1100",
    f"--screenshot={os.path.join(out_dir, 'custom_eq_flyout_dark.png')}",
    os.path.join(out_dir, "preview_flyout_dark.html")
])

print("All screenshots captured with full height!")
