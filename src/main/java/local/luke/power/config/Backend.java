package local.luke.power.config;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonElement;
public interface Backend {
  String id();
  List<Path> files();
  void validate(Map<String,JsonElement> values) throws Exception;
  void apply(Map<String,JsonElement> values) throws Exception;
}
