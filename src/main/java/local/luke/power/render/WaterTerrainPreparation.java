package local.luke.power.render;

/** Retains valid endpoints while disabled and requests one catch-up only after skipped terrain changes. */
final class WaterTerrainPreparation {
  private Object world;
  private boolean prepared,active,changedWhileDisabled;
  boolean update(Object nextWorld,boolean enabled){
    boolean changedWorld=world!=nextWorld;
    if(changedWorld){world=nextWorld;prepared=active=changedWhileDisabled=false;}
    boolean wasActive=active;active=world!=null&&enabled;
    if(!active)return false;
    boolean refresh=!prepared||!wasActive&&changedWhileDisabled;
    prepared=true;changedWhileDisabled=false;
    // A new world's native render grid already starts dirty.
    return refresh&&!changedWorld;
  }
  void invalidate(Object owner){if(world==owner&&prepared&&!active)changedWhileDisabled=true;}
  boolean active(){return active;}
  void reset(){world=null;prepared=active=changedWhileDisabled=false;}
}
