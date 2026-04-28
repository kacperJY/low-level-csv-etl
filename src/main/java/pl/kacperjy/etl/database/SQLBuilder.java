package pl.kacperjy.etl.database;

import pl.kacperjy.etl.model.ColumnDef;
import pl.kacperjy.etl.model.ConflictResolutionMode;
import pl.kacperjy.etl.model.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SQLBuilder {

    public static String sqlCreate(Schema schema) {

        List<String> columns = new ArrayList<>();
        List<String> primaryKeyColumns = new ArrayList<>();

        for (ColumnDef columnDef : schema.columnDefList()) {
            String textColumn = "\t" + columnDef.name() + " " + columnDef.dataType();

            if (columnDef.isUnique())
                textColumn += " UNIQUE";

            columns.add(textColumn);

            if (columnDef.isPrimaryKey())
                primaryKeyColumns.add(columnDef.name());
        }

        String start = "CREATE TABLE %s (\n".formatted(schema.tableName());

        StringBuilder builder = new StringBuilder(start);
        builder.append(String.join(",\n", columns));

        if (primaryKeyColumns.isEmpty())
            return builder.append("\n);").toString();

        builder.append(",\n\tPRIMARY KEY(");
        builder.append(String.join(",", primaryKeyColumns));
        builder.append(")");

        builder.append("\n);");


        return builder.toString();
    }

    public static String sqlInsert(Schema schema) {
        String columns = schema.columnDefList().stream()
                .map(ColumnDef::name)
                .collect(Collectors.joining(","));

        String wildcards = schema.columnDefList().stream()
                .map(c -> "?::" + c.dataType())
                .collect(Collectors.joining(","));

        StringBuilder sql = new StringBuilder()
                .append("INSERT INTO ").append(schema.tableName())
                .append(" (").append(columns).append(") ")
                .append("VALUES (").append(wildcards).append(")");

        List<String> conflictColumns = schema.conflictTargets();

        if (conflictColumns.isEmpty()) {
            return sql.toString();
        }

        sql.append(" ON CONFLICT (")
                .append(String.join(",", conflictColumns))
                .append(") ");

        if (schema.conflictResolution() == ConflictResolutionMode.IGNORE) {
            sql.append("DO NOTHING");
        } else if (schema.conflictResolution() == ConflictResolutionMode.UPDATE) {
            String updateClause = schema.columnDefList().stream()
                    .filter(columnDef -> !columnDef.isPrimaryKey())
                    .map(ColumnDef::name)
                    .filter(columnDefName -> !conflictColumns.contains(columnDefName))
                    .map(conflictColumnName -> conflictColumnName + " = EXCLUDED." + conflictColumnName)
                    .collect(Collectors.joining(", "));

            sql.append("DO UPDATE SET ").append(updateClause);
        }
        sql.append(";");

        return sql.toString();
    }
}
