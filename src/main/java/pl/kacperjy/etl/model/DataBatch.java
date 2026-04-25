package pl.kacperjy.etl.model;

public record DataBatch(
        byte[] rawData,
        long[] startRowIndexArray,
        long[] endRowIndexArray,
        long[] separatorIndexArray,
        int rowsInBatch,
        int columnsInRow
) {}
