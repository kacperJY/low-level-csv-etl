package pl.kacperjy.etl.utils;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ConsoleReader {

    private final Scanner scanner = new Scanner(System.in);
    private Printer printer;

    public ConsoleReader(Printer printer) {
        this.printer = printer;
    }

    public int getInt(){
        while (true) {
            try {
                String numberString = scanner.nextLine();
                return Integer.parseInt(numberString);
            } catch (NumberFormatException e) {
                printer.printErrorMessage("Expected integer value. Try again.");
            }
        }
    }

    public String getString(){
        return scanner.nextLine();
    }
}
