import sys
import graphviz
#Takes a dot file as std input and outputs a SVG file in bytes.
src = graphviz.Source(sys.stdin.read())
src.format = 'svg'
print(src.pipe(encoding='utf-8'))
