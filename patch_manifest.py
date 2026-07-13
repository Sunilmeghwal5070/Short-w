import re

with open("/app/applet/app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

perms = """    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
"""

content = content.replace('<application', perms + '    <application')

with open("/app/applet/app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
