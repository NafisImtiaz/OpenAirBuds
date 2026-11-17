import subprocess

dexdump = r"C:\Users\ymcad15\AppData\Local\Android\Sdk\build-tools\35.0.0\dexdump.exe"
out = subprocess.check_output([dexdump, "-d", "scratch/dex_extract/headset_classes.dex"], text=True, errors="replace")

pos = out.find("Class descriptor  : 'Lcom/realme/iot/headset/contract/bean/CustomEqInfoKt;'")
if pos != -1:
    print(out[pos:pos+3000])
