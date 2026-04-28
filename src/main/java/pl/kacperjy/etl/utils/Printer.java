package pl.kacperjy.etl.utils;

import java.io.PrintWriter;

public class Printer {

    public void printLine(Object obj) {
        printLine("", obj);
    }

    public void print(Object obj){
        System.out.print(obj);
    }

    public void printLine(String prefix, Object obj){
        System.out.println(prefix + obj);
    }

    public void printMenu(Object[] array) {
        for (Object object : array) {
            printLine(object);
        }
    }

    public void printHeader(String headerName) {
        String content = "### %s ###".formatted(headerName);
        printLine(content);
    }

    public void printErrorMessage(Object obj) {
        printErrorMessage("",obj);

    }

    public void printErrorMessage(String prefix, Object obj) {
        if (obj instanceof Throwable throwable)
            printLine(prefix + "### ERROR : " + throwable.getMessage());
        else
            printLine(prefix + "### ERROR : " + obj.toString());
    }
}

