package pl.kacperjy.etl.model;

public record ColumnDef(
        String name,
        String dataType,
        boolean isPrimaryKey,
        boolean isUnique
) {
}
