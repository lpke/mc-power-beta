"""Normalize build metadata for the pinned Loom toolchain; runtime artifacts stay untouched."""
from pathlib import Path
import hashlib,io,json,zipfile,re
ROOT=Path(__file__).resolve().parents[1]
def normalize(data):
 output=io.BytesIO()
 with zipfile.ZipFile(io.BytesIO(data)) as source, zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as dest:
  for entry in source.infolist():
   content=source.read(entry.filename)
   if entry.filename.endswith('.jar'):content=normalize(content)
   elif entry.filename=='META-INF/MANIFEST.MF':content=re.sub(rb'Fabric-Loom-Version: [^\r\n]+',b'Fabric-Loom-Version: 1.11.8',content)
   elif entry.filename.endswith('.accesswidener'):content=content.replace(b'classTweaker v1 ',b'accessWidener v2 ',1)
   dest.writestr(entry,content)
 return output.getvalue()
def main():
 out=ROOT/'build/platform-compile';out.mkdir(parents=True,exist_ok=True)
 for mod in json.loads((ROOT/'vendor/manifest.json').read_text()):
  if mod['id'] not in {'stationapi','gcapi3','glassnetworking'}:continue
  src=ROOT/mod['file'];assert hashlib.sha256(src.read_bytes()).hexdigest()==mod['sha256']
  data=normalize(src.read_bytes())
  (out/src.name).write_bytes(data)
  def extract_nested(data):
   with zipfile.ZipFile(io.BytesIO(data)) as jar:
    for name in jar.namelist():
     if name.endswith('.jar'):
      nested=jar.read(name);target=out/name.split('/')[-1]
      if target.exists() and target.read_bytes()!=nested:raise ValueError('Conflicting library '+target.name)
      target.write_bytes(nested);extract_nested(nested)
  extract_nested(data)
if __name__=='__main__':main()
