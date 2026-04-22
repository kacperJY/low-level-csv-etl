package pl.kacperjy.etl.utils;

import java.io.PrintWriter;

public class Printer {

    public void printLine(Object obj){
        System.out.println(obj);
    }

    public void printMenu(Object[] array){
        for (Object object : array) {
            printLine(object);
        }
    }

    public void printErrorMessage(Object obj){
        if(obj instanceof Throwable throwable)
            printLine("### ERROR : " + throwable.getMessage());
        else
            printLine("### ERROR : " + obj.toString());

    }
}
