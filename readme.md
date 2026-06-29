# 🚀 High-Performance Java ETL Pipeline

Wydajny, wielowątkowy system ETL (Extract, Transform, Load) napisany w nowoczesnej **Javie 25**, zaprojektowany do błyskawicznego przetwarzania i ładowania gigabajtowych plików płaskich (CSV) do relacyjnej bazy danych (PostgreSQL).

Projekt skupia się na ekstremalnej wydajności wejścia/wyjścia (I/O). Omija standardowe mechanizmy strumieniowania Javy na rzecz **bezpośredniego mapowania pamięci (NIO)** oraz wykorzystuje **Wątki Wirtualne (Project Loom)** i **Strukturalną Współbieżność** do zrównoleglenia zadań asynchronicznych.

## 🚀 Szybki start (Budowanie i Uruchamienie)

Projekt wykorzystuje narzędzie Maven do zarządzania zależnościami i budowania. Proces kompilacji został skonfigurowany przy użyciu `maven-assembly-plugin`, co pozwala na wygenerowanie w pełni wykonywalnego pliku "Fat JAR" ze wszystkimi zależnościami.

### 1. Budowanie projektu
Aby zbudować projekt, otwórz terminal w głównym katalogu i uruchom polecenie:
```bash
mvn clean package
```

Skompilowany plik wykonywalny (np. etl-pipeline-jar-with-dependencies.jar) pojawi się w katalogu target/.
2. Pierwsze uruchomienie i konfiguracja

System został zaprojektowany tak, aby był gotowy do działania (Plug & Play).
Uruchom wygenerowany plik JAR po raz pierwszy:

```bash
java --enable-preview -jar target/file_name.jar
```

Podczas pierwszego uruchomienia, aplikacja automatycznie wygeneruje domyślny plik konfiguracyjny (np. application.properties / config.json).

Kroki konfiguracyjne:

    Otwórz nowo wygenerowany plik konfiguracyjny.

    Uzupełnij dane uwierzytelniające do bazy danych PostgreSQL (URL, użytkownik, hasło).

    Wskaż ścieżkę do folderu, w którym aplikacja ma szukać plików schematów JSON.

    Ważne: Ścieżki do samych plików z danymi (CSV) nie są definiowane globalnie – wskazuje się je bezpośrednio wewnątrz konkretnego pliku schematu JSON.

3. Dane przykładowe (Examples)

Aby ułatwić start, w katalogu examples/ w głównym drzewie projektu umieszczono:

    Przykładowy plik konfiguracyjny schematu .json.

    Kompatybilny z nim testowy plik danych .csv.

🏗️ Architektura Systemu (Pipeline)

System został zaprojektowany zgodnie z zasadami Single Responsibility Principle oraz wzorcem Pipes and Filters. Przepływ danych dzieli się na dwie całkowicie odseparowane fazy, zarządzane przez główny Orkiestrator (DataLoader):

    Faza I/O (Producent): Niskopoziomowy odczyt z dysku operujący na surowych bajtach i offsetach pamięci.

    Faza CPU & DB (Konsument): Konwersja znaków, parsowanie i masowy zapis do bazy danych (JDBC Batch Insert), orkiestrowana przez Wątki Wirtualne.

Dzięki architekturze "Emit & Forget", Skaner plików nigdy nie czeka na bazę danych, a Orkiestrator zabezpiecza system przed przepełnieniem pamięci przy pomocy mechanizmu Backpressure.
⚙️ Kluczowe Rozwiązania Inżynieryjne (Tech Highlights)

    Zero-GC Data Scanner: Główna pętla czytająca (DataFileScanner) unika ciągłego alokowania pamięci na gorącej ścieżce. Zamiast tworzyć tysiące obiektów String, Skaner iteruje po zmapowanych bajtach, nadpisując wyłącznie wskaźniki (typu long) w reużywalnych tablicach (BATCH_SIZE).

    Pamięć Mapowana (NIO MappedByteBuffer): Aby uniknąć wąskiego gardła RAM przy plikach 10 GB+, pliki mapowane są bezpośrednio do pamięci wirtualnej w oknach po 1 GB (PAGE_SIZE), z autorską bezpieczną obsługą przesunięć na stykach buforów.

    Strukturalna Współbieżność i Backpressure: Przetwarzanie i wysyłka paczek odbywa się w lekkich Wątkach Wirtualnych (StructuredTaskScope). Zastosowanie Semaforów zrównuje tempo odczytu z rozmiarem puli połączeń HikariCP.

    Ochrona bazy przed Deadlockami i Deduplikacja: Przed wysłaniem paczki przez zoptymalizowany sterownik JDBC (reWriteBatchedInserts=true), system wykonuje w pamięci RAM deduplikację za pomocą struktury TreeMap. Sortuje to paczkę po kluczach biznesowych, fizycznie eliminując zakleszczenia wewnątrz PostgreSQL przy równoległych operacjach ON CONFLICT.

    Izolacja Błędów (Fail-Fast): Uszkodzone pliki przerywają przetwarzanie tylko jednego, izolowanego schematu. Kody błędów bazy są w locie tłumaczone z surowego formatu SQLState na czytelne komunikaty biznesowe.

📊 Wydajność (Micro-Benchmarking)

System został poddany rygorystycznym testom obciążeniowym na lokalnej infrastrukturze, wykorzystując asynchroniczne I/O oraz zoptymalizowaną pulę HikariCP.

Środowisko testowe:

    Zbiór danych: pracownicy.csv (1 000 000 wierszy, 6 kolumn) + books.csv (300 000 wierszy, 3 kolumny).

    Łączny wolumen: 1.3 miliona wierszy (prawie 7 milionów analizowanych komórek danych).

    Konfiguracja: Wątki Wirtualne + BATCH_SIZE = 1000 + Max Pool Size = 15.

Wynik:
Całkowity czas wykonania programu (wczytanie I/O ➔ parsowanie UTF-8 ➔ translacja JDBC ➔ fizyczny zapis na dysku bazy z obsługą indeksów B-Tree) wynosi średnio 3.3 - 4.0 sekundy.
Osiągnięta przepustowość to około 390 000 przetworzonych wierszy na sekundę.
🗺️ Status Projektu i Plany Rozwoju (Roadmap)

Obecna architektura jest stabilna, przetestowana i gotowa do pracy (Feature Complete).

Zrealizowano:

    [x] Silnik wejścia/wyjścia z użyciem pamięci mapowanej (NIO).

    [x] Skaner w architekturze Zero-GC.

    [x] Wdrożenie Wątków Wirtualnych i zarządzanie ciśnieniem (Backpressure).

    [x] Własny Orkiestrator (DataLoader) izolujący awarie.

    [x] Deduplikacja paczek i strukturalna ochrona przed zakleszczeniami bazy danych (Deadlocks).

    [x] Automatyczne mapowanie kodów SQLState.

    [x] Skrypty budujące system (Maven Assembly).

Future Updates (W planach):

    [ ] Row-Level Fallback Strategy: Przełączanie systemu w tryb izolowania pojedynczych błędnych wierszy w przypadku awarii całej paczki, zamiast jej całkowitego odrzucania.

    [ ] Advanced Column Constraints: Obsługa zaawansowanych obostrzeń walidacyjnych aplikacji (np. wymuszanie wzorców Regex, walidacja zakresów min/max) zdefiniowanych na poziomie schematów JSON.