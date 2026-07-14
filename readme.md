# ETL Pipeline using Java NIO, Virtual Threads and JDBC Batch Processing

Projekt ETL napisany w **Java 25**, służący do wydajnego przetwarzania plików CSV i zapisywania danych do bazy **PostgreSQL**.

Głównym celem projektu było sprawdzenie, jak daleko można zoptymalizować proces odczytu i ładowania dużych plików przy użyciu nowoczesnych funkcji Javy, takich jak **NIO (Memory Mapped Files)**, **Virtual Threads (Project Loom)** oraz **JDBC Batch Processing**.

Projekt został napisany głównie w celach edukacyjnych, aby lepiej zrozumieć niskopoziomowe operacje I/O, współbieżność oraz komunikację z bazą danych bez użycia frameworków.

---

# 🚀 Quick Start

## Build

```bash
mvn clean package
```

Po zakończeniu kompilacji plik JAR zostanie utworzony w katalogu `target/`.

## Run

```bash
java --enable-preview -jar target/file_name.jar
```

Przy pierwszym uruchomieniu aplikacja automatycznie wygeneruje domyślny plik konfiguracyjny.

Następnie wystarczy:

- uzupełnić dane połączenia z PostgreSQL,
- wskazać katalog z plikami schematów JSON.

Ścieżki do plików CSV definiowane są bezpośrednio w odpowiednim pliku schematu.

---

# 📂 Example

W katalogu `examples/` znajdują się przykładowe pliki:

- przykładowy schemat JSON,
- przykładowy plik CSV zgodny ze schematem.

---

# 🏗️ Architecture

Pipeline został podzielony na dwa niezależne etapy:

```text
CSV
 │
 ▼
Memory Mapped Scanner
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

Odczyt danych odpowiada wyłącznie za pobieranie danych z dysku, natomiast przetwarzanie oraz zapis do bazy wykonywane są równolegle przy użyciu Virtual Threads.

Takie rozdzielenie upraszcza odpowiedzialności poszczególnych komponentów i pozwala efektywnie wykorzystać zasoby systemu.

---

# ⚙️ Implementation Details

Projekt wykorzystuje kilka rozwiązań, które miały na celu poprawę wydajności:

- odczyt plików przez **Memory Mapped Files (Java NIO)**,
- **Virtual Threads** do równoległego przetwarzania batchy,
- **JDBC Batch Inserts** do ograniczenia liczby operacji na bazie,
- kontrolę liczby równoległych zadań przy pomocy semaforów,
- deduplikację rekordów przed zapisem,
- własną obsługę wyjątków oraz mapowanie kodów SQLState na bardziej czytelne komunikaty.

---

# 📊 Performance

Projekt został przetestowany lokalnie na zestawie:

- **1 000 000** rekordów (`employees.csv`),
- **300 000** rekordów (`books.csv`).

Łącznie około **1,3 mln rekordów**.

Konfiguracja testowa:

- Java 25
- Virtual Threads
- Batch Size = 1000
- HikariCP Pool Size = 15

Średni czas wykonania:

- **3.3–4.0 s**

Przepustowość:

- **około 390 000 rekordów/s**

---

# ✅ Roadmap

## Done

- ✅ Memory Mapped File Scanner
- ✅ JDBC Batch Inserts
- ✅ Virtual Threads
- ✅ Backpressure
- ✅ Batch deduplication
- ✅ SQLState error mapping

## Planned

- ⏳ Row-level fallback dla błędnych rekordów
- ⏳ Dodatkowe reguły walidacji kolumn w schematach JSON
- ⏳ Testy jednostkowe
