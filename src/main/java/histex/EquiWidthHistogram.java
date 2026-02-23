package histex;

public class EquiWidthHistogram extends CountHistogram {

  final float start;
  final float end;
  final int buckets;


  public EquiWidthHistogram(float start, float end, int buckets) {
    super(buckets);
    this.start = start;
    this.end = end;
    this.buckets = buckets;
  }

  @Override
  public int getBucketForValue(float v) {
    int bi = (int) ((v-start)/(end-start) * buckets);
    return bi;
  }

  @Override
  float getBucketStart(int bucketIndex) {
    return start + (end-start)/buckets * bucketIndex;
  }
}
