import subprocess
import os

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
brain_dir = r"C:/Users/ymcad15/.gemini/antigravity/brain/e7eb441c-9823-4a3c-aaab-3635d2edd850"

with open(os.path.join(brain_dir, "kenkashi_ui_preview.html"), "r", encoding="utf-8") as f:
    base_html = f.read()

# Override CSS to focus cleanly on the device mockup with full content visible
clean_css = """
<style>
header, .controls-toolbar { display: none !important; }
body { padding: 24px 0 !important; background: #0B0F17 !important; display: flex; justify-content: center; }
.device-frame { height: 960px !important; margin: 0 auto !important; }
.tab-viewport { overflow-y: visible !important; }
</style>
"""

# 1. Page with Settings Tab in Paper Light
html_light = base_html.replace(
    "</head>",
    clean_css + "</head>"
).replace(
    "window.addEventListener('DOMContentLoaded', () => {",
    "window.addEventListener('DOMContentLoaded', () => {\n      setTab(2);\n      updateImages();"
)
light_html_path = os.path.join(brain_dir, "preview_settings_tab.html")
with open(light_html_path, "w", encoding="utf-8") as f:
    f.write(html_light)

# 2. Page with Settings Tab in Charcoal Dark
html_dark = base_html.replace(
    "</head>",
    clean_css + "</head>"
).replace(
    "window.addEventListener('DOMContentLoaded', () => {",
    "window.addEventListener('DOMContentLoaded', () => {\n      setThemeMode('dark');\n      setTab(2);\n      updateImages();"
)
dark_html_path = os.path.join(brain_dir, "preview_settings_tab_dark.html")
with open(dark_html_path, "w", encoding="utf-8") as f:
    f.write(html_dark)

# Output image paths
png_light = os.path.join(brain_dir, "settings_tab.png")
png_dark = os.path.join(brain_dir, "settings_tab_dark.png")

# Run headless Edge to capture 460x1040 viewport screenshots
subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=460,1040",
    f"--screenshot={png_light}",
    light_html_path
], check=True)

subprocess.run([
    edge_path, "--headless=new", "--disable-gpu", "--window-size=460,1040",
    f"--screenshot={png_dark}",
    dark_html_path
], check=True)

print("Screenshots captured successfully:")
print("Light:", png_light)
print("Dark:", png_dark)
