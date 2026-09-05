#!/usr/bin/env python3
"""Run against a disposable emulator. Clears Auto Skip data; restores accessibility settings."""
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SERIAL = sys.argv[1] if len(sys.argv) > 1 else "emulator-5554"
if not re.fullmatch(r"emulator-\d+", SERIAL):
    raise SystemExit("Use a disposable emulator; this check clears Auto Skip's app data.")
ADB = str(Path(os.environ["ANDROID_HOME"]) / "platform-tools/adb") if "ANDROID_HOME" in os.environ else "adb"
PACKAGE = "com.ytadsskipper.app"
SERVICE = PACKAGE + "/.SkipService"
OUT = ROOT / "app/build/smoke"
OUT.mkdir(parents=True, exist_ok=True)


def adb(*args):
    return subprocess.check_output([ADB, "-s", SERIAL, *args], text=True, timeout=30).strip()


def setting(key, value):
    if value in ("null", ""):
        adb("shell", "settings", "delete", "secure", key)
    else:
        adb("shell", "settings", "put", "secure", key, value)


def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/autoskip-check.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/autoskip-check.xml"))


def wait_text(text):
    for _ in range(6):
        tree = hierarchy()
        if any(node.get("text") == text for node in tree.iter("node")):
            return
        time.sleep(0.5)
    raise AssertionError(f"Screen did not show {text!r}")


def tap(resource):
    for _ in range(4):
        tree = hierarchy()
        for node in tree.iter("node"):
            if node.get("resource-id") == PACKAGE + ":id/" + resource:
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
                if x2 > x1 and y2 > y1:
                    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                    return
        width, height = map(int, re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))[-1])
        adb("shell", "input", "swipe", str(width // 2), str(height * 4 // 5), str(width // 2), str(height // 3), "300")
    raise AssertionError(f"Control not found: {resource}")


def launch():
    adb("shell", "am", "start", "-W", "-f", "0x14000000", "-n", PACKAGE + "/.MainActivity")


def screenshot(name):
    with (OUT / name).open("wb") as image:
        subprocess.run([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"], stdout=image, check=True, timeout=30)


old_services = adb("shell", "settings", "get", "secure", "enabled_accessibility_services")
old_enabled = adb("shell", "settings", "get", "secure", "accessibility_enabled")
try:
    setting("enabled_accessibility_services", "null")
    setting("accessibility_enabled", "0")
    adb("install", "-r", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk"))
    adb("install", "-r", str(ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"))
    adb("shell", "am", "force-stop", PACKAGE)
    output = adb("shell", "am", "instrument", "-w", PACKAGE + ".test/" + PACKAGE + ".SmokeInstrumentation")
    print(output)
    assert "PASS:" in output and "FAIL:" not in output, "Instrumentation checks failed"
    adb("shell", "pm", "clear", PACKAGE)
    launch()
    wait_text("One permission. You’re in control.")
    screenshot("onboarding.png")
    tap("not_now")
    wait_text("Setup needed")
    screenshot("paused.png")
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    launch()
    wait_text("Setup needed")
    assert not any(n.get("resource-id") == PACKAGE + ":id/open_settings" for n in hierarchy().iter("node")), "Dismissed sheet reopened"
    tap("auto_skip")
    wait_text("One permission. You’re in control.")
    tap("open_settings")
    # Shell permission grants are for this disposable emulator only. Real users enable access in Settings.
    setting("enabled_accessibility_services", SERVICE)
    setting("accessibility_enabled", "1")
    time.sleep(1)
    launch()
    wait_text("Ready to skip")
    screenshot("ready.png")
    tap("auto_skip")
    wait_text("Paused")
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    launch()
    wait_text("Paused")
    setting("enabled_accessibility_services", "null")
    setting("accessibility_enabled", "0")
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    launch()
    wait_text("Setup needed")
    print("PASS: settings return, Ready, pause persistence, permission revocation.")
    print(f"Screenshots: {OUT}")
finally:
    setting("enabled_accessibility_services", old_services)
    setting("accessibility_enabled", old_enabled)
    adb("shell", "rm", "-f", "/sdcard/autoskip-check.xml")
