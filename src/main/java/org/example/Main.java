package org.example;

import org.example.Data.InstancesClass;
import org.example.Data.ReadData;
import org.example.Tabu.EvaluationFunction;
import org.example.Tabu.Solution;
import org.example.Parameters.Parameters;
import org.example.Parameters.ParseArgument;
import org.example.Tabu.Tabu;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static InstancesClass instance;

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Usage: java -jar example.jar <instanceIndex> <experimentNumber>");
            System.exit(1);
        }

        try {

            String instancePrefix = args[0];
            double avg = 0;
            double max = Double.POSITIVE_INFINITY;
            int runs = 1;
            for (int runCount = 1; runCount <= runs; runCount++) {
                Solution bestSolution = null;
                while (bestSolution == null || bestSolution.getFitness() == Double.POSITIVE_INFINITY) {
//              int runCount = Integer.parseInt(args[1]);
                    long randomSeed = System.currentTimeMillis() + runCount;

                    Path dataDir = Paths.get("src/main/java/org/example/Data/instance");
                    if (!Files.isDirectory(dataDir)) {
                        throw new IllegalStateException("Data directory not found: " + dataDir);
                    }

                    // Locate instance file
                    Optional<Path> instanceFile = Files.list(dataDir)
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().startsWith(instancePrefix))
                            .findFirst();

                    if (instanceFile.isEmpty()) {
                        throw new IllegalArgumentException("No instance file found for prefix: " + instancePrefix);
                    }

                    Path jsonFile = instanceFile.get();
                    String fileName = jsonFile.getFileName().toString();

                    // Extract problem size (p<number>)
//                    Matcher matcher = Pattern.compile("\\bp\\d+\\b").matcher(fileName);
//                    String problemSize = matcher.find() ? matcher.group().substring(1) : "unknown";
                    Matcher matcher = Pattern
                            .compile("^(\\d+)_(\\d+)\\.json$")
                            .matcher(fileName);

                    String problemSize = matcher.matches() ? matcher.group(1) : "unknown";

//                    // Create result directory safely
//                    String resultDirName = "MultiDepotHHCRSP_"
//                            + fileName.substring(0, Math.min(20, fileName.length()))
//                            + "_results";
//
//                    Path resultDir = Paths.get("src/main/java/org/example", resultDirName);
//                    Files.createDirectories(resultDir);
//
//                    // Redirect output to result file
//                    Path outputFile = resultDir.resolve(
//                            "Result_" + instancePrefix + "_" + runCount + "_" + randomSeed + ".txt"
//                    );
//
//                    try (PrintStream fileOut = new PrintStream(outputFile.toFile())) {
//                        System.setOut(fileOut);

                    System.out.printf(
                            "Config Parameters: ProblemSize=%s, instanceNumber=%s, seed=%d%n",
                            problemSize, instancePrefix, randomSeed
                    );

                    // Read instance
                    instance = ReadData.read(jsonFile.toFile());

                    // Parse GA parameters
                    Parameters parameters = ParseArgument.getConfiguration(args);

//                    ArrayList<Integer>[] caregivers = new ArrayList[3];
//
//                    caregivers[0] = new ArrayList<>(Arrays.asList(9, 2, 4, 8, 6));
//                    caregivers[1] = new ArrayList<>(Arrays.asList(7));
//                    caregivers[2] = new ArrayList<>(Arrays.asList(7, 9, 5, 1, 0, 8, 3));
//                    Solution s = new Solution(caregivers,0.0,true);
//                    EvaluationFunction.Evaluate(s);
//
//                    System.out.println("Best Fitness: " + s.getFitness());
//                    System.exit(1);

                    // Run GA
                    Tabu ts = new Tabu(parameters, randomSeed, instance);
                    bestSolution= ts.evolve();
                    avg += bestSolution.getFitness();
                    if (bestSolution.getFitness() < max && bestSolution.getFitness() != Double.POSITIVE_INFINITY) {
                        max = bestSolution.getFitness();
                    }

                    assert bestSolution != null;
                    System.out.println("----------------- Solution ----------------------");
                    System.out.println(" Best Fitness: " + bestSolution.getFitness());
                    System.out.println("Total Distance: " + bestSolution.getTotalTravelCost() + " Total Tardiness: " + bestSolution.getTotalTardiness() + " Highest Tardiness: " + bestSolution.getHighestTardiness() +
                            " Total Waiting Time " + bestSolution.getTotalWaitingTime() + " Total Overtime " + bestSolution.getOvertime() + " Highest Idle Time " + bestSolution.getHighestIdleTime());
                    bestSolution.showSolution(0);
//                    }
                }

            }
            System.out.println("Best: " + max);
            System.out.println("Average Fitness: " + avg / runs);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}