package pl.kacperjy.etl.io;

import pl.kacperjy.etl.model.DataBatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class DataBatchParser {

    private DataBatchParser(){}

    public static List<String[]> parseDataBatch(DataBatch dataBatch) {
        byte[] rawData = dataBatch.rawData();
        int rowNumber = dataBatch.rowsInBatch();
        int columnsInRow = dataBatch.columnsInRow();

        long[] startRows = dataBatch.startRowIndexArray();
        long[] endRows = dataBatch.endRowIndexArray();
        long[] separators = dataBatch.separatorIndexArray();

        // RESULT
        List<String[]> resultData = new ArrayList<>(rowNumber);

        // Reference variable to calculate localPositions
        long batchGlobalStart = startRows[0];

        // Separators counter
        int currentSeparatorIndex = 0;

        for (int r = 0; r < rowNumber; r++) {
            String[] rowFields = new String[columnsInRow];

            // Row start pos
            long currentWordStartGlobal = startRows[r];

            for (int c = 0; c < columnsInRow; c++) {
                // Column end
                long currentWordEndGlobal;

                // CHECK IF last separator in row
                if (c == columnsInRow - 1) {
                    currentWordEndGlobal = endRows[r]; // Last column [last_separator ; row_end_pos]
                } else {
                    currentWordEndGlobal = separators[currentSeparatorIndex++]; // Next separator
                }

                int localOffset = (int) (currentWordStartGlobal - batchGlobalStart);
                int wordLength = (int) (currentWordEndGlobal - currentWordStartGlobal);

                rowFields[c] = new String(rawData, localOffset, wordLength, StandardCharsets.UTF_8);

                currentWordStartGlobal = currentWordEndGlobal + 1;
            }
            resultData.add(rowFields);
        }

        return resultData;
    }
}
