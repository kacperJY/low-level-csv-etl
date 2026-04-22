package pl.kacperjy.etl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Schema(
        @JsonProperty("tableName") String tableName,
        @JsonProperty("csvFilePath") String filePath,
        @JsonProperty("errorMode") String errorMode,
        @JsonProperty("conflictResolution") String conflictResolution,
        @JsonProperty("columns") List<ColumnDef> columnDefList
) {
}
