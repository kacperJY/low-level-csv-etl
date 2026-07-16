# ETL Pipeline with Java NIO, Virtual Threads, and JDBC Batch Processing

A Java 25 ETL application for reading CSV files, processing records concurrently, and loading data into PostgreSQL.

The project was created as a learning exercise focused on low-level file I/O, concurrency, and direct database access without application frameworks.

## Tech Stack

- Java 25
- Java NIO Memory-Mapped Files
- Virtual Threads
- JDBC Batch Processing
- PostgreSQL
- HikariCP
- Maven

## How It Works

```text
CSV
 │
 ▼
Memory-Mapped Scanner
 │
 ▼
Parser
 │
 ▼
Batch Builder
 │
 ▼
JDBC Batch Insert
 │
 ▼
PostgreSQL
```

The file-reading stage retrieves data from disk, while parsing, batch processing, and database writes are performed concurrently using Virtual Threads.

The implementation includes:

- memory-mapped file access,
- JDBC batch inserts,
- semaphore-based concurrency limits,
- backpressure,
- record deduplication before database insertion,
- custom exception handling,
- SQLState mapping for more readable database error messages.

## Build and Run

### Build

```bash
mvn clean package
```

After the build is complete, the JAR file will be available in the `target/` directory.

### Run

```bash
java --enable-preview -jar target/file_name.jar
```

On the first run, the application creates a default configuration file.

Configure:

- PostgreSQL connection details,
- the directory containing JSON schema files.

CSV file paths are defined in the corresponding schema files.

The `examples/` directory contains an example JSON schema and a matching CSV file.

## Local Performance Results

The application was tested locally with approximately **1.3 million records**:

- **1,000,000** records from `employees.csv`,
- **300,000** records from `books.csv`.

Observed results:

- execution time: **3.3–4.0 seconds**,
- throughput: approximately **390,000 records per second**.

These results were measured in a local environment and may vary depending on the hardware, Docker and PostgreSQL configuration, storage performance, file structure, and input data.

## Benchmark Environment

- CPU: Intel Core Ultra 5 125H
- RAM: 16 GB LPDDR5x, 4800 MHz
- Storage: 1 TB M.2 PCIe SSD
- Operating system: Ubuntu 24 LTS
- Java: 25
- PostgreSQL: running in a Docker container
- Batch size: 1000
- HikariCP pool size: 15

## Roadmap

### Completed

- Memory-Mapped File Scanner
- JDBC Batch Inserts
- Virtual Threads
- Backpressure
- Batch deduplication
- SQLState error mapping

### Planned

- Row-level fallback for invalid records
- Additional column validation rules in JSON schemas
- Unit tests
