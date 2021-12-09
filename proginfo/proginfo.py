#!/usr/bin/python3
import os
import sys
import glob
import yaml
from string import Template

def main():
    print("Build completed")
    with open(findFile('boardconfig.yaml')) as file:
        fullfile = yaml.full_load(file)
        boards = fullfile['boards']
        boardFiles = {}
        if sys.argv[1] in boards:
            for param in boards[sys.argv[1]]:
                boardFiles[param] = findFile(boards[sys.argv[1]][param])
            render(boardFiles, findFile(boards[sys.argv[1]]['templateFile']))
        else:
            print("ERROR: Board " + sys.argv[1] + " not found in boardconfig.yaml")
            exit(1)
    print("")

# Utility Functions

def findFile(f, filter=""):
    r = glob.glob("../**/" + f, recursive=True)
    if filter != "":
        r = [s for s in r if filter in s]
    if not r:
        print("ERROR: Could not find file " + f)
        exit(1)
    return os.path.realpath(r[0])

def render(d, template):
    with open(os.path.join(os.getcwd(),template)) as f:
            src = Template(f.read())
            output = src.substitute(d)
            print(output)

if __name__ == "__main__":
    if len(sys.argv) <= 1:
        print("Board not supplied")
        exit(1)
    main()
