#!/usr/bin/env python3
"""Apply narrow, reproducible integration patches to hash-pinned third-party artifacts."""
from pathlib import Path
import zipfile,json,hashlib
ROOT=Path(__file__).resolve().parents[1]
def patch():
 source=ROOT/'vendor/jars/QuickAdditions-2.12.0.jar';dest=ROOT/'build/patched/QuickAdditions-2.12.0-powerbeta.jar';dest.parent.mkdir(parents=True,exist_ok=True)
 manifest=json.loads((ROOT/'vendor/manifest.json').read_text())
 records=manifest if isinstance(manifest,list) else manifest.get('mods',[])
 expected=next(x for x in records if x.get('id')=='quickadditions')
 digest=hashlib.sha256(source.read_bytes()).hexdigest()
 assert digest==expected['sha256'],'Upstream artifact changed; review the patch before proceeding'
 removed={'client.SoundManagerMixin','client.TitleScreenMixin'}
 with zipfile.ZipFile(source) as z,zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED) as out:
  for info in z.infolist():
   data=z.read(info.filename)
   if info.filename=='quickadditions.mixins.json':
    obj=json.loads(data);found=set()
    for section in ['client','mixins']:
     found.update(set(obj.get(section,[]))&removed);obj[section]=[m for m in obj.get(section,[]) if m not in removed]
    assert found==removed,'Audio mixin layout changed'
    data=json.dumps(obj,indent=2).encode()
   if info.filename=='fabric.mod.json':
    obj=json.loads(data);obj['version']=obj['version']+'+powerbeta.1';data=json.dumps(obj,indent=2).encode()
   fixed=zipfile.ZipInfo(info.filename,(1980,1,1,0,0,0));fixed.compress_type=zipfile.ZIP_DEFLATED;out.writestr(fixed,data)
 print(dest)
if __name__=='__main__':patch()
