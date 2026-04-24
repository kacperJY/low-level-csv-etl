🚀 High-Performance Java ETL Pipeline

Wydajny, wielowątkowy system ETL (Extract, Transform, Load) napisany w nowoczesnej Javie, zaprojektowany do błyskawicznego przetwarzania i ładowania gigabajtowych plików płaskich (CSV) do relacyjnej bazy danych (PostgreSQL).

Projekt skupia się na ekstremalnej wydajności wejścia/wyjścia (I/O) poprzez ominięcie standardowych mechanizmów strumieniowania Javy na rzecz bezpośredniego mapowania pamięci (NIO) oraz wykorzystaniu Wątków Wirtualnych (Project Loom) do zrównoleglenia zadań asynchronicznych.
🏗️ Architektura Systemu (Pipeline)

System został zaprojektowany zgodnie z zasadą Single Responsibility Principle oraz wzorcem Pipes and Filters. Przepływ danych dzieli się na dwie całkowicie odseparowane fazy:

    Faza I/O (Producent): Niskopoziomowy odczyt z dysku operujący na surowych bajtach i offsetach pamięci.

    Faza CPU & DB (Konsument): Konwersja znaków, parsowanie i masowy zapis do bazy danych (JDBC Batch Insert), orkiestrowana przez Wątki Wirtualne.

Dzięki architekturze "Emit & Forget", skaner nigdy nie czeka na bazę danych (chroniony przez system Backpressure), a baza danych otrzymuje pocięte paczki danych z maksymalną przepustowością.
⚙️ Kluczowe Rozwiązania Inżynieryjne (Tech Highlights)
1. Zero-GC Data Scanner (Omijanie Garbage Collectora)

Główna pętla czytająca plik (DataFileScanner) została zaprojektowana w architekturze Zero-GC na gorącej ścieżce. Zamiast tworzyć obiekty String dla każdego słowa i tablice dla każdego wiersza, skaner iteruje po zmapowanych bajtach, nadpisując wyłącznie wskaźniki (typu long) w reużywalnych tablicach o stałym rozmiarze (BATCH_SIZE). Obiekty tworzone są dopiero na etapie paczkowania.
2. Pamięć Mapowana (NIO MappedByteBuffer)

Aby uniknąć wąskiego gardła, jakim jest RAM podczas czytania plików po 10 GB+, pliki mapowane są prosto do pamięci wirtualnej w oknach po 1 GB (PAGE_SIZE). Autorski MapMemoryReader potrafi płynnie i bezpiecznie wycinać bajty na styku dwóch gigabajtowych buforów, korzystając z matematyki absolutnych przesunięć (offsetów).
3. Wirtualne Wątki (Project Loom) i Backpressure

Przetwarzanie pociętych paczek (parsowanie z UTF-8 na String i wysyłka do bazy) odbywa się w lekkich Wątkach Wirtualnych w modelu Thread-per-Batch. Aby zapobiec zagłodzeniu bazy danych (DDoS na własną pulę połączeń HikariCP) i przepełnieniu RAM-u z powodu ogromnej szybkości Skanera, system używa Semaforów do zarządzania ciśnieniem (Backpressure), blokując Producenta, gdy Konsumenci nie nadążają.
🧩 Główne Komponenty

    DataFileScanner: Agresywny, niskopoziomowy czytnik plików. Wylicza globalne pozycje znaków nowej linii oraz separatorów. Po zebraniu np. 1000 wierszy, wycina z pamięci surowy blok bajtów i emituje go dalej. Odporny na problem uciętych resztówek na końcu pliku (The Tail End Problem).

    MapMemoryReader: Klasa obudowująca logikę obsługi listy obiektów MappedByteBuffer. Tłumaczy globalne pozycje w pliku na fizyczne indeksy wewnątrz 1-gigabajtowych stron pamięci operacyjnej.

    DataBatch (DTO): Niemutowalny rekord transferowy. Stanowi "kontrakt" między fazą I/O a fazą procesora. Przechowuje wyciętą paczkę surowych bajtów oraz pozycje początków i końców słów wewnątrz tej paczki.

🚀 Status Projektu

    [x] Silnik I/O (Mapowanie pamięci)

    [x] Skaner NIO z architekturą Zero-GC

    [x] Obsługa podziału na Batch i hermetyzacja DTO

    [ ] Parser paczek (Tłumaczenie bajtów na tekst UTF-8)

    [ ] Implementacja Wątków Wirtualnych z mechanizmem Backpressure

    [ ] Zapis do bazy danych PostgreSQL przez pulę połączeń HikariCP