package pl.kacperjy.etl.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.exceptions.FileScanException;
import pl.kacperjy.etl.exceptions.WrongFormatFileException;
import pl.kacperjy.etl.model.DataBatch;
import pl.kacperjy.etl.model.Schema;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class DataFileScanner {

    private static final Logger logger = LoggerFactory.getLogger(DataFileScanner.class);

    private final Collection<Schema> schemaCollection;

    private static final int BATCH_SIZE = 1000;

    private static final int PAGE_SIZE = 1024 * 1024 * 1024; // 1 GB

    public DataFileScanner(Collection<Schema> schemaCollection) {
        this.schemaCollection = schemaCollection;
    }

    public void scanAndFillQueue(Consumer<DataBatch> dataBatchConsumer) {

        // MAIN LOOP
        for (Schema schema : schemaCollection) {
            Path filePath = Path.of(schema.filePath());
            List<MappedByteBuffer> mappedByteBufferList = new ArrayList<>();

            // MAPPING MEMORY
            try (
                    FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)
            ) {
                long fileSize = fileChannel.size();

                for (long position = 0; position < fileSize; position += PAGE_SIZE) {
                    long bytesToMap = Math.min(PAGE_SIZE, (fileSize - position));

                    MappedByteBuffer mappedByteBuffer = fileChannel.map(
                            FileChannel.MapMode.READ_ONLY,
                            position,
                            bytesToMap);

                    mappedByteBufferList.add(mappedByteBuffer);
                }
            } catch (IOException e) {
                throw new FileScanException("Cannot read file. Not enough permissions or files doesn't exists. Schema {%s} will not be loaded to database".formatted(schema.tableName()), e);
            }

            // MAPPED MEMORY - READY TO USE
            MapMemoryReader mapMemoryReader = new MapMemoryReader(mappedByteBufferList);

            // VALIDATE INFO
            final int maxColumnNumber = schema.columnDefList().size();

            boolean endOfFile = false;

            long[] startRowIndexArray = new long[BATCH_SIZE];
            long[] endRowIndexArray = new long[BATCH_SIZE];
            long[] separatorIndexArray = new long[BATCH_SIZE * (maxColumnNumber - 1)];

            int rowsCounter = 0;
            int separatorCounter = 0;

            int separatorCounterBeforeEndLine = 0;

            long globalStartRowPosition = 0;
            long globalPosition = 0;

            byte[] tempBytes = new byte[1024]; // Reading mode - 1 KB

            while (!endOfFile) {
                int nBytes = mapMemoryReader.getNBytes(tempBytes);

                // No more data
                if (nBytes == 0) {
                    endOfFile = true;
                    continue;
                }

                for (int i = 0; i < nBytes; i++) {
                    if (tempBytes[i] == '\n') {
                        if (separatorCounterBeforeEndLine != (maxColumnNumber - 1)) {
                            System.out.println("TRUE");
                            throw new WrongFormatFileException("Schema file defined %d columns per row. Found %d columns".
                                    formatted(maxColumnNumber, separatorCounterBeforeEndLine + 1));
                        }
                        startRowIndexArray[rowsCounter] = globalStartRowPosition; // INSERT START POSITION of ROW
                        endRowIndexArray[rowsCounter] = globalPosition; // INSERT END POSITION of ROW
                        rowsCounter++; // UPDATE rows number in BATCH
                        separatorCounterBeforeEndLine = 0; // RESET counter of columns in row
                        globalStartRowPosition = globalPosition + 1; // POSITION where starts next row - +1 byte after last newLine [previous row end]

                        // FULL BATCH
                        if (rowsCounter == BATCH_SIZE) {
                            int batchBytesSize = (int) (endRowIndexArray[endRowIndexArray.length - 1] - startRowIndexArray[0]);
                            byte[] batchBytesArray = new byte[batchBytesSize];
                            mapMemoryReader.readRandomBytes(batchBytesArray, startRowIndexArray, endRowIndexArray, rowsCounter-1);

                            // PREPARE safe array to avoid race condition and modifying array by many parts of application
                            long [] safeStartRowIndexArray = Arrays.copyOf(startRowIndexArray,startRowIndexArray.length);
                            long [] safeEndRowIndexArray = Arrays.copyOf(endRowIndexArray,endRowIndexArray.length);
                            long [] safeSeparatorIndexArray = Arrays.copyOf(separatorIndexArray,separatorIndexArray.length);

                            DataBatch dataBatch = new DataBatch(
                                    batchBytesArray,
                                    safeStartRowIndexArray,
                                    safeEndRowIndexArray,
                                    safeSeparatorIndexArray,
                                    rowsCounter,maxColumnNumber);
                            dataBatchConsumer.accept(dataBatch); // PUSH full dataBatch to parse and persists in DB - PIPELINE

                            // CLEAR BATCH STATE
                            rowsCounter = 0;
                            separatorCounter = 0;
                        }

                    } else if (tempBytes[i] == ';') {
                        separatorIndexArray[separatorCounter] = globalPosition;
                        separatorCounter++;
                        separatorCounterBeforeEndLine++;
                    }
                    globalPosition++;
                }
            }

            if(rowsCounter != 0) {
                // Append newLine if file doesn't end with new line
                if (globalPosition != endRowIndexArray[rowsCounter-1]) {
                    startRowIndexArray[rowsCounter] = globalStartRowPosition; // ADD start position of last row
                    endRowIndexArray[rowsCounter] = globalPosition; // ADD end position of last row
                    rowsCounter++;
                }

                // PUSH last dataBatch - if there are rest of rows - rowsCounter < BATCH_SIZE
                int currentBatchBytesSize = (int) (endRowIndexArray[rowsCounter-1] - startRowIndexArray[0]);
                byte[] batchBytesArray = new byte[currentBatchBytesSize];
                mapMemoryReader.readRandomBytes(batchBytesArray,startRowIndexArray,endRowIndexArray,rowsCounter-1);

                // PREPARE safe array to avoid race condition and modifying array by many parts of application
                long [] safeStartRowIndexArray = Arrays.copyOf(startRowIndexArray,startRowIndexArray.length);
                long [] safeEndRowIndexArray = Arrays.copyOf(endRowIndexArray,endRowIndexArray.length);
                long [] safeSeparatorIndexArray = Arrays.copyOf(separatorIndexArray,separatorIndexArray.length);

                DataBatch dataBatch = new DataBatch(
                        batchBytesArray,
                        safeStartRowIndexArray,
                        safeEndRowIndexArray,
                        safeSeparatorIndexArray,
                        rowsCounter,maxColumnNumber);
                dataBatchConsumer.accept(dataBatch); // PUSH full dataBatch to parse and persists in DB - PIPELINE
            }

            System.out.println("STATYSTKI TESTOWE");
            System.out.println(Arrays.toString(startRowIndexArray));
            System.out.println(Arrays.toString(endRowIndexArray));
            System.out.println(Arrays.toString(separatorIndexArray));
        }

    }

    private static class MapMemoryReader {

        private int currentBufferIndex = 0;
        private final int maxCurrentBufferIndex;
        private final List<MappedByteBuffer> mappedByteBufferList;

        MapMemoryReader(List<MappedByteBuffer> mappedByteBufferList) {
            this.mappedByteBufferList = mappedByteBufferList;
            this.maxCurrentBufferIndex = mappedByteBufferList.size() - 1; // SAVE from getNext Buffer that doesn't exists
            // EX. if there is only one buffer we would get situation that currentBufferIndex=0 but maxCurrentBufferIndex=1 and
            // buffer with currentBufferIndex=0 has remaining=0 we would try to take nextBuffer because currentBufferIndex != maxCurrentBufferIndex
        }

        void readRandomBytes(byte[] dataBytes, long[] startRowIndexArray, long[] endRowIndexArray, int lastIndex) {
            long startGlobal = startRowIndexArray[0];
            long endGlobal = endRowIndexArray[lastIndex];

            // Start coordinates
            int startBufferIndex = (int) (startGlobal / DataFileScanner.PAGE_SIZE);
            int startPos = (int) (startGlobal % DataFileScanner.PAGE_SIZE);

            // End coordinates
            int endBufferIndex = (int) (endGlobal / DataFileScanner.PAGE_SIZE);
            int endPos = (int) (endGlobal % DataFileScanner.PAGE_SIZE);

            int offsetInDestination = 0; // Array offset

            // Buffers - loop
            for (int i = startBufferIndex; i <= endBufferIndex; i++) {
                MappedByteBuffer current = mappedByteBufferList.get(i);

                // Start  read Position = depends if its firstBuffer or nextBuffer
                int readStart = (i == startBufferIndex) ? startPos : 0;

                // End read Position = depends if its endBuffer or previousBuffer
                int readEnd = (i == endBufferIndex) ? endPos : current.capacity();

                // How many bytes
                int lengthToRead = readEnd - readStart;

                // Reading bytes
                current.get(readStart, dataBytes, offsetInDestination, lengthToRead);

                offsetInDestination += lengthToRead;
            }

            logger.info("Downloaded datapack of {} bytes", offsetInDestination);
        }

        int getNBytes(byte[] dst) {
            int toRead = dst.length;
            int offset = 0;

            MappedByteBuffer current = mappedByteBufferList.get(currentBufferIndex);
            while (toRead > 0) {
                int available = current.remaining();

                if (available == 0) {
                    if (currentBufferIndex == maxCurrentBufferIndex)
                        return dst.length - toRead;
                    current = mappedByteBufferList.get(++currentBufferIndex);
                    continue; // Start again with next buffer
                }

                int chunk = Math.min(toRead, available);
                current.get(dst, offset, chunk);

                offset += chunk;
                toRead -= chunk;
            }

            return dst.length;
        }
    }
}
