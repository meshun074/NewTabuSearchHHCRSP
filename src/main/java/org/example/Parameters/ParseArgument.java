package org.example.Parameters;

public class ParseArgument {
    public static Parameters getConfiguration(String[] args) {
        String instance = "";
        int totalIterations = 20000;


        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--iteration":
                    totalIterations = Integer.parseInt(args[++i]);
                case "--instance":
                    instance = args[++i];
                    break;
            }
        }
        return new Parameters(instance, totalIterations);
    }
}
