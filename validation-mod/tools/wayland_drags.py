#!/usr/bin/env python3
"""Run the drag/click regressions through a private KWin compositor's fake-input protocol."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
from input_regressions import Inputs


class WaylandInputs(Inputs):
    def __init__(self, game, runtime, socket, helper):
        self.game = game.resolve()
        assert not self.game.is_relative_to(Path.home() / '.local/share/PrismLauncher/instances')
        runtime = runtime.resolve()
        assert runtime.is_relative_to(Path('/tmp')), 'Use a private compositor in a temporary runtime directory'
        assert Path(socket).name == socket and (runtime / socket).is_socket()
        assert (game / 'power-beta-validation.log').exists()
        assert json.loads((game / 'config/power-beta.json').read_text())['settings']['audio']['master'] == 0
        env = os.environ.copy()
        env.update(XDG_RUNTIME_DIR=str(runtime), WAYLAND_DISPLAY=socket)
        self.process = subprocess.Popen([str(helper.resolve())], env=env, stdin=subprocess.PIPE, text=True)
        self.send('abs 90 400'); self.click(); time.sleep(.2)

    def send(self, command):
        self.process.stdin.write(command + '\n'); self.process.stdin.flush()

    def button(self, down):
        self.send(f'button 273 {int(down)}')

    def click(self):
        self.send('button 272 1\nbutton 272 0')

    def move(self, dx, dy):
        self.send(f'rel {dx} {dy}')

    def origin(self):
        # Opening a GUI releases a Wayland pointer lock asynchronously. Wait until
        # its position hint has settled before beginning the physical gesture.
        for _ in range(10):
            self.send('abs 91 400\nabs 90 400'); time.sleep(.15)
            before = self.state()
            if before['mouseX'] == 90 and before['focused'] and not before['grabbed']:
                return before, self.pointer()
        raise AssertionError('Compositor did not place the pointer at the test origin')

    def pointer(self):
        state = self.state()
        return state['mouseX'], state['displayHeight'] - 1 - state['mouseY']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('game_dir', type=Path)
    parser.add_argument('--runtime-dir', required=True, type=Path)
    parser.add_argument('--socket', required=True)
    parser.add_argument('--input-helper', required=True, type=Path)
    args = parser.parse_args()
    inputs = WaylandInputs(args.game_dir, args.runtime_dir, args.socket, args.input_helper)
    try:
        inputs.gesture_checks(); inputs.inventory_click_checks()
    finally:
        inputs.send('button 273 0\nbutton 272 0')
        inputs.process.stdin.close(); inputs.process.wait(timeout=5)
        inputs.command('movement-close')


if __name__ == '__main__': main()
