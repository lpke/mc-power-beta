#!/usr/bin/env python3
"""Photo-mode and free-look input checks on an isolated, muted Xvfb clone."""
import argparse
import json
from pathlib import Path
import time
from input_regressions import Inputs


def run(v):
    def state():
        v.command('photo-controls state')
        return json.loads((v.game/'power-beta-photo-state.json').read_text())

    def absolute(x,y):
        v.t.XTestFakeMotionEvent(v.d,0,int(x),int(y),0);v.x.XFlush(v.d);time.sleep(.08)

    def button(code,down):
        v.t.XTestFakeButtonEvent(v.d,code,down,0);v.x.XFlush(v.d)

    def click(name,fraction=.5):
        s=state();b=s['buttons'][name]
        absolute((b['x']+b['width']*fraction)*s['displayWidth']/s['width'],(b['y']+10)*s['displayHeight']/s['height'])
        button(1,True);time.sleep(.08);button(1,False);time.sleep(.1)

    def reset():
        v.key('Shift_L',True);v.tap('space');v.key('Shift_L',False)
        absolute(400,300)
        s=state();assert (s['cameraX'],s['cameraY'],s['rotationGoal'],s['zoomGoal'],s['tiltGoal'])==(0,0,0,1.25,30),s

    for helper in ['native','raw']:
        v.command('movement-close');v.command('release-152 setup');v.command('release-152 '+helper)
        v.command('photo-controls open');opening=state()
        assert .01<=opening['zoomGoal']<=12,opening
        v.command('photo-controls layout');assert state()['zoomGoal']==opening['zoomGoal'],state()
        v.tap('space');assert state()['zoomGoal']==opening['zoomGoal'],state()
        v.tap('q');click('tiltSlider',.8);v.command('photo-controls resize')
        assert state()['zoomGoal']==opening['zoomGoal'],state()
        print('PASS photo opening zoom survives center, rotation, tilt and resize',helper,opening['zoomGoal'],flush=True)
        for code in [1,3]:
            for delay in [0,.005,.04,.15]:
                reset();before=state()
                button(code,True);time.sleep(delay);v.move(30,20);time.sleep(delay);button(code,False)
                time.sleep(.2);after=state()
                assert after['dragButton']==-1 and not after['grabbed'],after
                if code==1:
                    # Pan is a viewport fraction; convert it back to physical pixels at this zoom.
                    assert abs(after['cameraX'] * before['displayWidth'] * 2**before['zoomGoal'] - 30)<.02 and after['cameraY']<0,(before,after)
                    assert after['rotationGoal']==0 and after['tiltGoal']==30,after
                else:
                    assert abs(after['rotationGoal']-.06)<.0001 and 33<=after['tiltGoal']<=34,after
                    assert after['cameraX']==after['cameraY']==0,after
                v.move(7,4);time.sleep(.1);idle=state()
                assert all(idle[k]==after[k] for k in ['cameraX','cameraY','rotationGoal','tiltGoal']),idle
                print('PASS photo drag',helper,code,delay,flush=True)
        reset();before=state();button(2,True);v.move(30,20);button(2,False);time.sleep(.2)
        after=state()
        assert after['dragButton']==-1 and all(after[k]==before[k] for k in ['cameraX','cameraY','rotationGoal','tiltGoal']),after
        # Middle click owns its location menu instead of rotating the camera.
        v.command('photo-450 state')
        if json.loads((v.game/'power-beta-data/reports/photo-450-state.json').read_text())['context']:
            v.tap('Escape')
        print('PASS photo middle-click leaves the camera still',helper,flush=True)
        reset();button(1,True);v.move(30,20);button(1,False);time.sleep(.1)
        click('centerButton');assert state()['cameraX']==state()['cameraY']==0,state()
        reset()
        for code,expected in [(4,1.5),(5,1.25)]:
            button(code,True);button(code,False);time.sleep(.1)
            assert state()['zoomGoal']==expected,state()
        for _ in range(8):
            button(5,True);button(5,False)
        time.sleep(.1);assert abs(state()['zoomGoal']-.01)<1e-6,state()
        v.tap('q');click('tiltSlider',.8);v.command('photo-controls resize')
        assert abs(state()['zoomGoal']-.01)<1e-6,state()
        for key,field,sign in [('w','cameraY',-1),('s','cameraY',1),('a','cameraX',1),('d','cameraX',-1)]:
            reset();v.key(key,True);time.sleep(.1);v.key(key,False)
            assert state()[field]*sign>0,state()
        reset();v.tap('q');assert state()['rotationGoal']==.5,state()
        v.tap('e');assert state()['rotationGoal']==0,state()
        click('rotateLeftButton');assert state()['rotationGoal']==.5,state()
        click('rotateRightButton');assert state()['rotationGoal']==0,state()
        before=state()['renderPlayer'];click('renderPlayerButton');assert state()['renderPlayer']!=before
        click('tiltSlider',.8);assert 65<=state()['tiltGoal']<=77,state()
        reset();click('tiltSlider',.5);assert 43<=state()['tiltGoal']<=46,state()
        # A widget-origin drag must never turn into a camera drag.
        s=state();b=s['buttons']['tiltSlider'];absolute((b['x']+70)*s['displayWidth']/s['width'],8*s['displayHeight']/s['height'])
        button(1,True);v.move(-100,220);time.sleep(.1);button(1,False)
        assert state()['cameraX']==0 and state()['rotationGoal']==0,state()
        reset();button(3,True);v.move(20,10);time.sleep(.1)
        v.focus(False);before=state();v.move(20,10);v.key('w',True);time.sleep(.1);after=state()
        assert not after['focused'] and after['dragButton']==-1,after
        assert all(after[k]==before[k] for k in ['cameraX','cameraY','rotationGoal','tiltGoal']),after
        v.key('w',False);v.focus(True);v.move(20,10);button(3,False)
        assert state()['rotationGoal']==before['rotationGoal'],state()
        reset();click('timeSlider',.6);changed=state();assert abs(changed['daylight']-changed['originalDaylight'])>100,changed
        v.command('photo-controls resize');click('exitButton');v.command('photo-controls closed')
        assert not state()['open'] and state()['screen']!='none',state()
        v.command('photo-controls open');shots=set((v.game/'screenshots').glob('*.png'))
        click('screenshotButton');time.sleep(.4)
        assert set((v.game/'screenshots').glob('*.png'))-shots,'photo screenshot missing'
        v.tap('Escape');v.command('photo-controls closed');assert state()['screen'].endswith('.class_525'),state()
        print('PASS photo keyboard, wheel, sliders, buttons, focus, screenshot and exits',helper,flush=True)
    v.command('release-152 setup');v.command('release-152 native')
    v.command('set tweaks.freeLookPerspective 1');v.command('set tweaks.freeLookFollowThirdPerson true')
    for start_third in [False,True]:
        if v.state()['thirdPerson']!=start_third:v.tap('F5')
        v.key('grave',True);v.move(120,30);time.sleep(.2);before=v.state()
        assert before['freeLook'] and abs(before['lookYaw']-before['yaw'])>1,before
        for _ in range(4):
            v.tap('F5');after=v.state()
            assert after['freeLook'] and after['thirdPerson']!=before['thirdPerson'],after
            assert all(after[k]==before[k] for k in ['lookYaw','lookPitch','yaw','pitch']),(before,after)
            before=after
        v.key('grave',False);after=v.state()
        assert not after['freeLook'] and after['thirdPerson']==before['thirdPerson'],after
        print('PASS held free-look perspective transitions',start_third,flush=True)
    for helper in ['native','raw']:
        v.command('release-152 setup');v.command('release-152 '+helper)
        v.command('inventory-look-445 prepare survival')
        v.origin();v.button(True);v.move(40,30);time.sleep(.2);after=v.state()
        assert after['looking'] and after['guiMouseX']<0 and after['guiMouseY']<0,after
        # The hover-cache fields intentionally stay hidden. Check the pose
        # actually submitted to the native player renderer instead.
        v.command('inventory-look-445 check')
        v.move(50,20);time.sleep(.15);v.command('inventory-look-445 check')
        v.button(False);time.sleep(.15);v.command('inventory-look-445 check')
        v.command('inventory-look-445 stop');v.command('movement-close')
        print('PASS inventory model stays at drag origin with hidden hover',helper,flush=True)


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('game',type=Path);p.add_argument('--display',required=True);a=p.parse_args()
    run(Inputs(a.game,a.display))
