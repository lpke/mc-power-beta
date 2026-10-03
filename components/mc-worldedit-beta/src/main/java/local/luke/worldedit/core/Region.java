package local.luke.worldedit.core;

import java.util.*;

public record Region(Pos min, Pos max) implements Iterable<Pos> {
  public Region {
    if (!min.valid() || !max.valid() || min.x() > max.x() || min.y() > max.y() || min.z() > max.z())
      throw new IllegalArgumentException("Selection is outside Beta's world bounds.");
  }

  public static Region between(Pos a, Pos b) {
    return new Region(
        new Pos(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z())),
        new Pos(Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z())));
  }

  public long volume() {
    return (long) sizeX() * sizeY() * sizeZ();
  }

  public int sizeX() {
    return max.x() - min.x() + 1;
  }

  public int sizeY() {
    return max.y() - min.y() + 1;
  }

  public int sizeZ() {
    return max.z() - min.z() + 1;
  }

  public Region shift(Pos d) {
    return new Region(min.add(d), max.add(d));
  }

  public boolean contains(Pos p) {
    return p.x() >= min.x()
        && p.x() <= max.x()
        && p.y() >= min.y()
        && p.y() <= max.y()
        && p.z() >= min.z()
        && p.z() <= max.z();
  }

  public boolean wall(Pos p) {
    return p.x() == min.x() || p.x() == max.x() || p.z() == min.z() || p.z() == max.z();
  }

  public boolean face(Pos p) {
    return wall(p) || p.y() == min.y() || p.y() == max.y();
  }

  public Iterator<Pos> iterator() {
    return new Iterator<>() {
      long index;

      public boolean hasNext() {
        return index < volume();
      }

      public Pos next() {
        if (!hasNext()) throw new NoSuchElementException();
        long n = index++;
        return new Pos(
            min.x() + (int) (n % sizeX()),
            min.y() + (int) (n / sizeX() % sizeY()),
            min.z() + (int) (n / sizeX() / sizeY()));
      }
    };
  }
}
