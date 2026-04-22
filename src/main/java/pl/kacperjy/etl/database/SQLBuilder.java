package pl.kacperjy.etl.database;

import pl.kacperjy.etl.model.ColumnDef;
import pl.kacperjy.etl.model.Schema;

import java.util.ArrayList;
import java.util.List;

public class SQLBuilder {


    public static String sqlCreate(Schema schema){


        List<String> columns = new ArrayList<>();
        List<String> primaryKeyColumns = new ArrayList<>();

        for (ColumnDef columnDef : schema.columnDefList()) {
            String textColumn = "\t" + columnDef.name() + " " + columnDef.dataType();

            if(columnDef.isUnique())
                textColumn += " UNIQUE";

            columns.add(textColumn);

            if(columnDef.isPrimaryKey())
                primaryKeyColumns.add(columnDef.name());
        }

        String start = "CREATE TABLE IF NOT EXISTS %s (\n".formatted(schema.tableName());

        StringBuilder builder = new StringBuilder(start);
        builder.append(String.join(",\n",columns));

        if(primaryKeyColumns.isEmpty())
            return builder.append("\n);").toString();

        builder.append(",\n\tPRIMARY KEY(");
        builder.append(String.join(",",primaryKeyColumns));
        builder.append(")");

        builder.append("\n);");

        return builder.toString();
    }
}
