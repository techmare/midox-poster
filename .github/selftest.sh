#!/usr/bin/env bash
# Runs on the emulator in the "Screen check" workflow. Every adb call has a time limit.
set -x
PKG=ma.midoxshop.poster
A() { timeout 90 adb "$@"; }
mkdir -p shots

# Tap the first on-screen element whose content description contains $1 (or whose text is exactly $1).
tapdesc() {
  A shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  A pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'EOF'
import re, sys
x = open('/tmp/ui.xml', encoding='utf-8').read()
want = sys.argv[1]
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    d = re.search(r'content-desc="([^"]*)"', n)
    t = re.search(r' text="([^"]*)"', n)
    if (d and want in d.group(1)) or (t and t.group(1) == want):
        b = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
        print((b[0] + b[2]) // 2, (b[1] + b[3]) // 2)
        break
EOF
)
  echo "tapdesc '$1' -> $xy"
  [ -n "$xy" ] && A shell input tap $xy
}
# Tap the first on-screen element whose text contains $1 (for rows whose full text carries counts or dates).
taptext() {
  A shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  A pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'EOF2'
import re, sys, html
x = open('/tmp/ui.xml', encoding='utf-8').read()
want = sys.argv[1]
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    t = re.search(r' text="([^"]*)"', n)
    if t and want in html.unescape(t.group(1)):
        b = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
        print((b[0] + b[2]) // 2, (b[1] + b[3]) // 2)
        break
EOF2
)
  echo "taptext '$1' -> $xy"
  [ -n "$xy" ] && A shell input tap $xy
}
# Tap the first text field on screen (Compose fields are EditText to the screen reader).
tapfield() {
  A shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  A pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  local xy
  xy=$(python3 - <<'EOF3'
import re
x = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    if 'class="android.widget.EditText"' in n:
        b = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
        print((b[0] + b[2]) // 2, (b[1] + b[3]) // 2)
        break
EOF3
)
  echo "tapfield -> $xy"
  [ -n "$xy" ] && A shell input tap $xy
}
# The emulator is slow and System UI sometimes shows "isn't responding"; hide such dialogs and press Wait if one is up.
A shell settings put global hide_error_dialogs 1
undialog() {
  A shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  A pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  if grep -q "isn't responding\|isn&apos;t responding" /tmp/ui.xml 2>/dev/null; then
    tapdesc "Wait"; sleep 2
  fi
}
snap() { undialog; A exec-out screencap -p > "shots/$1.png"; }
# Tap $2 pixels below the element whose text is exactly $1 (for rows whose text the test can't know).
tapbelow() {
  A shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
  A pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
  local xy
  xy=$(python3 - "$1" "$2" <<'PY'
import re, sys
x = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    t = re.search(r' text="([^"]*)"', n)
    if t and t.group(1) == sys.argv[1]:
        b = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
        print(540, b[3] + int(sys.argv[2]))
        break
PY
)
  echo "tapbelow '$1' -> $xy"
  [ -n "$xy" ] && A shell input tap $xy
}

A devices
A shell getprop sys.boot_completed
A install -r app/build/outputs/apk/debug/app-debug.apk
# Granted up front so the permission questions don't cover the screenshots (the Needs you card is shot once without).
A shell pm grant $PKG android.permission.POST_NOTIFICATIONS
up() { A shell input swipe 540 1700 540 700 500; sleep 2; }

# 1. No Midox on the emulator: Today should say what to fix, not flash "Open Midox" forever.
A shell am start -n $PKG/.MainActivity
sleep 12
snap 00-no-midox
# Permissions for the alarm, then the demo products (debug builds only).
A shell appops set $PKG SYSTEM_ALERT_WINDOW allow
A shell appops set $PKG SCHEDULE_EXACT_ALARM allow
A shell appops set $PKG USE_FULL_SCREEN_INTENT allow

pass() {  # pass <suffix>: every screen once, by day or at night
  local s=$1
  A shell am force-stop $PKG
  A shell am start -n $PKG/.MainActivity --ez demo true
  sleep 10
  snap "01-today$s"
  up; snap "01b-today-scrolled$s"
  # Post mode on the first pick.
  A shell am force-stop $PKG
  A shell am start -n $PKG/.MainActivity --es go post
  sleep 8
  snap "02-post$s"
  taptext "Titre"; sleep 2
  snap "03-post-copied$s"
  up; snap "03b-post-bottom$s"
  # Facebook isn't on the emulator: Open Marketplace shows a message or a browser; come back.
  taptext "Open Marketplace"; sleep 4
  snap "04-after-open$s"
  A shell am start -n $PKG/.MainActivity; sleep 4
  snap "04b-back-in-post$s"
  taptext "I posted it"; sleep 5
  snap "05-done$s"
  taptext "I'm done for today"; sleep 4
  snap "06-today-after$s"
  # The 10 minutes between posts: Start posting counts down; post mode waits.
  taptext "Next post in"; sleep 5
  snap "07-post-waiting$s"
  A shell input keyevent KEYCODE_BACK; sleep 3
  taptext "Queue"; sleep 4
  snap "08-queue$s"
  taptext "Sat"; sleep 3
  snap "08b-queue-day$s"
  taptext "Posted"; sleep 4
  snap "09-posted$s"
  taptext "Live"; sleep 3
  taptext "Got messages"; sleep 2
  snap "09b-posted-noted$s"
  taptext "Settings"; sleep 4
  snap "10-settings$s"
  # Good day up to 10: the second "More" (+) button is Good day's. Two digits must show whole.
  if [ -z "$s" ]; then
    A shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; A pull /sdcard/ui.xml /tmp/ui.xml > /dev/null 2>&1
    xy=$(python3 -c "
import re
x=open('/tmp/ui.xml',encoding='utf-8').read()
m=[n for n in re.findall(r'<node [^>]*>',x) if 'content-desc=\"More\"' in n]
b=list(map(int,re.search(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"',m[1]).groups())) if len(m)>1 else None
print((b[0]+b[2])//2,(b[1]+b[3])//2) if b else print('')
")
    for i in 1 2 3 4 5 6 7 8 9; do [ -n "$xy" ] && A shell input tap $xy; sleep 0.4; done
    sleep 2; snap "10a-settings-goal-10"
  fi
  up; snap "10b-settings$s"
  up; snap "10c-settings$s"
  taptext "Try the alarm now"; sleep 6
  snap "11-alarm$s"
  taptext "Close"; sleep 3
}

pass ""
A shell cmd uimode night yes; sleep 3
pass "-dark"
A shell cmd uimode night no; sleep 2

# The opening animation from the icon, recorded by day and at night (launch-light.mp4, launch-dark.mp4).
for mode in no yes; do
  A shell cmd uimode night $mode; sleep 3
  A shell am force-stop $PKG
  timeout 30 adb shell screenrecord --time-limit 8 /sdcard/launch.mp4 &
  sleep 2
  A shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $PKG/.MainActivity
  wait
  A pull /sdcard/launch.mp4 "shots/launch-$([ $mode = yes ] && echo dark || echo light).mp4"
done
A shell cmd uimode night no
# After the opening, the screen must show its rows (the 2.5 s safety): a still a few seconds in.
sleep 4
snap 12-after-opening

A shell run-as $PKG cat shared_prefs/poster.xml > shots/prefs.txt 2>&1
A logcat -d -s AndroidRuntime:E > shots/crashes.txt
ls -la shots
