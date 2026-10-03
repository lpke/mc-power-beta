#!/usr/bin/env python3
"""Build sequentially and export an account-free Prism instance with pinned dependencies."""
from pathlib import Path
import argparse,hashlib,json,shutil,subprocess,zipfile
from patch_vendor import patch
ROOT=Path(__file__).resolve().parents[1]
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--skip-build',action='store_true');args=parser.parse_args()
 for entry in json.loads((ROOT/'vendor/libraries/manifest.json').read_text()):
  source=ROOT/entry['file']
  assert hashlib.sha256(source.read_bytes()).hexdigest()==entry['sha256'],f'Library changed: {source.name}'
 if not args.skip_build:
  subprocess.run(['bash','./gradlew','-p','input-api','--no-daemon','build'],cwd=ROOT,check=True)
  for component in ['mc-lpkecreative','mc-lpketweaks','mc-worldedit-beta']:
   subprocess.run(['bash','./gradlew','--no-daemon','build'],cwd=ROOT/'components'/component,check=True)
  subprocess.run(['python3','tools/build_utilities.py'],cwd=ROOT,check=True)
  subprocess.run(['bash','./gradlew','--no-daemon','build'],cwd=ROOT,check=True)
 patch()
 out=ROOT/'dist';out.mkdir(exist_ok=True);stage=out/'Power Beta'
 if stage.exists():shutil.rmtree(stage)
 shutil.copytree(ROOT/'pack',stage)
 mods=stage/'.minecraft/mods';mods.mkdir(parents=True)
 manifest=json.loads((ROOT/'vendor/manifest.json').read_text());installed=[]
 utilities=json.loads((ROOT/'source-build/modules.json').read_text())
 for entry in manifest:
  if entry['id'] in utilities:
   if utilities[entry['id']]['enabled']:continue
   module=utilities[entry['id']]
   src=ROOT/'source-build/build'/entry['id']/'libs'/(entry['id']+'-'+module['version']+'+powerbeta.1.jar')
   shutil.copy2(src,mods/(src.name+'.disabled'));continue
  src=ROOT/entry['file'];assert hashlib.sha256(src.read_bytes()).hexdigest()==entry['sha256'],f'Artifact changed: {src.name}'
  if entry['id']=='gcapi3':src=ROOT/'build/patched'/(src.stem+'-powerbeta.jar')
  shutil.copy2(src,mods/src.name)
 shutil.copy2(ROOT/'build/libs/power-beta-1.0.0.jar',mods/'power-beta-1.0.0.jar')
 defaults=ROOT/'src/main/resources/assets/powerbeta/defaults'
 for name in (defaults/'index.txt').read_text().splitlines():
  target=stage/'.minecraft'/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(defaults/name,target)
 # Keep notices beside the export, including separate upstream license files.
 licenses=stage/'licenses';licenses.mkdir();shutil.copy2(ROOT/'NOTICE',stage/'NOTICE.txt');shutil.copy2(ROOT/'README.md',stage/'README.md')
 for base in [ROOT/'vendor/sources',ROOT/'vendor/licenses']:
  for p in base.rglob('*'):
   if p.is_file() and p.name.upper().startswith(('LICENSE','COPYING','NOTICE')):
    target=licenses/p.relative_to(base);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
 for p in (ROOT/'components').glob('*/LICENSE'):shutil.copy2(p,licenses/(p.parent.name+'-LICENSE.txt'))
 shutil.copy2(ROOT/'vendor/manifest.json',stage/'COMPONENTS.json')
 shutil.copy2(ROOT/'docs/upstreams.md',stage/'UPSTREAMS.md')
 shutil.copy2(ROOT/'CHANGELOG.md',stage/'CHANGELOG.md')
 for p in sorted(mods.iterdir()):installed.append({'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'enabled':p.suffix=='.jar'})
 (stage/'PACK-MANIFEST.json').write_text(json.dumps({'name':'Power Beta','version':'1.0.0','mods':installed},indent=2)+'\n')
 forbidden={'accounts.json','level.dat','session.lock','servers.dat','power-beta-validation.command'}
 assert not any(p.name in forbidden for p in stage.rglob('*'))
 assert not any('validation' in p.name or 'modmenu' in p.name.lower() for p in mods.iterdir())
 archive=out/'Power-Beta-1.0.0-Prism.zip'
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for p in sorted(stage.rglob('*')):
   if p.is_file():info=zipfile.ZipInfo(str(p.relative_to(out)),(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,p.read_bytes())
 digest=hashlib.sha256(archive.read_bytes()).hexdigest();(out/(archive.name+'.sha256')).write_text(digest+'  '+archive.name+'\n');print(archive,digest)
if __name__=='__main__':main()
