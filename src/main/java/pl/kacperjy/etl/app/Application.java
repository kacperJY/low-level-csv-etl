package pl.kacperjy.etl.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kacperjy.etl.utils.ConsoleReader;
import pl.kacperjy.etl.utils.Printer;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final AppConfig appConfig;

    // UTILS
    private final Printer printer;
    private final ConsoleReader consoleReader;

    public Application(AppConfig appConfig) {
        this.appConfig = appConfig;

        // UTILS
        this.printer = new Printer();
        this.consoleReader = new ConsoleReader(printer);
    }

    public void start() {
        Option option;
        do {
            printer.printMenu(Option.values());

            option = Option.getOptionFromIndex(consoleReader.getInt());
            if(option == null){
                printer.printLine("There is no such option in menu. Try again");
                continue;
            }

            switch (option){
                case EXIT -> {
                }
                case SHOW_SCHEMAS -> {
                }
                case PERSIST_SCHEMAS -> {
                }
                case LOAD_DATA -> {
                }
            }
        } while (option != Option.EXIT);
    }

    private static enum Option {
        EXIT(0, "Exit"),
        SHOW_SCHEMAS(1, "Show schemas list"),
        PERSIST_SCHEMAS(2, "Persist schemas"),
        LOAD_DATA(3, "Load csv files");

        private final int optionIndex;
        private final String description;

        Option(int optionIndex, String description) {
            this.optionIndex = optionIndex;
            this.description = description;
        }

        @Override
        public String toString() {
            return optionIndex + " - " + description;
        }

        public int getOptionIndex() {
            return optionIndex;
        }

        public String getDescription() {
            return description;
        }

        public static Option getOptionFromIndex(int index) {
            for (Option value : Option.values()) {
                if(value.optionIndex == index)
                    return value;
            }
            return null;
        }
    }
}
