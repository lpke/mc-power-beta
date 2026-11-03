#!/usr/bin/env python3
"""GUI release races and physical pointer checks in an already-running muted Xvfb clone."""
import argparse
import ctypes as c
import json
from pathlib import Path
import time
from input_regressions import Inputs


class Cursor472(Inputs):
    def __init__(self, game, display):
        super().__init__(game, display)
        self.x.XSync.argtypes = [c.c_void_p, c.c_int]
        self.status = game / 'power-beta-gui-cursor-472.json'
        self.ack = game / 'power-beta-gui-cursor-472.ack'

    def read(self):
        return json.loads(self.status.read_text())

    def absolute(self, x, y, state):
        self.t.XTestFakeMotionEvent(self.d, 0, state['windowX'] + x, state['windowY'] + y, 0)
        self.x.XSync(self.d, False)

    def acknowledge(self, phase):
        temporary = self.ack.with_suffix('.tmp')
        temporary.write_text(phase); temporary.replace(self.ack)

    def race(self, helper, kind, stale_query=False):
        report = self.game / 'power-beta-validation.log'; start = report.stat().st_size
        command = f'gui-cursor-472 race {helper} {kind}'
        if stale_query: command += ' stale-query'
        path = self.game / 'power-beta-validation.command'
        temporary = path.with_suffix('.command.tmp'); temporary.write_text(command); temporary.replace(path)
        queued = moved = False
        deadline = time.monotonic() + 15
        while time.monotonic() < deadline:
            output = report.read_bytes()[start:].decode(errors='replace')
            assert 'COMMAND FAILED' not in output, output
            state = self.read() if self.status.exists() else {}
            if state.get('phase') == 'queue' and not queued:
                # These reach the X server while the native client is held just
                # before setScreen, leaving genuine gameplay MotionNotify queued.
                for offset in [-30, -70, -120, -180, -230]:
                    self.absolute(state['width'] // 2 + offset, state['height'] // 2 + 11, state)
                self.acknowledge('queue'); queued = True
            elif state.get('phase') == 'move' and queued and not moved:
                self.move(19, 7); self.x.XSync(self.d, False)
                self.acknowledge('move'); moved = True
            if queued and moved and 'COMMAND DONE ' + command in output:
                result = self.read(); assert result['phase'] == 'done', result
                # XQueryPointer is independent of LWJGL and the Java observer.
                expected = (result['width'] // 2 + 19, result['height'] - 1 - result['height'] // 2 + 7)
                if self.pointer() != expected:
                    result['violations'].append(f'external X pointer {self.pointer()} differs from {expected}')
                    result['passed'] = False
                return result
            time.sleep(.001)
        raise TimeoutError(command)

    def navigation_and_drag(self, helper):
        self.command('gui-cursor-472 helper ' + helper)
        self.command('gui-cursor-472 inventory')
        self.command('gui-cursor-472 state'); initial = self.read()
        origin = (30, initial['height'] // 2)
        self.absolute(*origin, initial); time.sleep(.1)
        self.command('gui-cursor-472 state'); before = self.read()
        self.button(True); time.sleep(.08); self.move(24, 12); time.sleep(.08)
        self.command('gui-cursor-472 state'); held = self.read()
        assert held['looking'] and held['grabbed'], held
        self.button(False); self.move(19, 7); time.sleep(.15)
        self.command('gui-cursor-472 state'); after = self.read()
        expected = (origin[0] + 19, origin[1] + 7)
        assert not after['looking'] and not after['grabbed'], after
        assert self.pointer() == expected, (self.pointer(), expected, after)
        assert (after['mouseX'], after['height'] - 1 - after['mouseY']) == expected, after
        assert abs(after['yaw'] - before['yaw']) > .01 or abs(after['pitch'] - before['pitch']) > .01
        self.command('gui-cursor-472 menu')
        assert self.pointer() == expected, (self.pointer(), expected)
        print('PASS inventory drag origin, immediate release motion and free menu navigation', helper, flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game_dir', type=Path); parser.add_argument('--display', required=True)
    parser.add_argument('--trials', type=int, default=3)
    parser.add_argument('--observe', action='store_true', help='record baseline failures without stopping')
    parser.add_argument('--skip-drags', action='store_true')
    parser.add_argument('--stale-query', action='store_true',
                        help='inject one pre-release native query position; does not reproduce Xwayland warp delay')
    args = parser.parse_args()
    inputs = Cursor472(args.game_dir, args.display); results = []
    try:
        for helper in ['native', 'raw']:
            for kind in ['chest', 'inventory', 'pause', 'chat']:
                for trial in range(args.trials):
                    result = inputs.race(helper, kind, args.stale_query); results.append(result)
                    print('PASS' if result['passed'] else 'OBSERVED FAILURE', helper, kind, trial,
                          result['violations'], flush=True)
                    if not args.observe: assert result['passed'], result
            if not args.skip_drags: inputs.navigation_and_drag(helper)
    finally:
        inputs.button(False)
        inputs.command('gui-cursor-472 finish')
        filename = 'gui-cursor-472-stale-query.json' if args.stale_query else 'gui-cursor-472.json'
        destination = args.game_dir / 'power-beta-data/reports/release-472' / filename
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(json.dumps(results, indent=2) + '\n')
    print('Recorded', len(results), 'native GUI releases;', sum(not r['passed'] for r in results), 'failed', flush=True)


if __name__ == '__main__': main()
