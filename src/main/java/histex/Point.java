package histex;

record Point(float x, float y) implements Comparable<Point> {
  @Override
  public int compareTo(Point o) {
    return Float.compare(this.x, o.x);
  }
}
