package pl.kacperjy.etl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Schema(
        @JsonProperty("tableName") String tableName,
        @JsonProperty("csvFilePath") String filePath,
        @JsonProperty("errorMode") ErrorMode errorMode,
        @JsonProperty("conflictResolution") ConflictResolutionMode conflictResolution,
        @JsonProperty("hasHeader") boolean hasHeader,
        @JsonProperty("conflictTargets") List<String> conflictTargets,
        @JsonProperty("columns") List<ColumnDef> columnDefList
) {
}
