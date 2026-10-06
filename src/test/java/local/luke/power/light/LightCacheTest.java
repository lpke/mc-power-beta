package local.luke.power.light;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LightCacheTest {
  @Test void configurableBudgetPreservesDefaultAndFinishesFasterWithoutExtraWorkWhenOff() {
    assertEquals(2048,new LightSettings().checksPerTick);
    for (int budget : new int[]{256,2048,16384}) {
      LightSettings s=new LightSettings();s.enabled=true;s.checksPerTick=budget;s.validate();
      LightCache cache=new LightCache();Object world=new Object();int ticks=0;
      while(cache.cells().isEmpty() && ticks<100) {
        AtomicInteger reads=new AtomicInteger();
        cache.tick(world,0,64,0,s,(x,y,z)->{reads.incrementAndGet();return y==64?7:-1;});
        assertTrue(reads.get()<=budget);ticks++;
      }
      assertEquals((5733+budget-1)/budget,ticks);
      s.enabled=false;
      cache.tick(world,0,64,0,s,(x,y,z)->{fail("Inactive overlay sampled world");return 0;});
      assertTrue(cache.cells().isEmpty());
    }
    LightSettings s=new LightSettings();s.checksPerTick=0;assertThrows(IllegalArgumentException.class,s::validate);
    s.checksPerTick=Integer.MAX_VALUE;assertThrows(IllegalArgumentException.class,s::validate);
  }
  @Test void inactiveAndMissingWorldNeverReadBlocksAndDropCachedLabels() {
    LightCache cache = new LightCache();
    LightSettings settings = new LightSettings();
    Object world = new Object();
    var count = new AtomicInteger();
    LightCache.Sampler sampler = (x,y,z) -> { count.incrementAndGet(); return 7; };
    for (int i=0;i<100;i++) cache.tick(world,0,64,0,settings,sampler);
    assertEquals(0,count.get());
    settings.enabled=true;
    cache.tick(world,0,64,0,settings,sampler);
    assertFalse(cache.cells().isEmpty());
    count.set(0);settings.enabled=false;
    cache.tick(world,0,64,0,settings,sampler);
    assertTrue(cache.cells().isEmpty());
    settings.enabled=true;
    cache.tick(null,0,64,0,settings,sampler);
    assertEquals(0,count.get());
  }

  @Test void workMemoryAndWorldHeightStayBoundedAtMaximumRange() {
    LightCache cache = new LightCache();
    LightSettings settings = new LightSettings();
    settings.enabled=true;settings.radius=24;settings.verticalRange=16;
    Object world = new Object();
    for(int height:new int[]{-100,0,64,127,200}) {
      for(int tick=0;tick<40;tick++) {
        AtomicInteger reads=new AtomicInteger();
        cache.tick(world,-17,height,-17,settings,(x,y,z)-> {
          assertTrue(y>=0&&y<=126);
          assertTrue((x+17)*(x+17)+(z+17)*(z+17)<=24*24);
          reads.incrementAndGet();return y%2==0?7:-1;
        });
        assertTrue(reads.get()<=LightCache.WORK_PER_TICK);
        assertTrue(cache.cells().size()<=LightCache.MAX_LABELS);
      }
    }
  }

  @Test void refreshesStationaryLightingAndInvalidatesOnTeleportWorldAndModeChange() {
    LightCache cache = new LightCache();
    LightSettings settings = new LightSettings();settings.enabled=true;settings.radius=2;settings.verticalRange=1;
    Object first=new Object(),second=new Object();
    cache.tick(first,0,64,0,settings,(x,y,z)->7);
    assertTrue(cache.cells().stream().allMatch(c->c.level()==7));
    cache.tick(first,0,64,0,settings,(x,y,z)->12);
    assertTrue(cache.cells().stream().allMatch(c->c.level()==12));
    cache.tick(second,1000,64,-1000,settings,(x,y,z)->3);
    assertTrue(cache.cells().stream().allMatch(c->c.level()==3&&c.x()>=998&&c.z()<=-998));
    settings.radius=24;settings.lightSource=1;
    cache.tick(second,1000,64,-1000,settings,(x,y,z)->-1);
    assertTrue(cache.cells().isEmpty());
  }

  @Test void partialScansFinishWhileWalkingAndDoNotPublishInvalidLevels() {
    LightCache cache = new LightCache();
    LightSettings s=new LightSettings();s.enabled=true;
    Object world=new Object();
    for(int i=0;i<20;i++)cache.tick(world,i/4,64,0,s,(x,y,z)-> y==64?15:99);
    assertFalse(cache.cells().isEmpty());
    assertTrue(cache.cells().stream().allMatch(c->c.level()==15&&c.y()==64));
  }
}
