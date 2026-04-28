package pl.kacperjy.etl.model;

import java.util.List;

public record ColumnDef(
        String name,
        String dataType,
        boolean isPrimaryKey,
        boolean isUnique,
        List<String> additionalAttributes
) {
}
