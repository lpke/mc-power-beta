#!/usr/bin/env python3
"""Exercise real F3/F4 events in a muted disposable Xvfb client."""
import argparse
import json
from pathlib import Path
import time
from input_regressions import Inputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game', type=Path)
    parser.add_argument('--display', required=True)
    args = parser.parse_args()
    io = Inputs(args.game, args.display)

    def setup(kind='default'):
        io.command('release-174 keys ' + kind)
        time.sleep(.2)

    def state():
        io.command('release-174 keys-state')
        return json.loads((args.game / 'power-beta-174-keys.json').read_text())

    def batch(events):
        for name, down in events:
            key = io.x.XKeysymToKeycode(io.d, io.x.XStringToKeysym(name.encode()))
            io.t.XTestFakeKeyEvent(io.d, key, down, 0)
        io.x.XFlush(io.d)
        time.sleep(.3)

    try:
        setup()
        io.key('F3', True)
        s = state(); assert not s['debug'] and s['pending'] and s['shownFrames'] == 0, s
        time.sleep(.8)  # Held/repeated input cannot toggle early.
        assert not state()['debug']
        io.key('F3', False); assert state()['debug']
        io.key('F3', True); assert state()['debug']
        io.key('F3', False); assert not state()['debug']
        print('PASS standalone F3 toggles on release, once per press', flush=True)

        for first in ['F3', 'F4']:
            for shown in [False, True]:
                setup('shown' if shown else 'default')
                io.key('F3', True); io.key('F4', True)
                s = state(); assert 'ModePickerScreen' in s['screen'], s
                io.key(first, False); io.key('F4' if first == 'F3' else 'F3', False)
                s = state()
                assert s['debug'] == shown and not s['pending'] and not s['screen'], s
                assert s['hiddenFrames' if shown else 'shownFrames'] == 0, s
        print('PASS both release orders preserve debug state without a single flickering frame', flush=True)

        setup(); io.key('F3', True); io.tap('F4'); io.tap('F4'); io.tap('F4'); io.key('F3', False)
        s = state(); assert not s['debug'] and s['shownFrames'] == 0, s
        setup(); io.key('F4', True); io.key('F3', True); io.key('F4', False); io.key('F3', False)
        assert not state()['debug']
        setup(); batch([('F3', True), ('F4', True), ('F4', False), ('F3', False)])
        assert not state()['debug']
        setup(); batch([('F3', True), ('F3', False)])
        assert state()['debug']
        print('PASS repeated cycling, reverse presses and complete event batches', flush=True)

        for kind in ['debug-remap', 'modifier-remap', 'cycle-remap', 'chord', 'disabled', 'no-cheats']:
            setup(kind)
            if kind == 'chord': io.key('Control_L', True)
            key = 'F6' if kind == 'debug-remap' else 'F3'
            io.key(key, True); s = state(); assert s['debug'] and not s['pending'], (kind, s)
            io.key(key, False); assert state()['debug']
            if kind == 'chord': io.key('Control_L', False)
        print('PASS remapped/chorded/disabled/unavailable bindings remain immediate', flush=True)

        setup(); io.key('F3', True); io.focus(False); io.key('F3', False); io.focus(True)
        assert not state()['debug']
        setup(); io.key('F3', True); io.tap('Escape'); io.key('F3', False)
        io.command('release-174 keys-close'); assert not state()['debug']
        setup(); io.key('F3', True); io.command('title'); io.command('new-world'); io.key('F3', False)
        assert not state()['debug']
        print('PASS focus, menu and world changes discard pending F3', flush=True)

        setup(); before = state(); io.key('w', True); io.key('F3', True); time.sleep(.5)
        after = state(); assert abs(after['z'] - before['z']) + abs(after['x'] - before['x']) > .1, (before, after)
        assert not after['debug']
        io.key('F3', False); io.key('w', False); assert state()['debug']
        print('PASS movement stays active while F3 waits for release', flush=True)
    finally:
        for key in ['F3', 'F4', 'F6', 'Control_L', 'w']: io.key(key, False)
        io.focus(True)


if __name__ == '__main__':
    main()
