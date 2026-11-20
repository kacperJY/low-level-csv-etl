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

## Memory and Allocation

The pipeline separates allocation-free scanning from allocation-heavy parsing.

**Scanning (one thread, no per-row allocations)**

- The file is memory-mapped in 1 GB windows, because `MappedByteBuffer` is indexed by `int` and cannot address a larger region at once.
- Bytes are read through a single reused 4 KB buffer.
- Row boundaries and separator positions are stored as offsets in reused `long[]` arrays. No objects are created per row or per field while scanning.

**Batch hand-off (one set of allocations per 1,000 rows)**

- When a batch is full, its bytes and offset arrays are copied into a new `DataBatch`.
- The copy is deliberate: each worker owns its data, so the scanner can keep reusing its buffers without sharing mutable state between threads.

**Parsing and loading (worker Virtual Threads)**

- Each field becomes a `String` and each row a `String[]`, which are bound with `setObject` in the JDBC batch. This is where most of the allocation happens.
- A semaphore limits the number of batches in flight to 15, matching the HikariCP pool size. The scanner blocks before submitting the next batch, which applies backpressure and keeps memory usage bounded.

**Possible improvements**

- Loading data with PostgreSQL `COPY` (`CopyManager`) instead of batched `INSERT` statements.
- Parsing numeric columns directly from bytes to avoid intermediate `String` objects.

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