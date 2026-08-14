#!/usr/bin/env python3
"""cities.prop 数据时效检查(纯标准库,离线)

检查项:
1. 时区字符串有效性(zoneinfo)
2. 时区 UTC 偏移与经度的一致性(|经度/15 - 时区偏移| > 3 小时视为可疑,
   容忍夏令时 ±1;东经 116° 配 UTC-5 这类配置直接暴露)
3. 经纬度越界
4. 城市记录统计(简繁两段 + 地图城市)
"""
import zoneinfo
import datetime

path = 'cities.prop'
txt = open(path, 'rb').read().decode('utf-16')
lines = txt.splitlines()

now = datetime.datetime.now(datetime.timezone.utc)
rows = []
for ln in lines:
    s = ln.strip()
    if not s or s.startswith('#'):
        continue
    parts = s.split('|')
    if len(parts) != 5:
        continue  # DST 历史段(时区|起|止|偏移)等非城市行

    country, city, lon_s, lat_s, tz = [p.strip() for p in parts]
    try:
        lon, lat = float(lon_s), float(lat_s)
    except ValueError:
        print(f'[坐标非法] {country}|{city} lon={lon_s} lat={lat_s}')
        continue
    rows.append((country, city, lon, lat, tz))

print(f'数据行数: {len(rows)}')

bad_tz, offset_suspect, coord_range = 0, 0, 0
suspects = []
for country, city, lon, lat, tz in rows:
    # 1) 时区有效
    try:
        if tz == 'Unknown':
            continue  # 程序占位符(用户手选时区)
        zi = zoneinfo.ZoneInfo(tz)
    except Exception:
        bad_tz += 1
        if bad_tz <= 10:
            print(f'[时区无效] {country}|{city} tz={tz}')
        continue
    # 2) 经度与时区偏移一致性
    off = zi.utcoffset(now)
    off_h = (off.days * 86400 + off.seconds) / 3600.0
    expect = round(lon / 15.0)
    diff = abs(off_h - expect)
    if diff > 3.5:  # 容忍半时区+夏令时
        offset_suspect += 1
        if len(suspects) < 25:
            suspects.append(f'{country}|{city} lon={lon} tz={tz} UTC{off_h:+.0f} 预期≈UTC{expect:+d}')
    # 3) 越界
    if not (-180 <= lon <= 180 and -90 <= lat <= 90):
        coord_range += 1
        print(f'[坐标越界] {country}|{city} {lon},{lat}')

print(f'时区无效: {bad_tz}, 偏移可疑: {offset_suspect}, 坐标越界: {coord_range}')
if suspects:
    print('\n-- 经度与时区偏移不一致的可疑城市(前 25)--')
    for s in suspects:
        print(' ', s)
