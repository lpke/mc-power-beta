#!/usr/bin/env python3
"""Build sequentially and export an account-free Prism instance with pinned dependencies."""
from pathlib import Path
import argparse,hashlib,json,shutil,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def export_markdown(source,target):
 text=(ROOT/source).read_text()
 base='https://github.com/lpke/mc-power-beta/blob/main/'
 text=text.replace('(docs/','('+base+'docs/').replace('(AGENTS.md)','('+base+'AGENTS.md)')
 text=text.replace('(music-assets.md)','('+base+'docs/music-assets.md)').replace('(LICENSE)','(LICENSE.txt)')
 target.write_text(text)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--skip-build',action='store_true');args=parser.parse_args()
 for entry in json.loads((ROOT/'vendor/libraries/manifest.json').read_text()):
  source=ROOT/entry['file']
  assert hashlib.sha256(source.read_bytes()).hexdigest()==entry['sha256'],f'Library changed: {source.name}'
 if not args.skip_build:
  subprocess.run(['bash','./gradlew','-p','input-api','--no-daemon','build'],cwd=ROOT,check=True)
  for component in ['creative','building','worldedit']:
   subprocess.run(['bash','./gradlew','--no-daemon','build'],cwd=ROOT/'modules'/component,check=True)
  subprocess.run(['python3','tools/build_utilities.py'],cwd=ROOT,check=True)
  subprocess.run(['bash','./gradlew','--no-daemon','build'],cwd=ROOT,check=True)
 out=ROOT/'dist';out.mkdir(exist_ok=True);stage=out/'Power Beta'
 if stage.exists():shutil.rmtree(stage)
 shutil.copytree(ROOT/'pack',stage)
 mods=stage/'.minecraft/mods';mods.mkdir(parents=True)
 manifest=json.loads((ROOT/'vendor/manifest.json').read_text());installed=[]
 platform={'smoothbeta','stationapi','entityculling','glassnetworking','retroauth','stapi-fast-intro'}
 for entry in manifest:
  if entry['id'] not in platform:continue
  assert entry['enabled'],f'Platform dependency disabled: {entry["id"]}'
  src=ROOT/entry['file'];assert hashlib.sha256(src.read_bytes()).hexdigest()==entry['sha256'],f'Artifact changed: {src.name}'
  shutil.copy2(src,mods/src.name)
 shutil.copy2(ROOT/'build/libs/power-beta-1.0.0.jar',mods/'power-beta-1.0.0.jar')
 defaults=ROOT/'src/main/resources/assets/powerbeta/defaults/power-beta.json'
 target=stage/'.minecraft/config/power-beta.json';target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(defaults,target)
 assert len(list(mods.glob('*.jar')))==7
 # Keep licenses beside the export, including separate upstream dependency licenses.
 licenses=stage/'licenses';licenses.mkdir();shutil.copy2(ROOT/'LICENSE',stage/'LICENSE.txt');export_markdown('README.md',stage/'README.md')
 for base in [ROOT/'modules',ROOT/'vendor/licenses']:
  for p in base.rglob('*'):
   if p.is_file() and not any(k in p.parts for k in ['build','.gradle']) and p.name.upper().startswith(('LICENSE','COPYING','NOTICE')):
    target=licenses/p.relative_to(base);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target)
 for p in (ROOT/'modules').glob('*/LICENSE'):shutil.copy2(p,licenses/(p.parent.name+'-LICENSE.txt'))
 shutil.copy2(ROOT/'vendor/manifest.json',stage/'COMPONENTS.json')
 export_markdown('docs/upstreams.md',stage/'UPSTREAMS.md')
 export_markdown('CHANGELOG.md',stage/'CHANGELOG.md')
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
