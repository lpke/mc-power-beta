#!/usr/bin/env python3
"""Import album-only songs from the owner's purchased copies; leave originals untouched."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/powerbeta/music'
SONGS = {
    '1-02': 'Door', '1-04': 'Death', '1-06': 'Moog City', '1-10': 'Équinoxe',
    '1-15': 'Chris', '1-17': 'Excuse', '1-20': 'Dog', '1-22': 'Beginning',
    '1-23': 'Droopy Likes Ricochet', '1-24': 'Droopy Likes Your Face',
    '2-01': 'Ki', '2-05': 'Flake', '2-14': 'Kyoto', '2-25': 'Eleven', '2-30': 'Intro',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folder', type=Path)
    args = parser.parse_args()
    manifest_path = ASSETS / 'manifest.json'
    manifest = json.loads(manifest_path.read_text())
    additions = []
    for number, title in SONGS.items():
        candidates = list(args.folder.glob(number + '. *.mp3'))
        if len(candidates) != 1:
            raise ValueError(f'Expected one purchased MP3 for {number}: {title}')
        source = candidates[0]
        probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams',
            '-show_format', '-of', 'json', str(source)]))
        audio = next(s for s in probe['streams'] if s['codec_type'] == 'audio')
        stem = unicodedata.normalize('NFKD', title).encode('ascii', 'ignore').decode().lower().replace(' ', '_')
        if title == 'Eleven': stem = 'album_eleven'
        filename = stem + '.ogg'
        target = ASSETS / filename
        temporary = target.with_suffix('.import.ogg')
        try:
            subprocess.run(['ffmpeg', '-v', 'error', '-nostdin', '-y', '-i', str(source),
                '-map', '0:a:0', '-vn', '-c:a', 'libvorbis', '-q:a', '8',
                '-map_metadata', '-1', '-metadata', 'title=' + title, str(temporary)], check=True)
            data = temporary.read_bytes()
            temporary.replace(target)
        finally:
            temporary.unlink(missing_ok=True)
        additions.append({'file': filename, 'title': title, 'era': 'Alpha' if number[0] == '1' else 'Beta',
            'usage': 'Album extras', 'source': 'owner-purchased-album', 'sourceFile': source.name,
            'sourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
            'sourceCodec': audio['codec_name'], 'sourceSampleRate': int(audio['sample_rate']),
            'sourceBitRate': int(audio.get('bit_rate', probe['format'].get('bit_rate', 0))),
            'conversion': 'FFmpeg libvorbis quality 8; original channels and sample rate',
            'sha1': hashlib.sha1(data).hexdigest(), 'size': len(data), 'events': []})
        print(title, audio['sample_rate'], audio.get('bit_rate'), len(data), flush=True)
    filenames = {t['file'] for t in additions}
    manifest['tracks'] = [t for t in manifest['tracks'] if t['file'] not in filenames] + additions
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n')


if __name__ == '__main__':
    main()
