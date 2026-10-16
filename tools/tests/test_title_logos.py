import hashlib
import json
from pathlib import Path
import struct
import unittest
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'modules/title/src/main/resources/assets/power_title/logos'


class TitleLogosTest(unittest.TestCase):
    def test_bundled_logos_match_the_pinned_official_sources(self):
        sources = json.loads((ASSETS / 'sources.json').read_text())
        self.assertEqual({'beta.png', 'java_2017.png', 'java_2020.png', 'modern.png',
                          'edition.png', 'edition_modern.png'}, {row['file'] for row in sources})
        for row in sources:
            with self.subTest(file=row['file']):
                image = (ASSETS / row['file']).read_bytes()
                self.assertEqual(row['sha256'], hashlib.sha256(image).hexdigest())
                self.assertEqual(b'\x89PNG\r\n\x1a\n', image[:8])
                self.assertIn(struct.unpack('>II', image[16:24]),
                              [(256, 256), (1024, 256), (128, 16), (512, 64)])
                self.assertIn(urlparse(row['client']['url']).netloc,
                              ['piston-data.mojang.com', 'launcher.mojang.com'])
                self.assertEqual(40, len(row['client']['sha1']))

    def test_fresh_installs_keep_the_custom_retro_logo(self):
        defaults = json.loads((ROOT / 'src/main/resources/assets/powerbeta/defaults/power-beta.json').read_text())
        self.assertEqual(0, defaults['settings']['title']['style'])
        self.assertGreater(len(defaults['settings']['title']['logo']['lines']), 0)
