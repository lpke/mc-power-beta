#!/usr/bin/env python3
"""Physical hotbar/offhand regressions for a muted disposable client on Xvfb."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game", type=Path, required=True)
    parser.add_argument("--test-root", type=Path, required=True)
    parser.add_argument("--pid", type=int, required=True)
    parser.add_argument("--window", required=True)
    parser.add_argument("--display", required=True)
    parser.add_argument("--suite", choices=["core", "rows", "offhand", "all"], default="core")
    parser.add_argument("--repeat-seconds", type=float, default=1.4)
    args = parser.parse_args()
    game = args.game.resolve(strict=True)
    work = args.test_root.resolve(strict=True)
    normal = Path.home() / ".local/share/PrismLauncher/instances"
    if game.is_relative_to(normal) or args.display == os.environ.get("DISPLAY"):
        raise SystemExit("Use an isolated test root and display outside normal instances")
    processes = subprocess.check_output(["ps", "-eo", "comm,args"], text=True).splitlines()
    if not any(len(parts := line.split()) >= 3 and parts[0] == "Xvfb"
               and Path(parts[1]).name == "Xvfb" and parts[2] == args.display for line in processes):
        raise SystemExit("The test display must belong to Xvfb")
    config = json.loads((game / "config/power-beta.json").read_text())
    if config["settings"]["audio"]["master"] != 0:
        raise SystemExit("Mute the disposable clone before running physical tests")
    if not game.is_relative_to(work) or game == work:
        raise SystemExit("Refusing a game directory outside the supplied disposable test root")
    if args.repeat_seconds < 0.8 or args.repeat_seconds > 5:
        raise SystemExit("repeat-seconds must be between0.8 and5")
    process = Path("/proc") / str(args.pid)
    command_line = (process / "cmdline").read_bytes().replace(b"\0", b" ").decode(errors="replace")
    if (process / "cwd").resolve() != game and str(game) not in command_line:
        raise SystemExit("The supplied process does not belong to this game directory")
    env = dict(os.environ, DISPLAY=args.display)

    def xdo(*arguments):
        return subprocess.run(["xdotool", *map(str, arguments)], env=env, check=True, text=True, capture_output=True).stdout.strip()

    if (process / "cwd").resolve() != game or (process / "comm").read_text().strip() != "java":
        raise SystemExit("The game must be the exact owned Java process")
    if ("DISPLAY=" + args.display).encode() not in (process / "environ").read_bytes().split(b"\0"):
        raise SystemExit("The owned game must use the isolated test display")
    try:
        window_pid = int(xdo("getwindowpid", args.window))
    except subprocess.CalledProcessError:
        # LWJGL2 does not publish _NET_WM_PID. The unique owned title plus exact
        # process/display checks and the fixture's native focus gate replace it.
        title = "Prism Launcher: Power Beta 4.0.0 disposable"
        if xdo("getwindowname", args.window) != title or xdo("search", "--name", "^" + title + "$").splitlines() != [str(args.window)]:
            raise SystemExit("The LWJGL window is not the unique owned disposable title")
    else:
        if window_pid != args.pid:
            raise SystemExit("The supplied window does not belong to the supplied game process")
    xdo("windowfocus", "--sync", args.window)
    command = game / "power-beta-validation.command"
    log = game / "power-beta-validation.log"
    state_file = game / "power-beta-input-400.json"

    def request(text, marker, timeout=25):
        if command.exists():
            raise RuntimeError("Another validation command is pending")
        offset = log.stat().st_size if log.exists() else 0
        command.write_text("input-400 " + text + "\n")
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            with log.open(errors="replace") as stream:
                stream.seek(offset)
                output = stream.read()
            if "COMMAND FAILED" in output:
                raise RuntimeError(output.strip())
            if marker in output:
                return json.loads(state_file.read_text()) if state_file.exists() else {}
            time.sleep(0.1)
        raise TimeoutError("No fixture completion for " + text)

    def release():
        for key in ("c", "x", "v", "1", "4", "Control_L"):
            xdo("keyup", key)
        xdo("mouseup", 1)
        time.sleep(0.2)

    def at_pointer(state):
        xdo("mousemove", "--window", args.window, state["windowX"], state["windowY"])
        time.sleep(0.2)

    results = []

    def record(case, order):
        state = request("state", "INPUT400 STATE ")
        results.append({"case": case, "releaseOrder": order, "report": state.get("report")})
        print("PASS", case, order, state.get("report"), flush=True)

    def saved(case, first):
        release()
        state = request("prepare " + case, "INPUT400 READY ")
        try:
            activator, number = state["activator"], state["number"]
            xdo("keydown", activator)
            time.sleep(0.12)
            xdo("keydown", number)
            time.sleep(args.repeat_seconds)
            request("check-repeat", "INPUT400 CHECK-REPEAT ")
            xdo("keyup", number if first == "number" else activator)
            time.sleep(0.3)
            request("check-partial", "INPUT400 CHECK-PARTIAL ")
            xdo("keyup", activator if first == "number" else number)
            time.sleep(0.3)
            request("check", "INPUT400 CHECK ")
            record(case, first + " first")
        finally:
            release()
            request("stop", "INPUT400 STOP ")

    def simple(case):
        release()
        state = request("prepare " + case, "INPUT400 READY ")
        try:
            if case in ("row-save", "row-load", "scroll-save"):
                xdo("keydown", state["activator"])
                time.sleep(0.15)
                xdo("keydown", state["number"])
                time.sleep(0.3)
                request("check-held", "INPUT400 CHECK-HELD ")
                xdo("keyup", state["number"])
                time.sleep(0.15)
                xdo("keyup", state["activator"])
            elif case.startswith("mouse-"):
                at_pointer(state)
                xdo("keydown", "Control_L")
                time.sleep(0.1)
                xdo("click", 1)
                xdo("keyup", "Control_L")
            elif case == "game-plain":
                xdo("key", state["number"])
            else:
                if case == "offhand-inventory":
                    at_pointer(state)
                xdo("keydown", "v")
                time.sleep(args.repeat_seconds)
                request("check-held", "INPUT400 CHECK-HELD ")
                xdo("keyup", "v")
            time.sleep(0.35)
            request("check", "INPUT400 CHECK ")
            record(case, "number first" if case.startswith("row-") or case == "scroll-save" else "single press")
        finally:
            release()
            request("stop", "INPUT400 STOP ")

    try:
        if args.suite in ("core", "all"):
            simple("game-plain")
            for case in ("game-save", "game-load", "inventory-save", "inventory-load"):
                for order in ("number", "activator"):
                    saved(case, order)
        if args.suite in ("rows", "all"):
            for case in ("row-save", "row-load", "scroll-save"):
                simple(case)
        if args.suite in ("offhand", "all"):
            for case in ("offhand-game", "offhand-inventory", "offhand-text", "mouse-slot", "mouse-header", "mouse-outside"):
                simple(case)
    finally:
        release()
        # Stop is idempotent after a fixture assertion has already restored its state.
        request("stop", "INPUT400 STOP ")
        destination = game / "power-beta-data/reports/input-400/physical-driver-result.json"
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(json.dumps({"completed": results}, indent=2) + "\n")


if __name__ == "__main__":
    main()
