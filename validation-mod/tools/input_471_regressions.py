#!/usr/bin/env python3
"""Ctrl+S and keyboard-polling regressions in an already-running muted Xvfb clone."""
import argparse
import ctypes as c
import json
from pathlib import Path
import time
from input_regressions import Inputs


class Input471(Inputs):
    def previous_state(self):
        self.command('previous-command-471 state')
        return json.loads((self.game / 'power-beta-previous-command-471.json').read_text())

    def check_previous_command(self):
        self.command('previous-command-471 setup')
        try:
            self.tap('F9')
            state = self.previous_state()
            assert state['emptyMessages'] == 1 and state['screen'] == 'none', state
            self.command('previous-command-471 seed')
            self.key('F9', True); time.sleep(1.2)
            state = self.previous_state()
            assert state['seedMessages'] == 2 and state['screen'] == 'none', state
            assert state['notesUnchanged'] and state['historyUnchanged'], state
            self.key('F9', False); self.tap('F9')
            assert self.previous_state()['seedMessages'] == 3
            code = self.x.XKeysymToKeycode(self.d, self.x.XStringToKeysym(b'F9'))
            # LWJGL merges a same-key release/press with equal X timestamps into
            # an auto-repeat event. Keep this one flushed batch physically distinct.
            for index, down in enumerate([True, False, True, False]):
                self.t.XTestFakeKeyEvent(self.d, code, down, 0 if index == 0 else 2)
            self.x.XFlush(self.d); time.sleep(.15)
            state = self.previous_state()
            assert state['seedMessages'] == 5 and state['screen'] == 'none', state
            self.command('previous-command-471 chat'); self.tap('F9')
            state = self.previous_state()
            assert state['seedMessages'] == 6 and state['notesUnchanged'] and state['historyUnchanged'], state
            self.command('previous-command-471 menu'); self.tap('F9')
            assert self.previous_state()['seedMessages'] == 6
            self.command('previous-command-471 close'); self.command('previous-command-471 clear'); self.tap('F9')
            state = self.previous_state()
            assert state['seedMessages'] == 6 and state['emptyMessages'] == 2 and state['screen'] == 'none', state
            self.command('previous-command-471 ordinary-chat'); self.tap('F9')
            state = self.previous_state()
            assert state['screen'] != 'none', state
            print('PASS previous command dispatch, press ownership, history, repeat suppression, menus and reset', flush=True)
        finally:
            self.key('F9', False); self.command('previous-command-471 finish')

    def state471(self):
        self.command('input-471 state')
        return json.loads((self.game / 'power-beta-input-471.json').read_text())

    def check_ctrl_s(self):
        for debug in [False, True]:
            self.command('input-471 debug ' + str(debug).lower())
            self.command('input-471 bind 61 0')
            for control, code in [('Control_L', 29), ('Control_R', 157)]:
                for modifier_first in [False, True]:
                    self.command('input-471 reset')
                    self.key(control, True); self.key('s', True)
                    state = self.state471()
                    assert state['physical'][str(code)] and state['physical']['31'], state
                    assert state['modifiers'] == 1 and not state['hudDown'], state
                    assert state['hudEventCode'] == -1000 and not state['overlay'], state
                    for key in ([control, 's'] if modifier_first else ['s', control]): self.key(key, False)
                    state = self.state471()
                    assert state['modifiers'] == 0 and not state['overlay'], state
                    print('PASS Ctrl+S', debug, control, modifier_first, flush=True)
            self.key('w', True); self.key('Control_L', True); self.tap('s'); self.key('Control_L', False)
            assert self.state471()['physical']['17']
            self.key('Control_L', True); self.tap('s'); self.key('Control_L', False); self.key('w', False)
            self.command('input-471 menu')
            self.key('Control_L', True); self.tap('s'); self.key('Control_L', False)
            assert self.state471()['screen'] != 'none'
            self.command('input-471 close')
            self.command('input-471 reset')
            self.tap('F3')
            state = self.state471()
            assert state['overlay'] and not state['graph'], state
            self.tap('F3')
            assert not self.state471()['overlay']
            self.key('Control_L', True); self.tap('F3'); self.key('Control_L', False)
            state = self.state471()
            assert state['overlay'] and state['graph'], state
            print('PASS held W, open menu, plain F3 and Ctrl+F3', debug, flush=True)
        self.command('input-471 reset')
        self.key('Control_L', True); self.key('s', True)
        self.focus(False); self.key('Control_L', False); self.key('s', False); self.focus(True)
        self.command('input-471 close')
        assert self.state471()['modifiers'] == 0
        print('PASS focus loss during Ctrl+S', flush=True)

    def check_remapped_keys(self):
        self.command('input-471 debug false')
        # Xvfb's default F13 symbols can share a lower function key's shifted slot.
        # Give spare physical codes explicit symbols on this verified private display.
        self.x.XGetKeyboardMapping.argtypes = [c.c_void_p, c.c_ubyte, c.c_int, c.POINTER(c.c_int)]
        self.x.XGetKeyboardMapping.restype = c.POINTER(c.c_ulong)
        self.x.XChangeKeyboardMapping.argtypes = [c.c_void_p, c.c_int, c.c_int, c.POINTER(c.c_ulong), c.c_int]
        width = c.c_int()
        old = self.x.XGetKeyboardMapping(self.d, 200, 12, c.byref(width))
        assert old and width.value > 0
        saved = (c.c_ulong * (12 * width.value))(*[old[i] for i in range(12 * width.value)])
        self.x.XFree(old)
        mapped = (c.c_ulong * (12 * width.value))(*[
            self.x.XStringToKeysym(('F'+str(13+i)).encode())
            for i in range(12) for _ in range(width.value)])

        def scan(code, down):
            self.t.XTestFakeKeyEvent(self.d, code, down, 0); self.x.XFlush(self.d); time.sleep(.1)

        self.x.XChangeKeyboardMapping(self.d, 200, width.value, mapped, 12); self.x.XFlush(self.d)
        try:
            for number in range(13, 25):
                code = 100 + number - 13 if number <= 18 else 113 + number - 19
                name = 'F' + str(number)
                self.command(f'input-471 bind {code} 0')
                scan(200 + number - 13, True)
                state = self.state471()
                assert state['physical'][str(code)] and state['hudDown'] and state['overlay'], state
                assert state['events'].get(str(code), 0) == 1, state
                scan(200 + number - 13, False)
                assert not self.state471()['hudDown']
                # S must also use the held-state path safely while this binding is inactive.
                self.key('Control_L', True); self.tap('s'); self.key('Control_L', False)
                print('PASS native event and held polling', name, code, flush=True)
        finally:
            for code in range(200, 212): scan(code, False)
            self.x.XChangeKeyboardMapping(self.d, 200, width.value, saved, 12); self.x.XFlush(self.d)
        for shift in [False, True]:
            self.command('input-471 bind 43 ' + ('2' if shift else '0'))
            if shift: self.key('Shift_L', True)
            self.key('backslash', True)
            state = self.state471()
            assert state['physical']['43'] and state['hudDown'] and state['overlay'], state
            assert state['events'].get('43', 0) == 1, state
            self.key('backslash', False)
            if shift: self.key('Shift_L', False)
            print('PASS backslash/pipe event and held polling', shift, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game_dir', type=Path); parser.add_argument('--display', required=True)
    args = parser.parse_args()
    inputs = Input471(args.game_dir, args.display)
    inputs.command('input-471 setup')
    try:
        inputs.check_ctrl_s(); inputs.check_remapped_keys()
    finally:
        for key in ['w', 's', 'Control_L', 'Control_R', 'Shift_L', 'backslash'] + ['F'+str(n) for n in range(3, 25)]:
            # Some Xvfb keymaps omit keys that were never used in this check.
            if inputs.x.XKeysymToKeycode(inputs.d, inputs.x.XStringToKeysym(key.encode())): inputs.key(key, False)
        inputs.focus(True)
        inputs.command('input-471 close'); inputs.command('input-471 finish')
    inputs.check_previous_command()


if __name__ == '__main__': main()
