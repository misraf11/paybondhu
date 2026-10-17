#!/bin/bash
# Runs inside the emulator job. Never fails the job: everything is written to e2e-out/ for inspection.
PKG=com.paybondhu.app
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=e2e-out
mkdir -p "$OUT"

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$1.xml" > /dev/null 2>&1; }
tap_text() {
  dump tmp
  xy=$(python3 ci/tap.py "$OUT/tmp.xml" "$1")
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "tapped '$1' at $xy"; else echo "NOT FOUND: '$1'"; fi
}
sms() { adb emu sms send "$1" "$2" > /dev/null; }

adb wait-for-device
adb shell settings put global window_animation_scale 0 || true
adb shell input keyevent 82 || true
adb install -r "$APK" 2>&1 | tail -1
for p in RECEIVE_SMS READ_SMS POST_NOTIFICATIONS; do adb shell pm grant $PKG android.permission.$p; done
adb shell dumpsys deviceidle whitelist +$PKG > /dev/null
adb logcat -c

# 1. Login screen with deep-link prefill
adb shell "am start -a android.intent.action.VIEW -d 'paybondhu://connect?email=shop@example.com&key=TESTKEY0123456789TESTKEY0123456789TESTKEY' $PKG"
sleep 6; shot 01-login; dump 01-login
tap_text "Connect"
sleep 10; shot 02-home-empty; dump 02-home-empty

# 2. Incoming SMS
sms bKash "You have received Tk 1,250.00 from 01712345678. Fee Tk 0.00. Balance Tk 5,300.50. TrxID BKX7A1B2C3 at 17/10/2026 14:32"
sleep 3
sms Nagad "Money Received. Amount: Tk 500.00 Sender: 01898765432 Ref: N/A TxnID: NGD91ZQ4ML Balance: Tk 2,100.00 17/10/2026 14:35"
sleep 3
sms bKash "Your bKash verification code is 482913. Do not share it."
sleep 3
sms bKash "You have received Tk 20.00 from 01711111111. Fee Tk 0.00. Balance Tk 90.00. TrxID REJECTME9A1 at 17/10/2026 14:38"
sleep 3
sms 01700000000 "Hi, TrxID ABCDEF12345 Tk 100 from a friend"
sleep 15
shot 03-home; dump 03-home

# 3. Pull a full sync so the server's "used" flag is applied
tap_text "Sync now"
sleep 8
shot 04-home-synced; dump 04-home-synced

# 4. Notification shade
adb shell cmd statusbar expand-notifications
sleep 3; shot 05-notification
adb shell dumpsys notification --noredact 2>/dev/null | grep -A12 "pkg=$PKG" | head -40 > "$OUT/notification.txt"
adb shell cmd statusbar collapse
sleep 1

# 5. History tab, then a transaction detail
tap_text "History"
sleep 3; shot 06-history; dump 06-history
tap_text "+৳1,250.00"
sleep 3; shot 07-detail; dump 07-detail
adb shell input keyevent 4
sleep 1

# 6. Settings
tap_text "Settings"
sleep 3; shot 08-settings; dump 08-settings
adb shell input swipe 540 1800 540 500 300
sleep 1; shot 09-settings-bottom

# 7. Service state, crash check, what reached the server
adb shell dumpsys activity services $PKG 2>/dev/null | grep -E "ServiceRecord|isForeground|foregroundId|types" | head -10 > "$OUT/service.txt"
adb logcat -d > "$OUT/logcat.txt" 2>&1
grep -E "FATAL EXCEPTION|AndroidRuntime" "$OUT/logcat.txt" | head -20 > "$OUT/crashes.txt"
echo "e2e script finished"
exit 0
