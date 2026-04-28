🚀 High-Performance Java ETL Pipeline

Wydajny, wielowątkowy system ETL (Extract, Transform, Load) napisany w nowoczesnej Javie 25, zaprojektowany do błyskawicznego przetwarzania i ładowania gigabajtowych plików płaskich (CSV) do relacyjnej bazy danych (PostgreSQL).

Projekt skupia się na ekstremalnej wydajności wejścia/wyjścia (I/O) poprzez ominięcie standardowych mechanizmów strumieniowania Javy na rzecz bezpośredniego mapowania pamięci (NIO) oraz wykorzystaniu Wątków Wirtualnych (Project Loom) i Strukturalnej Współbieżności (Structured Concurrency) do zrównoleglenia zadań asynchronicznych.
🏗️ Architektura Systemu (Pipeline)

System został zaprojektowany zgodnie z zasadą Single Responsibility Principle oraz wzorcem Pipes and Filters. Przepływ danych dzieli się na dwie całkowicie odseparowane fazy, zarządzane przez główny Orkiestrator (DataLoader):

    Faza I/O (Producent): Niskopoziomowy odczyt z dysku operujący na surowych bajtach i offsetach pamięci.

    Faza CPU & DB (Konsument): Konwersja znaków, parsowanie i masowy zapis do bazy danych (JDBC Batch Insert), orkiestrowana przez Wątki Wirtualne.

Dzięki architekturze "Emit & Forget", skaner nigdy nie czeka na bazę danych (chroniony przez system Backpressure), a baza danych otrzymuje ujednolicone paczki danych z maksymalną przepustowością, zabezpieczone przed konfliktami współbieżności.
⚙️ Kluczowe Rozwiązania Inżynieryjne (Tech Highlights)
1. Zero-GC Data Scanner (Omijanie Garbage Collectora)

Główna pętla czytająca plik (DataFileScanner) została zaprojektowana w architekturze Zero-GC na gorącej ścieżce. Zamiast tworzyć obiekty String dla każdego słowa i tablice dla każdego wiersza, skaner iteruje po zmapowanych bajtach, nadpisując wyłącznie wskaźniki (typu long) w reużywalnych tablicach o stałym rozmiarze (BATCH_SIZE). Obiekty tworzone są dopiero na etapie paczkowania.
2. Pamięć Mapowana (NIO MappedByteBuffer)

Aby uniknąć wąskiego gardła, jakim jest RAM podczas czytania plików po 10 GB+, pliki mapowane są prosto do pamięci wirtualnej w oknach po 1 GB (PAGE_SIZE). Autorski MapMemoryReader potrafi płynnie i bezpiecznie wycinać bajty na styku dwóch gigabajtowych buforów.
3. Strukturalna Współbieżność i Backpressure

Przetwarzanie pociętych paczek (parsowanie z UTF-8 i wysyłka do bazy) odbywa się w lekkich Wątkach Wirtualnych przy użyciu API StructuredTaskScope. Aby zapobiec zagłodzeniu bazy danych i przepełnieniu RAM-u z powodu ogromnej szybkości Skanera, system używa Semaforów do precyzyjnego zarządzania ciśnieniem (Backpressure), zrównując tempo odczytu z rozmiarem puli połączeń HikariCP.
4. Izolacja Błędów i Fail-Fast

System wdraża rygorystyczną strategię Fail-Fast obsługiwaną przez DataLoader. Uszkodzone pliki (np. złe formaty dat, błędy unikalności) przerywają przetwarzanie tylko jednego, izolowanego schematu, pozwalając na płynne ładowanie pozostałych plików. Kody błędów PostgreSQL są w locie tłumaczone na przyjazne komunikaty przez dedykowany SQLStateError Enum.
5. Ochrona Bazy Danych (Deadlocks & Deduplication)

Przed wysłaniem paczki do bazy przez zoptymalizowany sterownik JDBC (reWriteBatchedInserts=true), system wykonuje na niej w pamięci RAM deduplikację za pomocą struktury TreeMap. Pozwala to na zachowanie tylko najnowszych rekordów dla danego klucza biznesowego i jednoczesne posortowanie paczki, co fizycznie eliminuje problem zakleszczeń (Deadlocks) wewnątrz PostgreSQL przy równoległych operacjach ON CONFLICT.
🧩 Główne Komponenty

    DataLoader: Główny orkiestrator. Zarządza iteracją po schematach, otwiera StructuredTaskScope, zarządza semaforami przepustowości i izoluje błędy wyjątków bazy danych oraz Skanera.

    DataFileScanner: Agresywny, niskopoziomowy czytnik plików. Odporny na braki znaków nowej linii na końcu pliku, puste wiersze i ucięte paczki. Przekazuje surowe bloki pamięci do parsowania.

    DataDAO: Uniwersalna warstwa dostępu do danych operująca na wzorcu Strategii. Pozwala na wstrzykiwanie dowolnych zachowań przy zapisie (np. FailFastBatchInsertStrategy).

    MapMemoryReader: Klasa obudowująca logikę wskaźników dla bloków MappedByteBuffer.

    DataBatch (DTO): Niemutowalny rekord transferowy. Stanowi kontrakt między fazą I/O a fazą procesora, przechowując offsety bajtów do bezkolizyjnego parsowania w wątkach.

📊 Benchmarks (Wydajność)

System został poddany testom obciążeniowym na lokalnej infrastrukturze, wykorzystując pulę połączeń HikariCP połączoną z asynchronicznym wczytywaniem NIO.

Środowisko testowe:

    Plik 1: pracownicy.csv (1 000 000 wierszy, 6 kolumn)

    Plik 2: books.csv (300 000 wierszy, 3 kolumny)

    Łączny wolumen: 1.3 miliona wierszy (prawie 7 milionów komórek danych).

    Strategia: Wątki Wirtualne + BATCH_SIZE = 1000 + Limit puli/Semafora = 15.

Wynik:
Całkowity czas wykonania programu (wczytanie I/O -> parsowanie -> tłumaczenie JDBC -> zapis na dysku bazy z obsługą indeksów) wyniósł średnio 3.3 - 4.0 sekundy.
Daje to przepustowość na poziomie około 390 000 wierszy na sekundę.

(W planach: Szczegółowy Micro-Benchmarking izolujący czasy samego Skanera, Parserów Wątków i operacji Network I/O bazy danych).

🚀 Status Projektu

    [x] Silnik I/O (Mapowanie pamięci)

    [x] Skaner NIO z architekturą Zero-GC

    [x] Implementacja Wątków Wirtualnych i Backpressure

    [x] Deduplikacja paczek i eliminacja Deadlocków

    [x] Tłumaczenie błędów SQLState

    [ ] Roadmap: Row-Level Fallback Strategy

    [ ] Roadmap: Obsługa Advanced Column Constraints (Regex/Min/Max)