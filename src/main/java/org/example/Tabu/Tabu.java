package org.example.Tabu;

import com.sun.management.OperatingSystemMXBean;
import org.example.Data.InstancesClass;
import org.example.Parameters.Parameters;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tabu {
    private long startCpuTime;
    private long startTime;
    private final int patientLength;
    private final int MAX_TABU_SIZE;
    private final int MIN_TABU_SIZE;
    private final int caregiversNum;
    private final int numberOfIteration;
    private static Random rand;
    private Solution currentSolution;
    private Solution bestSolution;
    private final Queue<Integer> tabuList;
    private final List<Solution> relocateSolutions;
    private final List<Solution> tempSolutions;
    private final OperatingSystemMXBean osBean;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    public Tabu(Parameters parameters, long seed, InstancesClass instancesClass){
        this.osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        rand = new Random(seed);
        numberOfIteration = parameters.getNumberOfIteration();
        patientLength = instancesClass.getPatients().length;
        caregiversNum = instancesClass.getCaregivers().length;
        MAX_TABU_SIZE = patientLength / 3;
        MIN_TABU_SIZE = patientLength / 5;
        tabuList = new LinkedList<>();
        tempSolutions = new ArrayList<>(patientLength);
        relocateSolutions = Collections.synchronizedList(new ArrayList<>(patientLength));
    }
    
    public Solution evolve(){
        startTimer();
        currentSolution = Solution.initialize(patientLength);
        EvaluationFunction.EvaluateFitness(currentSolution);
        bestSolution = currentSolution;
        int tabuSize = (int) Math.round((MIN_TABU_SIZE + (MAX_TABU_SIZE - MIN_TABU_SIZE) * rand.nextDouble()));
        int lsStrategy = rand.nextInt(2);
        LocalSearchStrategy localSearchStrategy = getLocalSearch(lsStrategy);
        double bestFitness = bestSolution.getFitness();
        double tempBestFitness = bestFitness;
        int iterationWithoutImprovement = 0;
        for (int i = 0; i < numberOfIteration; i++){
            currentSolution = localSearchStrategy.execute(currentSolution);
            double currentFitness = currentSolution.getFitness();
//            System.out.println("Current: "+currentFitness);
            tabuList.add(currentSolution.getMove());
            if (currentFitness < tempBestFitness){
                if(currentFitness <= bestFitness){
                    bestSolution = currentSolution;
                    bestFitness = currentFitness;
                    iterationWithoutImprovement = 0;
                    if (tabuSize < MAX_TABU_SIZE)
                        tabuSize++;
                }
                update(i, iterationWithoutImprovement);
            }else {
                if (tabuSize > MIN_TABU_SIZE) {
                    tabuSize--;
                }
                lsStrategy = (lsStrategy + 1) % 10;
                localSearchStrategy = getLocalSearch(lsStrategy);
                iterationWithoutImprovement ++;
                
            }
            if (tabuList.size() > tabuSize) {
                tabuList.poll();
            }
            tempBestFitness = currentFitness;
            if (iterationWithoutImprovement > 0 && iterationWithoutImprovement % MAX_TABU_SIZE == 0) {
                List<Integer> selectedGene = currentSolution.getGenes()[rand.nextInt(caregiversNum)];
                Shuffle shuffle = new Shuffle(this, selectedGene,currentSolution,rand);
//                update(i, iterationWithoutImprovement);
                currentSolution = shuffle.Start();
//                System.out.println("Shuffled Solution: "+MAX_TABU_SIZE);
                update(i, iterationWithoutImprovement);
            }
        }
        update(numberOfIteration, iterationWithoutImprovement);

        return bestSolution;
    }
    @FunctionalInterface
    private interface LocalSearchStrategy {
        Solution execute(Solution currentSolution);
    }

    private LocalSearchStrategy getLocalSearch(int ls) {
        if (ls == 0) {
            return this::swapLocalSearch;
        }
        return this::relocateLocalSearch;
    }

    private Solution swapLocalSearch(Solution solution) {
        ExecutorService es = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        relocateSolutions.clear();
        List<Callable<Void>> swapTasks = new ArrayList<>();
//        for (int i = 0; i < patientLength; i++) {
//            SwapOperator sw = new SwapOperator(this, i, solution, rand);
//            relocateSolutions.add(sw.Start());
//            System.exit(1);
//        }
//
//        System.exit(1);
        for (int i = 0; i < patientLength; i++) {
            int finalI = i;
            swapTasks.add(()->{
                new SwapOperator(this, finalI,solution,rand).run();
                return null;
            });
        }

        tempSolutions.clear();
        invokeThreads(es,swapTasks);
        sortSolutions(tempSolutions);
        for (Solution sol : tempSolutions) {
            if (sol.getFitness() < bestSolution.getFitness()) {
                return sol;
            } else if (!tabuList.contains(sol.getMove())) {
                return sol;
            }
        }
        return tempSolutions.get(0);
    }

    private Solution relocateLocalSearch(Solution solution) {
        ExecutorService es = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        relocateSolutions.clear();
        List<Callable<Void>> relocateTasks = new ArrayList<>();
        for (int i = 0; i < patientLength; i++) {
            int finalI = i;
            relocateTasks.add(()->{
               new RelocateOperator(this, finalI,solution,rand).run();
               return null;
            });
        }
        tempSolutions.clear();
        invokeThreads(es,relocateTasks);
        sortSolutions(tempSolutions);
//        tempSolutions.forEach(tempSolution -> {System.out.println(tempSolution.getMove()+" - "+tempSolution.getFitness());});
//        double averageFitness = tempSolutions.stream().mapToDouble(Solution::getFitness).sum() / patientLength;
//        System.out.println(tabuList);
        for (Solution sol : tempSolutions) {
            if (sol.getFitness() < bestSolution.getFitness()) {
//                System.out.println("Best Move: "+sol.getMove()+" Fitness: "+sol.getFitness());
                return sol;
            } else if (!tabuList.contains(sol.getMove())) {
//                System.out.println("Other Move: "+sol.getMove()+" Fitness: "+sol.getFitness());
                return sol;
            }
        }

        return tempSolutions.get(0);
    }

    private void invokeThreads(ExecutorService service, List<Callable<Void>> neighborhoodTasks) {
        try {
            service.invokeAll(neighborhoodTasks);
            List<Solution> xSolutions = relocateSolutions;
            synchronized (xSolutions) {
                tempSolutions.addAll(xSolutions);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            service.shutdown();
        }
    }

    private void sortSolutions(List<Solution> solutions) {
        solutions.sort(Comparator.comparingDouble(Solution::getFitness));
    }

    public List<Solution> getRelocateSolutions(){
        return relocateSolutions;
    }

    public static boolean conflictCheck(List<Integer> c1Route, List<Integer> c2Route, int m, int n) {
        int index1;
        int index2;
        Set<Integer> route2 = new HashSet<>(c2Route);
        for (int i = 0; i < c1Route.size(); i++) {
            if (route2.contains(c1Route.get(i))) {
                index1 = c1Route.indexOf(c1Route.get(i));
                index2 = c2Route.indexOf(c1Route.get(i));
                if (m <= index1 && n > index2 || m > index1 && n <= index2) {
                    return false;
                }
            }
        }
        return true;
    }

    private void update(int iteration, int iterationWithoutImprovement) {
        System.out.println("Time at: " + getTotalTimeSeconds() + " CPU Timer " + String.format("%.3f", getTotalCPUTimeSeconds()) + " seconds Iteration " + iteration + " IterationWithoutImprovement " + iterationWithoutImprovement + " Current fitness: "+ currentSolution.getFitness() +" Best fitness: " + bestSolution.getFitness());
    }

    public void startTimer() {
        this.startCpuTime = osBean.getProcessCpuTime();
        this.startTime = System.currentTimeMillis();
    }

    public double getTotalCPUTimeSeconds() {
        long endCpuTime = osBean.getProcessCpuTime();
        return (endCpuTime - startCpuTime) / 1_000_000_000.0;
    }

    public double getTotalTimeSeconds() {
        long endTime = System.currentTimeMillis();
        return (endTime - startTime) / 1_000.0;
    }
}
