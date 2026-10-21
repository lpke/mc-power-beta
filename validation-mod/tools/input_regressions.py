#!/usr/bin/env python3
"""Physical input regressions for an already-running, muted validation clone on Xvfb."""
import argparse
import ctypes as c
import ctypes.util
import json
import os
from pathlib import Path
import subprocess
import time


def library(name):
    path = ctypes.util.find_library(name)
    if not path and Path('/nix/store').exists():
        path = next(Path('/nix/store').glob(f'*-lib{name.lower()}-*/lib/lib{name}.so.*'), None)
    if not path:
        raise RuntimeError(f'Cannot find lib{name}')
    return c.CDLL(str(path))


class Inputs:
    def __init__(self, game, display):
        self.game = game.resolve()
        normal = Path.home() / '.local/share/PrismLauncher/instances'
        assert not self.game.is_relative_to(normal), 'Use a disposable clone outside the normal instance list'
        assert display != os.environ.get('DISPLAY'), 'Use a separate Xvfb display'
        processes = subprocess.check_output(['ps', '-eo', 'comm,args'], text=True).splitlines()
        assert any(len(parts := line.split()) >= 3 and parts[0] == 'Xvfb'
                   and Path(parts[1]).name == 'Xvfb' and parts[2] == display
                   for line in processes), 'Display must belong to Xvfb'
        assert (game / 'power-beta-validation.log').exists(), 'Validation fixture is not installed/running'
        document = json.loads((game / 'config/power-beta.json').read_text())
        assert document['settings']['audio']['master'] == 0, 'Mute the disposable clone first'
        self.x, self.t = library('X11'), library('Xtst')
        self.x.XOpenDisplay.argtypes = [c.c_char_p]; self.x.XOpenDisplay.restype = c.c_void_p
        self.d = self.x.XOpenDisplay(display.encode()); assert self.d
        self.x.XDefaultRootWindow.argtypes = [c.c_void_p]; self.x.XDefaultRootWindow.restype = c.c_ulong
        self.root = self.x.XDefaultRootWindow(self.d)
        self.x.XQueryTree.argtypes = [c.c_void_p, c.c_ulong, c.POINTER(c.c_ulong), c.POINTER(c.c_ulong), c.POINTER(c.POINTER(c.c_ulong)), c.POINTER(c.c_uint)]
        self.x.XFetchName.argtypes = [c.c_void_p, c.c_ulong, c.POINTER(c.c_char_p)]
        self.x.XFree.argtypes = [c.c_void_p]
        root, parent, children, count = c.c_ulong(), c.c_ulong(), c.POINTER(c.c_ulong)(), c.c_uint()
        self.x.XQueryTree(self.d, self.root, c.byref(root), c.byref(parent), c.byref(children), c.byref(count))
        windows = []
        for i in range(count.value):
            name = c.c_char_p(); self.x.XFetchName(self.d, children[i], c.byref(name))
            if name.value and name.value.startswith(b'Prism Launcher: Power Beta'):
                windows.append(children[i])
            if name: self.x.XFree(name)
        self.x.XFree(children)
        assert len(windows) == 1, f'Expected exactly one disposable game window, got {len(windows)}'
        self.window = windows[0]
        self.x.XSetInputFocus.argtypes = [c.c_void_p, c.c_ulong, c.c_int, c.c_ulong]
        self.x.XFlush.argtypes = [c.c_void_p]
        self.x.XStringToKeysym.argtypes = [c.c_char_p]; self.x.XStringToKeysym.restype = c.c_ulong
        self.x.XKeysymToKeycode.argtypes = [c.c_void_p, c.c_ulong]; self.x.XKeysymToKeycode.restype = c.c_uint
        self.t.XTestFakeKeyEvent.argtypes = [c.c_void_p, c.c_uint, c.c_int, c.c_ulong]
        self.t.XTestFakeButtonEvent.argtypes = [c.c_void_p, c.c_uint, c.c_int, c.c_ulong]
        self.t.XTestFakeMotionEvent.argtypes = [c.c_void_p, c.c_int, c.c_int, c.c_int, c.c_ulong]
        self.t.XTestFakeRelativeMotionEvent.argtypes = [c.c_void_p, c.c_int, c.c_int, c.c_ulong]
        self.x.XQueryPointer.argtypes = [c.c_void_p, c.c_ulong, c.POINTER(c.c_ulong), c.POINTER(c.c_ulong), c.POINTER(c.c_int), c.POINTER(c.c_int), c.POINTER(c.c_int), c.POINTER(c.c_int), c.POINTER(c.c_uint)]
        self.focus(True)

    def command(self, command):
        report = self.game / 'power-beta-validation.log'; start = report.stat().st_size
        path = self.game / 'power-beta-validation.command'
        temporary = path.with_suffix('.command.tmp'); temporary.write_text(command + '\n'); temporary.replace(path)
        until = time.monotonic() + 30
        while time.monotonic() < until:
            output = report.read_bytes()[start:].decode(errors='replace')
            assert 'COMMAND FAILED' not in output and 'FAIL ' not in output, output
            if 'COMMAND DONE ' + command in output: return
            time.sleep(.05)
        raise TimeoutError(command)

    def state(self):
        self.command('release-152 state')
        return json.loads((self.game / 'power-beta-152-state.json').read_text())

    def focus(self, active):
        self.x.XSetInputFocus(self.d, self.window if active else self.root, 2, 0)
        self.x.XFlush(self.d); time.sleep(.3)

    def key(self, name, down):
        key = self.x.XKeysymToKeycode(self.d, self.x.XStringToKeysym(name.encode())); assert key
        self.t.XTestFakeKeyEvent(self.d, key, down, 0); self.x.XFlush(self.d); time.sleep(.1)

    def tap(self, name):
        self.key(name, True); self.key(name, False)

    def button(self, down):
        self.t.XTestFakeButtonEvent(self.d, 3, down, 0); self.x.XFlush(self.d)

    def move(self, dx, dy):
        self.t.XTestFakeRelativeMotionEvent(self.d, dx, dy, 0); self.x.XFlush(self.d)

    def click(self):
        self.t.XTestFakeButtonEvent(self.d, 1, True, 0)
        self.t.XTestFakeButtonEvent(self.d, 1, False, 0); self.x.XFlush(self.d)

    def origin(self):
        self.t.XTestFakeMotionEvent(self.d, 0, 90, 400, 0); self.x.XFlush(self.d); time.sleep(.15)
        return self.state(), self.pointer()

    def pointer(self):
        root, child = c.c_ulong(), c.c_ulong()
        rx, ry, wx, wy, mask = c.c_int(), c.c_int(), c.c_int(), c.c_int(), c.c_uint()
        assert self.x.XQueryPointer(self.d, self.window, c.byref(root), c.byref(child), c.byref(rx), c.byref(ry), c.byref(wx), c.byref(wy), c.byref(mask))
        return wx.value, wy.value

    def gesture_checks(self):
        for helper in ['native', 'raw']:
            self.command('movement-close'); self.command('release-152 setup')
            self.command('release-152 ' + helper); self.command('movement-inventory')
            for delay in [0, .001, .005, .01, .02, .04, .08]:
                for _ in range(3):
                    before, pointer = self.origin()
                    for _ in range(10):
                        self.button(True); time.sleep(delay)
                        self.move(24, 12); time.sleep(delay)
                        self.button(False); time.sleep(delay)
                    time.sleep(.15); after = self.state()
                    assert not after['looking'] and not after['grabbed'], after
                    assert (after['mouseX'], after['mouseY']) == (before['mouseX'], before['mouseY']), (before, after)
                    assert self.pointer() == pointer, (pointer, self.pointer())
                    assert abs(after['yaw'] - before['yaw'] - 36) < .02, (before, after)
                    # A cached LWJGL position can hide a failed compositor warp.
                    self.move(1, 0); time.sleep(.1); moved = self.state()
                    assert (moved['mouseX'], moved['mouseY']) == (before['mouseX']+1, before['mouseY']), (before, moved)
                print('PASS burst drags', helper, delay, flush=True)
            for hold in [0, .001, .01, .02, .06, .12]:
                for _ in range(5):
                    before, pointer = self.origin()
                    self.button(True); time.sleep(hold); self.move(24, 12); self.button(False); self.move(7, 3)
                    time.sleep(.15); after = self.state()
                    assert (after['mouseX'], after['mouseY']) == (before['mouseX']+7, before['mouseY']-3), (before, after)
                    assert self.pointer() == (pointer[0]+7, pointer[1]+3)
                    assert abs(after['yaw'] - before['yaw'] - 3.6) < .01, (before, after)
                print('PASS motion immediately after release', helper, hold, flush=True)
            self.command('movement-close')

    def focus_checks(self):
        for pause in [False, True]:
            for cancel in [False, True]:
                for reverse in [False, True]:
                    self.focus(True); self.command('movement-close'); self.command('release-152 setup')
                    self.command('release-152 survival')
                    self.command('set power_controls:general.pauseOnLostFocus ' + str(pause).lower())
                    self.command('set tweaks.autoWalkCancelOnLostFocus ' + str(cancel).lower())
                    if reverse: self.key('s', True)
                    self.tap('r')
                    if reverse: self.key('s', False)
                    self.command('movement-inventory'); self.focus(False)
                    before = self.state(); time.sleep(.5); after = self.state(); sign = -1 if reverse else 1
                    assert not after['focused'] and after['walking'] == (not cancel), after
                    if cancel: assert abs(after['z'] - before['z']) < .05, (before, after)
                    else:
                        assert after['forward'] == sign and after['direction'] == sign, after
                        assert sign*(after['z'] - before['z']) > .5, (before, after)
                    self.focus(True); self.tap('w'); assert not self.state()['walking']
                    print('PASS inventory focus movement', pause, cancel, reverse, flush=True)
        self.command('movement-close'); self.command('release-152 setup'); self.command('movement-inventory')
        self.key('w', True); assert self.state()['forward'] == 1
        self.focus(False); before = self.state(); time.sleep(.5); after = self.state()
        assert after['forward'] == 0 and not after['walking'] and abs(after['z'] - before['z']) < .05, (before, after)
        self.key('w', False); self.focus(True); self.command('movement-close')
        print('PASS manual inventory movement remains focus gated', flush=True)

    def inventory_click_checks(self):
        for helper in ['native', 'raw']:
            for hold in [0, .08]:
                self.command('movement-close'); self.command('release-152 setup')
                self.command('release-152 survival'); self.command('release-152 ' + helper)
                self.command('release-152 click-target')
                target = json.loads((self.game / 'power-beta-click-target.json').read_text())
                _, pointer = self.origin()
                self.button(True); time.sleep(hold); self.move(24, 12); self.button(False)
                # Normal motion and click follow the release in the same event batch.
                self.move(target['x'] - pointer[0], target['y'] - pointer[1])
                self.click()
                time.sleep(.2); self.command('release-152 click-check')
                print('PASS inventory click after drag', helper, hold, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game_dir', type=Path); parser.add_argument('--display', required=True)
    args = parser.parse_args()
    inputs = Inputs(args.game_dir, args.display)
    try:
        inputs.gesture_checks(); inputs.focus_checks(); inputs.inventory_click_checks()
    finally:
        inputs.button(False)
        for key in ['w', 's', 'r']: inputs.key(key, False)
        inputs.focus(True)
        inputs.command('movement-close')


if __name__ == '__main__': main()
