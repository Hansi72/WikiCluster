import sys
import graphviz
#Takes a dot file as std input and outputs a SVG file in bytes.
byteInput = sys.stdin.read().encode('utf-8', 'replace')
src = graphviz.Source(byteInput.decode())
src.format = 'svg'
print(src.pipe(encoding='utf-8'))
