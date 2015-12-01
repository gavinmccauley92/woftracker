#!/usr/bin/env python

from html.parser import HTMLParser
from datetime import datetime
import os

class PuzzleParser(HTMLParser):
	def __init__(self, output_file):
		super().__init__(convert_charrefs=True)
		self.output_file = output_file
		self.withinDateDiv = []
		self.withinSolution = []
		self.withinCategory = []
	def handle_starttag(self, tag, attrs):
		if (tag == 'div' and  (self.withinDateDiv or attrs and attrs[0][1] == 'Plaque Month')) or \
			(self.withinDateDiv and tag == 'span' and attrs and attrs[0][1] == 'InsetA'):
			self.withinDateDiv.append(tag)
		if tag in ('li', 'span') and (self.withinSolution or attrs and attrs[0][1] == 'Solution'):
			self.withinSolution.append(tag)
		if self.withinSolution and tag == 'div' or (tag == 'span' and attrs and attrs[0][1] == 'Italic'):
			self.withinCategory.append(tag)
	def handle_data(self, data):
		if self.withinDateDiv and self.withinDateDiv[-1] == 'span':
			try:
				d = datetime.strptime(data.strip(), '%A %B %d, %Y')
				self.output_file.write(d.strftime('%y/%m/%d') + '\n')
				#print(d)
			except ValueError as e:
				pass
		elif self.withinCategory:
			try:
				self.output_file.write('\t' + data.upper().strip() + '\n')
			except UnicodeEncodeError as e:
				pass
				#print(self.output_file)
	def handle_endtag(self, tag):
		if self.withinDateDiv and tag == self.withinDateDiv[-1]:
			self.withinDateDiv.pop()
		if self.withinSolution and tag == self.withinSolution[-1]:
			self.withinSolution.pop()
		if self.withinCategory and tag == self.withinCategory[-1]:
			self.withinCategory.pop()

pp = PuzzleParser(open('all_puzzle_cats.txt', 'w'))
for fn in filter(lambda f : f.endswith('html'), os.listdir('puzzles')):
	with open('puzzles/' + fn, 'r') as f:
		pp.feed(f.read())