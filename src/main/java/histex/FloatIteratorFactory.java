package histex;

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class FloatIteratorFactory {

  public static Supplier<FloatIterator> readFile(String path) {

    // TODO tr close resources
    return new FileFloatIteratorSupplier(path);
  }

  private static class FileFloatIteratorSupplier implements Supplier<FloatIterator> {
    private final String path;

    public FileFloatIteratorSupplier(String path) {
      this.path = path;
    }

    @Override
    public FloatIterator get() {
      try {
        InputStream fin = Files.newInputStream(Paths.get(path));
        BufferedInputStream in = new BufferedInputStream(fin, 1<<16);
        ZstdCompressorInputStream zsIn = new ZstdCompressorInputStream(in);
        BufferedInputStream bf = new BufferedInputStream(zsIn, 1<<16);
        BufferedReader r = new BufferedReader(new InputStreamReader(bf));

        return new FloatIterator() {
          private String nextLine = null;

          int i=0;
          @Override
          public float nextValue() {
            i+=1;
            advance();


            float result = Float.parseFloat(nextLine);
            nextLine = null;
            return result;
          }

          @Override
          public boolean hasNext() {
            advance();
            if ("#done".equals(nextLine)) {
              return false;
            }

            return nextLine != null;
          }

          private void advance() {
            if (nextLine == null) {
              try {
                nextLine = r.readLine();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }

          @Override
          public void close() throws IOException {
            r.close();
            zsIn.close();
          }
        };

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String toString() {
      return "data:" + path;
    }
  }
}
