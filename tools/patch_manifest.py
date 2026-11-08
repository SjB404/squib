#!/usr/bin/env python3
"""Sets versionCode and versionName in a compiled (binary) AndroidManifest.xml.

Used by tools/build-apk.sh when it can't run apktool's resource packer: it reuses the
compiled manifest and resources from the last APK and only changes the version.

usage: patch_manifest.py AndroidManifest.xml VERSION_CODE VERSION_NAME
"""
import struct
import sys

VERSION_CODE = 0x0101021B  # android:versionCode
VERSION_NAME = 0x0101021C  # android:versionName
TYPE_STRING, TYPE_INT = 0x03, 0x10


def read_pool(d, off):
    _, hsize, size, count, styles, flags, sstart, _ = struct.unpack_from('<HHIIIIII', d, off)
    if styles:
        sys.exit('string pool has styles: not supported')
    utf8 = bool(flags & 0x100)
    offs = struct.unpack_from('<%dI' % count, d, off + hsize)
    out = []
    for o in offs:
        p = off + sstart + o
        if utf8:
            n = d[p]
            p += 2 if n & 0x80 else 1
            n = d[p] if not d[p] & 0x80 else ((d[p] & 0x7F) << 8) | d[p + 1]
            p += 2 if d[p] & 0x80 else 1
            out.append(d[p:p + n].decode('utf-8'))
        else:
            n = struct.unpack_from('<H', d, p)[0]
            p += 2
            if n & 0x8000:
                n = ((n & 0x7FFF) << 16) | struct.unpack_from('<H', d, p)[0]
                p += 2
            out.append(d[p:p + 2 * n].decode('utf-16-le'))
    return out, flags, size


def write_pool(strings, flags):
    if flags & 0x100:
        sys.exit('UTF-8 string pool: not supported')
    data, offs = b'', []
    for s in strings:
        offs.append(len(data))
        b = s.encode('utf-16-le')
        n = len(b) // 2
        data += (struct.pack('<HH', 0x8000 | (n >> 16), n & 0xFFFF) if n > 0x7FFF else struct.pack('<H', n)) + b + b'\0\0'
    data += b'\0' * (-len(data) % 4)
    hsize = 28
    sstart = hsize + 4 * len(strings)
    size = sstart + len(data)
    return struct.pack('<HHIIIIII', 0x0001, hsize, size, len(strings), 0, flags, sstart, 0) + \
        struct.pack('<%dI' % len(strings), *offs) + data


def main():
    path, code, name = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    d = bytearray(open(path, 'rb').read())
    if struct.unpack_from('<H', d, 0)[0] != 0x0003:
        sys.exit('not a binary manifest')
    strings, flags, pool_size = read_pool(d, 8)
    rest = d[8 + pool_size:]

    # resource id map: string index -> attribute id
    ids = []
    if struct.unpack_from('<H', rest, 0)[0] == 0x0180:
        sz = struct.unpack_from('<I', rest, 4)[0]
        ids = list(struct.unpack_from('<%dI' % ((sz - 8) // 4), rest, 8))

    # first start-element (the <manifest> tag)
    p = 0
    while p < len(rest):
        t, h, sz = struct.unpack_from('<HHI', rest, p)
        if t == 0x0102:
            break
        p += sz
    else:
        sys.exit('no <manifest> element')
    attr_start, attr_size, attr_count = struct.unpack_from('<HHH', rest, p + 16 + 8)
    seen = set()
    for i in range(attr_count):
        a = p + 16 + attr_start + i * attr_size
        name_idx = struct.unpack_from('<I', rest, a + 4)[0]
        rid = ids[name_idx] if name_idx < len(ids) else None
        if rid == VERSION_CODE:
            struct.pack_into('<I', rest, a + 8, 0xFFFFFFFF)
            struct.pack_into('<HBBI', rest, a + 12, 8, 0, TYPE_INT, code)
            seen.add('code')
        elif rid == VERSION_NAME:
            strings.append(name)
            idx = len(strings) - 1
            struct.pack_into('<I', rest, a + 8, idx)
            struct.pack_into('<HBBI', rest, a + 12, 8, 0, TYPE_STRING, idx)
            seen.add('name')
    if seen != {'code', 'name'}:
        sys.exit('versionCode/versionName not found in <manifest>')

    pool = write_pool(strings, flags)
    body = pool + bytes(rest)
    out = struct.pack('<HHI', 0x0003, 8, 8 + len(body)) + body
    open(path, 'wb').write(out)


if __name__ == '__main__':
    main()
