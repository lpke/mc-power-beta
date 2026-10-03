#!/usr/bin/env python3
"""Build the reviewed presentation catalog from the installed GCAPI schema audit."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
audit=json.loads((ROOT/'docs/upstream-settings.json').read_text())
def words(s):
 s=re.sub(r'([a-z0-9])([A-Z])',r'\1 \2',s).replace('_',' ')
 s=re.sub(r'\bCONFIG\b','',s,flags=re.I).strip();s=re.sub(r'\s+',' ',s)
 return s.capitalize()
def section(e):
 root=e['root'];s=e['id'].split('.',1)[1];group=e['group'].strip('.').split('.')[-1]
 base={
 'betterscreenshots:config':('Interface','Screenshots'),
 'entityculling:config':('Video','Visibility culling'),
 'freecam:config':('Camera','Free camera'),
 'goldentweaks:config':('Gameplay','Gold equipment abilities'),
 'hudtweaks:config':('Interface','HUD and chat'),
 'hudtweaks:hudpositions':('Interface',words(group) if group else 'HUD layout'),
 'inventorytweaks:inventoryTweaks':('Inventory',words(group) if group else 'Item controls'),
 'misctweaks:config':('Gameplay',words(group) if group else 'World behavior'),
 'mojangfixstationapi:config':('Fixes','Client fixes'),
 'quickadditions:config':('Gameplay',words(group) if group else 'World rules'),
 'station-api-configuration:config':('Interface','Loading screen'),
 'unitweaks:bugfixes':('Fixes','Game fixes'),
 'unitweaks:features':('Gameplay',words(group) if group else 'Optional mechanics'),
 'unitweaks:gameplay':('Gameplay','Interaction'),
 'unitweaks:general':('General','Game and input'),
 'unitweaks:oldfeatures':('Gameplay','Older mechanics'),
 'unitweaks:recipes':('Crafting',words(group) if group else 'Recipes'),
 'unitweaks:tweaks':('Gameplay','Blocks and entities'),
 'unitweaks:userinterface':('Interface',words(group) if group else 'Menus'),
 'unitweakstelsaddons:config':('Gameplay',words(group) if group else 'Optional mechanics'),
 }
 page,group=base[root]
 low=s.lower()
 if root=='quickadditions:config' and 'music_config' in low:page,group='Audio','Music and ambience'
 if root=='quickadditions:config' and 'missing_achievements' in low:page,group='Advanced','Achievement recovery'
 if root=='unitweaks:features' and ('sound' in low or 'fallingpipe' in low):page,group='Audio','Extra sounds'
 if root=='unitweakstelsaddons:config' and ('debugoverlay' in low or 'overlayadditions' in low):page,group='Interface','Debug information'
 if 'particles_config' in low:page,group='Video','Particles'
 if 'stack_size_config' in low:page,group='Inventory','Stack sizes'
 if 'photo' in low and root=='unitweaks:userinterface':group='Screenshots'
 if 'videosettings' in low:page,group='Video','Available controls'
 if 'panorama' in low:group='Title background'
 if 'versiontext' in low:group='Version label'
 if 'frontviewthirdperson' in low:page,group='Camera','Third person'
 if root=='mojangfixstationapi:config' and ('resources' in low or 'authentication' in low):page,group='Advanced','Resource and account services'
 if root=='unitweaks:general' and ('resource' in low or 'disableddimensions' in low or 'tcp' in low):page,group='Advanced','Network and resources'
 return page,group
out={}
for e in audit:
 i=e['id'];root=e['root'];leaf=i.rsplit('.',1)[-1];page,group=section(e)
 label=e['label'].replace('Enable ','').replace(' Enabled','').replace('Default Value: ','')
 label=label.replace('(Restart required)','').strip()
 label=label.replace('MojangFix Text','Compatibility credits').replace('UniTweaks','Game').replace('Tweaks','controls')
 label=label[:1].upper()+label[1:].lower()
 # Preserve familiar acronyms without title-casing every word.
 for a in ['hud','fov','fps','tnt','tcp','f3','ip','gui','ctrl']:
  label=re.sub(r'\b'+a+r'\b',a.upper(),label)
 description=e['description'].replace('UniTweaks','the game settings').replace('StationAPI','extended block identifiers').replace('MojangFix','client fixes')
 description=re.sub(r'Default Value: .*','',description).strip()
 default=e['default']
 if root.startswith('goldentweaks:') and isinstance(default,bool):default=False
 if root in ['unitweaks:tweaks','unitweaks:recipes','unitweaks:oldfeatures'] and isinstance(default,bool):default=False
 if root=='unitweaks:features' and isinstance(default,bool):default=False
 if root=='misctweaks:config' and isinstance(default,bool):default=False
 if root=='quickadditions:config' and isinstance(default,bool):default=False
 if root=='quickadditions:config' and '.SPAWN_LIST_CONFIG.' in i:default=True
 if root=='unitweakstelsaddons:config' and leaf in ['enableShovelsEffectiveOnSoulSand','slabPlacementFixesEnabled']:default=False
 if root=='unitweaks:gameplay' and leaf=='noFoodWastage':default=False
 if root=='unitweaks:userinterface' and leaf in ['enablePanorma','showVersionTextIngame']:default=False
 if leaf=='customVersionText':default='Power Beta | Minecraft Beta 1.7.3'
 d={'page':page,'group':group,'label':label,'description':description,'default':default,'restart':e['restart'] or 'restart required' in e['description'].lower()}
 # Recipe and registry changes are evaluated during startup in these versions.
 if root=='unitweaks:recipes' or leaf in ['ENABLE_SOUL_SAND_RECIPE','SOUL_SAND_OUTPUT','enableLogRotation','enableModernWoodDoorStackSize','enableModernIronDoorStackSize','enableModernSignStackSize']:d['restart']=True
 if e['type'] in ['java.lang.Integer','java.lang.Float']:
  lo,hi=e['min'],e['max'];step=1 if e['type'].endswith('Integer') else .05
  if 'Offset' in leaf or leaf.endswith('YOffset'):lo,hi=-2048,2048
  if 'PhotoHeight' in leaf or 'PhotoWidth' in leaf:lo,hi=16,16384
  if leaf=='isometricPhotoScale':lo,hi=1,32
  if leaf=='isometricPhotoRotationOffset':lo,hi=-360,360
  if leaf=='chatHistorySize':lo,hi=1,10000
  if leaf in ['chatFadeTime','hotbarItemSelectionFadeTime']:lo,hi=1,12000
  if leaf=='chanceBlocksDropWhenExploded' or leaf.startswith('volume') or leaf=='asleepPlayerPercentageForSkippingNight':lo,hi=0,1
  if leaf=='appleDropChance':lo,hi=0,100
  if leaf=='monstersSpawnAtOrBelowLightLevel':lo,hi=0,15
  if leaf=='SPAWN_GROUP_SIZE':lo,hi=1,64
  if leaf.startswith('MAX_SPAWN_CAPACITY'):lo,hi=0,1024
  if leaf.startswith('musicCoundown') or 'RandomLimit' in leaf or leaf.endswith('Minimum'):lo,hi=1,2400000
  if leaf=='bedsSpeedUpNightTickRate':lo,hi=1,100
  if leaf=='alwaysSnowAboveThisYLevel':lo,hi=0,128
  if leaf=='SPAWN_PROTECTION_RADIUS':lo,hi=0,128
  if leaf=='SOUL_SAND_OUTPUT':lo,hi=1,16
  if leaf in ['stairsPerCraft','trapdoorsPerCraft']:lo,hi=1,64
  if leaf=='autosaveInterval':lo,hi=1,3600;d['restart']=True
  if leaf=='speed' and root=='freecam:config':lo,hi,step=0.1,100,.1
  if leaf=='drag' and root=='freecam:config':lo,hi,step=0.1,100,.1
  d.update(min=lo,max=hi,step=step)
 out[i]=d
overrides=ROOT/'src/main/resources/assets/powerbeta/settings-overrides.json'
if overrides.exists():
 for key,value in json.loads(overrides.read_text()).items():out.setdefault(key,{}).update(value)
p=ROOT/'src/main/resources/assets/powerbeta/settings-catalog.json';p.write_text(json.dumps(out,indent=2)+'\n')
print(f'Wrote {len(out)} reviewed setting entries')
