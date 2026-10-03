package local.luke.power.config;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
public final class ConfigSession {
  private final List<Setting> settings=new ArrayList<>();
  private final Map<String,Backend> backends=new LinkedHashMap<>();
  public void add(Backend backend,Collection<Setting> entries){
    if(backends.putIfAbsent(backend.id(),backend)!=null)throw new IllegalArgumentException("Duplicate backend "+backend.id());
    for(Setting s:entries){if(settings.stream().anyMatch(v->v.id.equals(s.id)))throw new IllegalArgumentException("Duplicate setting "+s.id);settings.add(s);}
  }
  public List<Setting> settings(){return Collections.unmodifiableList(settings);}
  public long changes(){return settings.stream().filter(Setting::changed).count();}
  public boolean restartRequired(){return settings.stream().anyMatch(s->s.changed()&&s.restart);}
  public void link(Setting changed){
    if(changed.id.equals("tweaks.placement.enabled")){
      boolean tied=settings.stream().filter(s->s.id.equals("tweaks.placement.restrictionTiedToFast")).findFirst().map(s->s.value.getAsBoolean()).orElse(false);
      if(tied)settings.stream().filter(s->s.id.equals("tweaks.placement.restrictionEnabled")).forEach(s->s.value=changed.value.deepCopy());
    }
  }
  public void discard(){for(Setting s:settings)s.value=s.original();}
  public void save(Path gameDir) throws Exception {
    Map<String,Map<String,JsonElement>> changes=new LinkedHashMap<>(),old=new LinkedHashMap<>();
    for(Setting s:settings)if(s.changed()){
      s.validate(s.value);
      changes.computeIfAbsent(s.backend,k->new LinkedHashMap<>()).put(s.id,s.value.deepCopy());
      old.computeIfAbsent(s.backend,k->new LinkedHashMap<>()).put(s.id,s.original());
    }
    if(changes.isEmpty())return;
    for(var e:changes.entrySet())backends.get(e.getKey()).validate(e.getValue());
    List<Path> files=changes.keySet().stream().flatMap(k->backends.get(k).files().stream()).distinct().toList();
    try(FileTransaction transaction=FileTransaction.begin(gameDir,files)){
      List<String> attempted=new ArrayList<>();
      try{
        for(var e:changes.entrySet()){attempted.add(e.getKey());backends.get(e.getKey()).apply(e.getValue());}
        transaction.commit();
      }catch(Exception failure){
        Collections.reverse(attempted);
        for(String id:attempted)try{backends.get(id).apply(old.get(id));}catch(Exception rollback){failure.addSuppressed(rollback);}
        throw failure;
      }
    }
    settings.forEach(Setting::accept);
  }
}
