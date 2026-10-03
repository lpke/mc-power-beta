#!/usr/bin/env python3
"""Generate only the reviewed pack defaults, never values from a user's instance."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
SCHEMA=json.loads((ROOT/'docs/settings-schema.json').read_text())
CATALOG=json.loads((ROOT/'src/main/resources/assets/powerbeta/settings-catalog.json').read_text())
DEST=ROOT/'src/main/resources/assets/powerbeta/defaults'
ENUMS={
 'tweaks.placement.slabMode':['CONTINUOUS','DOUBLE','MATCH_FIRST'],
 'tweaks.placement.restrictionMode':['FACE','PLANE','LAYER','COLUMN','LINE','DIAGONAL'],
 'tweaks.placement.listMode':['NONE','BLACKLIST','WHITELIST'],
 'tweaks.freeLookPerspective':['THIRD_PERSON','FIRST_PERSON'],
 'tweaks.hotbar.alignment':['TOP_LEFT','TOP_RIGHT','BOTTOM_LEFT','BOTTOM_RIGHT','CENTER'],
}
def nested(root,path,value):
 parts=path.split('.')
 for part in parts[:-1]:root=root.setdefault(part,{})
 root[parts[-1]]=value

def yaml_map(data,indent=0):
 lines=[]
 for key,value in data.items():
  prefix=' '*indent+key+':'
  if isinstance(value,dict):lines.append(prefix+'\n'+yaml_map(value,indent+2).rstrip())
  else:lines.append(prefix+' '+json.dumps(value,ensure_ascii=False))
 return '\n'.join(lines)+'\n'

def generate():
 files={};gc={};props={'tweaks':{},'creative':{},'worldedit':{}};logo={};keys={}
 for s in SCHEMA:
  key=s['id'];backend=s['backend'];value=CATALOG.get(key,{}).get('default',s['defaultValue'])
  if ':' in backend:nested(gc.setdefault(backend,{}),key[len(backend)+1:],value)
  elif backend in props:
   if key=='worldedit.worldOverride':continue
   if key in ENUMS:value=ENUMS[key][int(value)]
   if backend=='creative' and key.endswith('Reach'):value=int(round(float(value)*10))
   props[backend][key[len(backend)+1:]]=value
  elif backend=='logo':nested(logo,key[5:],value)
  elif s['kind']=='KEY':keys['key_'+key[5:]]=int(value)
 for backend,data in gc.items():
  mod,name=backend.split(':');files[f'config/{mod}/{name}.yml']=yaml_map(data)
 for backend,data in props.items():
  name={'tweaks':'lpketweaks','creative':'lpkecreative','worldedit':'worldedit-beta'}[backend]
  files[f'config/{name}.properties']='# Power Beta defaults\n'+'\n'.join(k+'='+str(v).lower() if isinstance(v,bool) else k+'='+str(v) for k,v in sorted(data.items()))+'\n'
 files['config/oldLogo.json']=json.dumps(logo,indent=2)+'\n'
 files['config/power-beta/audio.json']=json.dumps({'master':100,'categories':dict.fromkeys(['blocks','hostile','passive','players','weather','ambient','interface','records'],100),'sounds':{},'musicMode':'VANILLA','musicDirectories':[],'menuDirectories':[],'recursive':False,'shuffle':True,'avoidRepeats':True},indent=2)+'\n'
 native={'music':'1.0','sound':'1.0','invertYMouse':'false','mouseSensitivity':'0.5','viewDistance':'0','guiScale':'0','bobView':'true','anaglyph3d':'false','advancedOpengl':'false','fpsLimit':'1','difficulty':'2','fancyGraphics':'true','ao':'true','skin':'Default','lastServer':'','fov':'0.0','clouds':'true','cloud_height':'0.0','fog_density':'0.5','brightness':'0.0','render_distance':'0.2','fps_limit':'0.4','gui_scale':'0.0'}
 native.update(keys);files['options.txt']='\n'.join(k+':'+str(v) for k,v in native.items())+'\n'
 for name,text in files.items():p=DEST/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 (DEST/'index.txt').write_text('\n'.join(sorted(files))+'\n');print('Wrote',len(files),'default files')
if __name__=='__main__':generate()
