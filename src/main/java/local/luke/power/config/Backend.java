package local.luke.power.config;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface Backend {
  String id();

  List<Path> files();

  void validate(Map<String, JsonElement> values) throws Exception;

  void apply(Map<String, JsonElement> values) throws Exception;
}
