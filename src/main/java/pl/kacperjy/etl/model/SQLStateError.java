package pl.kacperjy.etl.model;

public enum SQLStateError {
    DUPLICATE("23505", "“Unique constraint violation: A record with this identifier already exists.”"),
    VARCHAR_OVERFLOW("22001", "Data truncation: The provided value exceeds the maximum allowed length for this field"),
    INVALID_DATE_FORMAT("22007", "Invalid datetime format: The provided value does not match the required date/time structure."),
    NUMERIC_OVERFLOW("22003", "Numeric value out of range: The input number is too large or too small for this column."),
    INVALID_TYPE("42804", "Datatype mismatch: The provided value type is incompatible with the target column.");

    private final String sqlState;
    private final String description;

    SQLStateError(String sqlState, String description) {
        this.sqlState = sqlState;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getSqlState() {
        return sqlState;
    }
}
