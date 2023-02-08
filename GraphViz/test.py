import graphviz
from graphviz import dot, Digraph

#g = graphviz.Digraph('G', 'test.dot')
#f = open("test.svg", "w")

src = graphviz.Source.from_file("test.dot")

#src.format = 'svg'
src.engine = 'twopi'


#src.scale = 0.1
src.render('test.dot', view=True).replace('\\', '/')
#f.write(g.source)
#print(g.source)