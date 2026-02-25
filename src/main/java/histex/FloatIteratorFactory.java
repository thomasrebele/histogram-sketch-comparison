package histex;

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.function.Supplier;

public class FloatIteratorFactory {

  public static Supplier<FloatIterator> readFile(String path) {

    // TODO tr close resources
    return () -> {

      try {
        InputStream fin = Files.newInputStream(Paths.get(path));
        BufferedInputStream in = new BufferedInputStream(fin);
        ZstdCompressorInputStream zsIn = new ZstdCompressorInputStream(in);
        BufferedInputStream bf = new BufferedInputStream(zsIn);
        Scanner r = new Scanner(bf);

        return new FloatIterator() {
          @Override
          public float nextValue() {
            return r.nextFloat();
          }

          @Override
          public boolean hasNext() {
            return r.hasNext() && r.hasNextFloat();
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
    };
  }

}
