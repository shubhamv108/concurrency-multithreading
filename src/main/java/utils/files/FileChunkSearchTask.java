package utils.files;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class FileChunkSearchTask implements Callable<List<String>> {
        private final File file;
        private final long start;
        private final long end;
        private final String searchTerm;

        public FileChunkSearchTask(File file, long start, long end, String searchTerm) {
            this.file = file;
            this.start = start;
            this.end = end;
            this.searchTerm = searchTerm;
        }

        @Override
        public List<String> call() throws Exception {
            Long startTimeInNanoseconds = System.nanoTime();
            List<String> matches = new ArrayList<>();
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(start);

                // Ensure not starting mid-line
                if (start != 0) {
                    raf.readLine();
                }

                String line;
                while (raf.getFilePointer() < end && (line = raf.readLine()) != null) {
                    if (line.contains(searchTerm)) {
                        matches.add(line);
                    }
                }

                // Handle the remainder of the last line in this chunk
                if (raf.getFilePointer() < file.length() && (line = raf.readLine()) != null) {
                    if (line.contains(searchTerm)) {
                        matches.add(line);
                    }
                }

                System.out.println(String.format("startBytes=%s totalTime=%s", start, System.nanoTime() - startTimeInNanoseconds));
            }
            return matches;
        }
    }