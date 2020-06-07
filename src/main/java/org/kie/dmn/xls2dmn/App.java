package org.kie.dmn.xls2dmn;

import java.io.File;

public class App {
    public static void main(String[] args) {
        if (args.length != 1 && args.length !=2) {
            throw new RuntimeException("Wrong arguments: inputfile [outputfile]");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            throw new RuntimeException(inputFile + " does not exists");
        }
        File outputFile = args.length ==2 ? new File(args[1]) : new File(inputFile.getAbsolutePath() +".dmn");
        System.out.println("Using inputFile: "+inputFile.getAbsolutePath());
        System.out.println("Using outputFile: "+outputFile.getAbsolutePath());
        new XLS2DMNParser(outputFile).parseFile(inputFile);
    }
}