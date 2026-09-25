#!/usr/bin/env python3
"""生成「反作弊字体映射」字体。

把常见字符集（GB2312 一级汉字 + 可打印 ASCII）按给定 seed 随机打乱，
产出一个字体：font 的 cmap[显示码点] = 真实字符的字形。

前端只拿到「显示码点」拼成的文本 + 该字体：
  - 渲染色形仍是原字（学生看到的题干正常）；
  - 但 DOM 中的文本是乱码，复制出去无法直接用于搜题。

用法: exam_font_gen.py <seed> <out_dir>
输出:
  <out_dir>/exam_font.woff2   打乱后的字体
  <out_dir>/mapping.json      {"seed": <int>, "pairs": {"<真实码点>": <显示码点>, ...}}
"""
import json
import os
import random
import sys


def build_charset():
    """GB2312 一级汉字（约 3755 个常用简体字）+ 可打印 ASCII。"""
    cjk = set()
    for b1 in range(0xB0, 0xD8):
        for b2 in range(0xA1, 0xFF):
            try:
                ch = bytes([b1, b2]).decode('gb2312')
            except Exception:
                continue
            if len(ch) == 1:
                cjk.add(ch)
    ordered = sorted(cjk)
    ordered += [chr(c) for c in range(0x20, 0x7F)]
    return ordered


def find_base_font():
    candidates = [
        '/usr/share/fonts/truetype/wqy/wqy-microhei.ttc',
        '/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc',
        '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc',
        '/usr/share/fonts-droid-fallback/truetype/DroidSansFallback.ttf',
        '/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf',
    ]
    for path in candidates:
        if os.path.exists(path):
            return path
    raise SystemExit('exam_font_gen: no base CJK font found')


def main():
    if len(sys.argv) != 3:
        raise SystemExit('usage: exam_font_gen.py <seed> <out_dir>')
    seed = int(sys.argv[1])
    out_dir = sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)

    from fontTools.ttLib import TTFont, TTCollection
    from fontTools.subset import Subsetter, Options
    from fontTools.ttLib.tables._c_m_a_p import CmapSubtable

    charset = build_charset()
    rnd = random.Random(seed)
    shuffled = charset[:]
    rnd.shuffle(shuffled)
    perm = {real: shuffled[i] for i, real in enumerate(charset)}  # real -> display

    base_path = find_base_font()
    if base_path.endswith('.ttc'):
        font = TTCollection(base_path, lazy=True).fonts[0]
    else:
        font = TTFont(base_path, lazy=True)

    # 仅保留本字符集的字形，显著缩小输出体积
    options = Options()
    options.glyph_names = False
    subsetter = Subsetter(options=options)
    subsetter.populate(unicodes=[ord(c) for c in charset])
    subsetter.subset(font)

    src_cmap = font.getBestCmap()
    new_cmap = {}
    pairs = {}
    for real in charset:
        real_code = ord(real)
        if real_code not in src_cmap:
            continue
        display_code = ord(perm[real])
        new_cmap[display_code] = src_cmap[real_code]
        pairs[str(real_code)] = display_code

    cmap_table = CmapSubtable.newSubtable(4)
    cmap_table.platformID = 3
    cmap_table.platEncID = 1
    cmap_table.language = 0
    cmap_table.cmap = new_cmap
    font['cmap'].tables = [cmap_table]

    # 关键：post 表 3.0 不输出任何字形名，避免 uniXXXX / cidXXXX 泄露真实码点。
    # 否则作弊者直接读取字体的字形名即可还原文字。
    # （glyf 表按字形顺序而非名字索引，二进制中本就不含名字，post 是唯一的名字来源。）
    if 'post' in font:
        font['post'].formatType = 3.0

    font.flavor = 'woff2'
    font.save(os.path.join(out_dir, 'exam_font.woff2'))

    with open(os.path.join(out_dir, 'mapping.json'), 'w', encoding='utf-8') as f:
        json.dump({'seed': seed, 'pairs': pairs}, f, ensure_ascii=False)


if __name__ == '__main__':
    main()
