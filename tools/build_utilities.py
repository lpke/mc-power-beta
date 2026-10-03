#!/usr/bin/env python3
"""Compile the in-house utility modules against the pinned platform API."""
from pathlib import Path
import argparse,json,os,subprocess
from prepare_platform import main as prepare_platform
ROOT=Path(__file__).resolve().parents[1]
def main():
 parser=argparse.ArgumentParser();parser.add_argument('modules',nargs='*');args=parser.parse_args()
 prepare_platform();catalog=json.loads((ROOT/'source-build/modules.json').read_text());logs=ROOT/'build/utility-logs';logs.mkdir(parents=True,exist_ok=True)
 for name in args.modules or catalog:
  assert name in catalog
  print('Building '+name,flush=True)
  with (logs/(name+'.log')).open('w') as log:
   result=subprocess.run(['bash','./gradlew','-p','source-build','-Pmodule='+name,'--no-daemon','build'],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
  if result.returncode:print((logs/(name+'.log')).read_text()[-14000:]);raise SystemExit(result.returncode)
  print('Built '+name,flush=True)
if __name__=='__main__':main()
