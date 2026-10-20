package local.luke.power.map;

/** Every radar entity has one category, including unfamiliar mod entities. */
public enum RadarKind {
  PLAYERS, HOSTILE, ANIMALS, ITEMS, VEHICLES, PROJECTILES, OTHER;

  public int color(MapSettings settings) {
    return switch (this) {
      case PLAYERS -> settings.radarPlayersColor;
      case HOSTILE -> settings.radarHostileColor;
      case ANIMALS -> settings.radarAnimalsColor;
      case ITEMS -> settings.radarItemsColor;
      case VEHICLES -> settings.radarVehiclesColor;
      case PROJECTILES -> settings.radarProjectilesColor;
      case OTHER -> settings.radarOtherColor;
    };
  }

  public boolean visible(MapSettings settings) {
    return switch (this) {
      case PLAYERS -> settings.players;
      case HOSTILE -> settings.hostile;
      case ANIMALS -> settings.animals;
      case ITEMS -> settings.items;
      case VEHICLES, PROJECTILES, OTHER -> settings.otherEntities;
    };
  }
}
