package local.luke.power.audio;

import java.lang.reflect.Field;
import local.luke.power.PowerBeta;

/** Retains the public music API while the pack owns playback and empty-playlist handling. */
final class LegacyMusic {
  private static Field cancel, song, record, dimension;

  static {
    try {
      Class<?> helper = Class.forName("local.luke.power.music_api.MusicState");
      cancel = helper.getField("cancelCurrentBGM");
      song = helper.getField("currentMusicSong");
      record = helper.getField("currentStreamingSong");
      dimension = helper.getField("songLevelId");
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  static boolean consumeStop() {
    try {
      boolean requested = cancel.getBoolean(null);
      if (requested) cancel.setBoolean(null, false);
      return requested;
    } catch (IllegalAccessException e) {
      PowerBeta.LOG.error("Could not read music stop request", e);
      return false;
    }
  }

  static void selected(String name, int level) {
    try {
      song.set(null, name);
      dimension.setInt(null, level);
    } catch (IllegalAccessException e) {
      PowerBeta.LOG.error("Could not publish music metadata", e);
    }
  }

  static void record(String name) {
    try {
      record.set(null, name);
    } catch (IllegalAccessException e) {
      PowerBeta.LOG.error("Could not publish record metadata", e);
    }
  }
}
