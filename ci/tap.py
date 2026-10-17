import re
import sys
import xml.etree.ElementTree as ET

path, text = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
for n in root.iter("node"):
    if n.get("text") == text or n.get("content-desc") == text:
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds"))
        l, t, r, b = map(int, m.groups())
        print((l + r) // 2, (t + b) // 2)
        break
