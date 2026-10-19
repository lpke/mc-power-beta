#!/usr/bin/env python3
"""Exercise matching-item transfers with real mouse events in a muted Xvfb clone."""
import argparse
import json
from pathlib import Path
import time

from input_regressions import Inputs


class Transfers(Inputs):
    def state(self):
        self.command('quick-transfer state')
        return json.loads((self.game / 'power-beta-transfer-state.json').read_text())

    def setup(self, direction, mode):
        self.command(f'quick-transfer setup {direction} {mode}')
        state = self.state()
        self.t.XTestFakeMotionEvent(self.d, 0, state['x'], state['y'], 0)
        self.x.XFlush(self.d)
        # Let the fixture's newly built terrain settle before measuring a click window.
        time.sleep(.4)

    def left(self, down):
        self.t.XTestFakeButtonEvent(self.d, 1, down, 0)
        self.x.XFlush(self.d)

    def shift(self, down):
        code = self.x.XKeysymToKeycode(self.d, self.x.XStringToKeysym(b'Shift_L'))
        self.t.XTestFakeKeyEvent(self.d, code, down, 0)
        self.x.XFlush(self.d)

    def gesture(self, shift_first=False):
        self.shift(True)
        time.sleep(.05)
        self.left(True); time.sleep(.05); self.left(False); time.sleep(.05)
        self.left(True); time.sleep(.12)
        if shift_first:
            self.shift(False); self.left(False)
        else:
            self.left(False); self.shift(False)
        time.sleep(.1)

    def verify(self, swept):
        state = self.state()
        assert state['source'][1:] == [0 if swept else 13, 11, 9], state
        assert state['screenOpen'] and state['cursorUnchanged'] and state['total'] == 20, state
        return state

    def checks(self):
        for helper in ['native', 'raw']:
            self.command('release-152 ' + helper)
            for direction in ['player', 'container']:
                for mode in ['SURVIVAL', 'CREATIVE']:
                    for shift_first in [False, True]:
                        for _ in range(3):
                            self.setup(direction, mode)
                            self.gesture(shift_first)
                            self.verify(True)
                        print('PASS matching transfer', helper, direction, mode, 'Shift released first' if shift_first else 'mouse released first', flush=True)
            for cancellation in ['timeout', 'focus', 'cursor', 'shift']:
                self.setup('player', 'SURVIVAL')
                self.shift(True); self.left(True); time.sleep(.03); self.left(False)
                if cancellation == 'timeout': time.sleep(.3)
                elif cancellation == 'focus':
                    self.focus(False); self.shift(False); self.focus(True); self.shift(True); time.sleep(.05)
                elif cancellation == 'cursor': self.command('quick-transfer replace-cursor')
                else: self.shift(False); time.sleep(.05); self.shift(True)
                self.left(True); time.sleep(.03); self.left(False); self.shift(False)
                time.sleep(.1); self.verify(False)
                print('PASS canceled matching transfer', helper, cancellation, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game_dir', type=Path)
    parser.add_argument('--display', required=True)
    args = parser.parse_args()
    inputs = Transfers(args.game_dir, args.display)
    try:
        inputs.command('new-world')
        inputs.checks()
    finally:
        inputs.left(False); inputs.shift(False); inputs.focus(True)
        inputs.command('quick-transfer close')


if __name__ == '__main__': main()
