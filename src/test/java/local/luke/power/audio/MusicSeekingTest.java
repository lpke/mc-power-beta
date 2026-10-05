package local.luke.power.audio;

import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import paulscode.sound.*;
import paulscode.sound.codecs.CodecJOrbis;

class MusicSeekingTest {
  @Test void repeatedInitializationPreservesTheExactRemainingPcm() throws Exception {
    SoundSystemConfig.setLogger(new SoundSystemLogger());
    SoundSystemConfig.setCodec("ogg",CodecJOrbis.class);
    var url=getClass().getResource("/assets/powerbeta/music/calm1.ogg");
    assertNotNull(url);
    assertTrue(MusicSeeking.oggDuration(url)>250);
    double position=123.45;
    MusicSeeking.request("music:calm1.ogg","BgMusic",url,"calm1.ogg",position);
    MusicSeeking.Prepared prepared=null;
    long deadline=System.nanoTime()+20_000_000_000L;
    while(prepared==null&&System.nanoTime()<deadline){prepared=MusicSeeking.take();if(prepared==null)Thread.sleep(10);}
    assertNotNull(prepared);
    assertEquals(position,prepared.seconds(),.001);
    var handoff=MusicSeeking.publish(prepared);
    var seek=new SeekCodec();
    try {
      assertTrue(seek.initialize(handoff));assertTrue(seek.initialize(handoff));
      var actual=MessageDigest.getInstance("SHA-256");long count=0;
      while(!seek.endOfStream()){var b=seek.read();if(b==null)break;actual.update(b.audioData);count+=b.audioData.length;}
      var original=new CodecJOrbis();
      var expected=MessageDigest.getInstance("SHA-256");long expectedCount=0;
      try {
        assertTrue(original.initialize(url));var f=original.getAudioFormat();
        long skip=(long)(position*f.getFrameRate())*f.getFrameSize();
        while(!original.endOfStream()){
          var b=original.read();if(b==null)break;
          int offset=(int)Math.min(skip,b.audioData.length);skip-=offset;
          expected.update(b.audioData,offset,b.audioData.length-offset);expectedCount+=b.audioData.length-offset;
        }
      }finally{original.cleanup();}
      assertEquals(expectedCount,count);assertArrayEquals(expected.digest(),actual.digest());
    }finally{seek.cleanup();MusicSeeking.cancel();}
    assertFalse(new SeekCodec().initialize(handoff),"handoff reused by a second decoder");
  }
}
