#!/usr/bin/env python3
"""Physical freecam/F1/preview checks in a muted disposable Xvfb instance."""
import argparse
import json
from pathlib import Path
import subprocess
import time
from input_regressions import Inputs


def run(v, display):
    output = v.game / 'power-beta-data/reports/freecam-preview'
    output.mkdir(parents=True, exist_ok=True)

    def state():
        v.command('freecam-preview state')
        return json.loads((v.game / 'power-beta-161-state.json').read_text())

    def shot(name):
        time.sleep(.3)
        path = output / (name + '.png')
        subprocess.run(['import', '-display', display, '-window', str(v.window), str(path)], check=True)
        return path

    def changed(a, b, box):
        x1, y1, x2, y2 = box
        def pixels(path):
            return subprocess.check_output(['magick', str(path), '-crop',
                f'{x2-x1}x{y2-y1}+{x1}+{y1}', '-depth', '8', 'rgb:-'])
        left, right = pixels(a), pixels(b)
        return sum(any(abs(left[j]-right[j]) > 30 for j in range(i, i+3))
                   for i in range(0, len(left), 3))

    v.command('freecam-preview setup')
    s = state()
    assert s['active'] and s['hideHud'] and s['slots'] == 27, s
    hidden = shot('hidden')
    w, h = map(int, subprocess.check_output(['identify', '-format', '%w %h', str(hidden)], text=True).split())
    panel = (w//4, h//8, 3*w//4, h//2)
    v.key('p', True)
    assert state()['previewHeld']
    preview = shot('hidden-preview')
    assert changed(hidden, preview, panel) > 3000, 'Preview missing with HUD hidden'
    v.tap('F1')
    s = state()
    assert s['active'] and not s['hideHud'] and s['previewHeld'], s
    shown = shot('visible-preview')
    assert changed(preview, shown, (0, 3*h//4, w, h)) > 1000, 'F1 did not render HUD'
    v.key('p', False)
    assert changed(shown, shot('visible'), panel) > 3000, 'Preview did not release'
    v.tap('F1')
    assert state()['hideHud']
    v.tap('c')
    s = state()
    assert not s['active'] and not s['hideHud'], s
    print('PASS preview with HUD hidden/visible, F1 rendering and visibility restoration', flush=True)

    for manual in [False, True]:
        v.command('freecam-preview hud-hidden')
        v.tap('c')
        s = state()
        assert s['active'] and s['hideHud'], s
        if manual:
            v.tap('F1')
            assert not state()['hideHud']
        v.tap('c')
        s = state()
        assert not s['active'] and s['hideHud'], s
    print('PASS initially hidden HUD stays hidden after exit, including manual F1 changes', flush=True)

    v.command('freecam-preview setup')
    v.key('p', True)
    v.command('pause')
    assert state()['screen'] != 'none'
    shot('menu-no-preview')
    v.key('p', False)
    v.tap('Escape')
    assert state()['active'] and state()['hideHud']
    v.command('set power_controls:general.pauseOnLostFocus false')
    v.key('p', True)
    v.focus(False)
    assert not state()['focused']
    shot('unfocused-no-preview')
    v.key('p', False)
    v.focus(True)
    assert state()['active'] and state()['hideHud']
    print('PASS menu/focus transitions preserve freecam HUD state', flush=True)

    v.command('freecam-preview normal')
    v.key('p', True)
    assert state()['slots'] == 27, 'Normal preview target lost'
    normal = shot('normal-preview')
    v.key('p', False)
    assert changed(normal, shot('normal'), panel) > 3000, 'Ordinary preview missing'
    print('PASS ordinary container preview after freecam exit', flush=True)
    v.command('freecam-preview exit')
    s = state()
    assert not s['active'] and not s['hideHud'], s
    print('PASS world exit restores original HUD', flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('game', type=Path)
    parser.add_argument('--display', required=True)
    args = parser.parse_args()
    run(Inputs(args.game, args.display), args.display)
