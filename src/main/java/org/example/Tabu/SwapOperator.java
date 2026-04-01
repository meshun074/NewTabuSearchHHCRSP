package org.example.Tabu;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;
import org.example.Main;

import java.util.*;

import static org.example.Tabu.EvaluationFunction.patientIsAssigned;
import static org.example.Tabu.RelocateOperator.getRouteIndexMethod;

public class SwapOperator implements Runnable {
    private final Tabu tabu;
    private final Solution p1;
    private static InstancesClass dataset = Main.instance;
    private static Patient[] allPatients = dataset.getPatients();
    private static final Caregiver[] allCaregivers = dataset.getCaregivers();
    private static int numOfCaregivers = dataset.getCaregivers().length;
    private final static int numOfDepartingPoints = 1;
    private static double[][] distances = dataset.getDistances();
    private final Random rand;
    private final int[] routeEndPoint;
    private final double[] routesCurrentTime;
    private final double[] highestAndTotalTardiness;
    private final int selectedPatient;
    private final Set<Integer> track;

    public SwapOperator(Tabu tabu, int selectedPatient, Solution p1, Random rand) {
        this.tabu = tabu;
        this.selectedPatient = selectedPatient;
        this.p1 = p1;
        this.rand = rand;
        this.routeEndPoint = new int[numOfCaregivers];
        this.routesCurrentTime = new double[numOfCaregivers];
        this.highestAndTotalTardiness = new double[2];
        this.track = new HashSet<>(100);
    }

    public Solution Start() {
        // Initialize variables
        List<Integer>[] p1Routes = p1.getGenes();
        List<Integer>[] c1Routes = new ArrayList[p1Routes.length];

        //Remove patients of selected route from parents
        for (int i = 0; i < p1Routes.length; i++) {
            List<Integer> route = new ArrayList<>(p1Routes[i]);
            c1Routes[i] = route;
        }
//        System.out.println("Original "+p1);

        Solution cTemp = new Solution(c1Routes, 0.0, false);
        EvaluationFunction.Evaluate(cTemp);
        boolean isInvalid;
        double bestFitness = cTemp.getFitness();
//        System.out.println("Selected Patient: "+selectedPatient);
//        System.out.println(cTemp);

        isInvalid = bestFitness == Double.POSITIVE_INFINITY;

        Patient p = allPatients[selectedPatient];
        cTemp.buildPatientRouteMap();
        double bestCost = Double.MAX_VALUE;
//        System.out.println("bestCost: " + bestCost);
        int bestFirst = -1;
        int bestSecond = -1;
        int bestThird = -1;
        int bestFirstPosition = -1;
        int bestSecondPosition = -1;
        int bestThirdPosition = -1;
        int bestPatient1 = -1;
        int bestPatient2 = -1;
        int pos = -1;

        Shift[] shifts = cTemp.getCaregiversRouteUp();
        int patient = selectedPatient;
        if (p.getRequired_caregivers().length > 1) {
            List<CaregiverPair> caregiverPairs = p.getAllPossibleCaregiverCombinations();
            List<Integer> patientIndexRoutes = new ArrayList<>(cTemp.getPatientRoutes(patient));
            int patientRouteIndex1 = patientIndexRoutes.get(0), patientRouteIndex2 = patientIndexRoutes.get(1);
            Set<Integer> firstPossibleRoutes = p.getPossibleFirstCaregiver();
            Set<Integer> secondPossibleRoutes = p.getPossibleSecondCaregiver();
            if (!firstPossibleRoutes.contains(patientRouteIndex1) || !secondPossibleRoutes.contains(patientRouteIndex2)) {
                patientRouteIndex1 = patientIndexRoutes.get(1);
                patientRouteIndex2 = patientIndexRoutes.get(0);
            }
            int p1Index = c1Routes[patientRouteIndex1].indexOf(patient);
            int p2Index = c1Routes[patientRouteIndex2].indexOf(patient);
            for (int x = 0; x < caregiverPairs.size(); x++) {
                CaregiverPair caregiverPair = caregiverPairs.get(x);
                int c1 = caregiverPair.getFirst();
                if (c1 == patientRouteIndex1) {
                    for (int j = 0; j < c1Routes[c1].size(); j++) {
                        int otherPatient1 = c1Routes[c1].get(j);
                        swapPatients(c1Routes[c1], p1Index, j);
                        boolean firstSwap = otherPatient1 != patient;
                        int firstPosition = Math.min(p1Index, j);
                        if (firstSwap) {
                            //Todo
//                            Solution temp = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
//                            temp.setFirst(c1);
//                            temp.setFirstPosition(first);
//                            bestChromosome = evaluateMove(temp, bestChromosome, cTemp, isInvalid);

                            double tempCost = calSwapMoveCost(1, c1, firstPosition, -1, -1, -1, -1, -1, -1, patient, otherPatient1, j, -1, -1, cTemp, bestCost, shifts, isInvalid);
//                            System.out.println("best cost: " + bestCost);
//                            System.out.println("1 Swap cost: " + tempCost);
//                            EvaluationFunction.EvaluateFitness(cTemp);
//                            double subCost = cTemp.getFitness();
//                            System.out.println("2 Swap cost: " + subCost);
//                            System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                    " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                    " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                            if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                bestCost = tempCost;
                                bestFirst = c1;
                                bestSecond = -1;
                                bestThird = -1;
                                bestFirstPosition = j;
                                bestSecondPosition = p1Index;
                                bestThirdPosition = -1;
                                bestPatient1 = otherPatient1;
                                bestPatient2 = -1;
                                pos = 1;
//                                if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                    System.out.println("Sub Cost s1:" + subCost);
//                                    System.out.println("best Cost s1:" + bestCost);
//                                    EvaluationFunction.Evaluate(cTemp);
//                                    System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                    System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                    System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                    System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                    int u =0 ;
//                                    for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                        System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                        u++;
//                                    }
//                                    System.exit(1);
//                                }
                            }

                        }
                        int c2 = caregiverPair.getSecond();
                        if (c2 == patientRouteIndex2) {
                            for (int k = 0; k < c1Routes[c2].size(); k++) {
                                int otherPatient2 = c1Routes[c2].get(k);
                                swapPatients(c1Routes[c2], p2Index, k);
                                boolean secondSwap = otherPatient2 != patient;
                                if (secondSwap) {
                                    //Todo
//                                    Solution temp1 = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
                                    int secondPosition = Math.min(p2Index, k);
                                    if (firstSwap) {
//                                        temp1.setFirst(c1);
//                                        temp1.setFirstPosition(first);
//                                        temp1.setSecond(c2);
//                                        temp1.setSecondPosition(second);
//                                        bestChromosome = evaluateMove(temp1, bestChromosome, cTemp, isInvalid);
                                        double tempCost = calSwapMoveCost(2, c1, firstPosition, -1, -1, c2, secondPosition, -1, -1, patient, otherPatient1, j, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = c1;
                                            bestSecond = -1;
                                            bestThird = c2;
                                            bestFirstPosition = j;
                                            bestSecondPosition = p1Index;
                                            bestThirdPosition = k;
                                            bestPatient1 = otherPatient1;
                                            bestPatient2 = otherPatient2;
                                            pos = 2;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                                System.out.println("Sub Cost s2 :" + subCost);
//                                                System.out.println("best Cost s2:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    } else {
//                                        temp1.setFirst(c2);
//                                        temp1.setFirstPosition(second);
//                                        bestChromosome = evaluateMove(temp1, bestChromosome, cTemp, isInvalid);
                                        double tempCost = calSwapMoveCost(3,-1, -1, -1, -1, c2, secondPosition,-1, -1, patient, -1, -1, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = -1;
                                            bestSecond = -1;
                                            bestThird = c2;
                                            bestFirstPosition = -1;
                                            bestSecondPosition = -1;
                                            bestThirdPosition = k;
                                            bestPatient1 = -1;
                                            bestPatient2 = otherPatient2;
                                            pos = 3;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                                System.out.println("Sub Cost s3:" + subCost);
//                                                System.out.println("best Cost s3:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    }
                                }
                                swapPatients(c1Routes[c2], p2Index, k);
                            }
                        } else {
                            for (int k = 0; k < c1Routes[c2].size(); k++) {
                                int otherPatient2 = c1Routes[c2].get(k);
                                Patient patient2 = allPatients[otherPatient2];
                                if (swapIsPossible(cTemp, patientRouteIndex1, otherPatient2, patient2, patientRouteIndex2, c2)) {
                                    c1Routes[c2].set(k, patient);
                                    c1Routes[patientRouteIndex2].set(p2Index, otherPatient2);
                                    //Todo
//                                    Solution temp = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
                                    if (firstSwap) {
//                                        temp.setFirst(c1);
//                                        temp.setFirstPosition(first);
//                                        temp.setSecond(c2);
//                                        temp.setSecondPosition(k);
//                                        temp.setThird(patientRouteIndex2);
//                                        temp.setThirdPosition(p2Index);
//                                        bestChromosome = evaluateMove(temp, bestChromosome, cTemp, isInvalid);
                                        double tempCost = calSwapMoveCost(4, c1, firstPosition, -1, -1, c2, k, patientRouteIndex2, p2Index, patient, otherPatient1, j, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = c1;
                                            bestSecond = -1;
                                            bestThird = c2;
                                            bestFirstPosition = j;
                                            bestSecondPosition = p1Index;
                                            bestThirdPosition = k;
                                            bestPatient1 = otherPatient1;
                                            bestPatient2 = otherPatient2;
                                            pos = 4;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                                System.out.println("Sub Cost s4:" + subCost);
//                                                System.out.println("best Cost s4:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    } else {
//                                        temp.setFirst(c2);
//                                        temp.setFirstPosition(k);
//                                        temp.setSecond(patientRouteIndex2);
//                                        temp.setSecondPosition(p2Index);
//                                        bestChromosome = evaluateMove(temp, bestChromosome, cTemp, isInvalid);

                                        double tempCost = calSwapMoveCost(5,-1, -1, -1, -1, c2, k, patientRouteIndex2, p2Index, patient, -1, -1, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = -1;
                                            bestSecond = -1;
                                            bestThird = c2;
                                            bestFirstPosition = -1;
                                            bestSecondPosition = -1;
                                            bestThirdPosition = k;
                                            bestPatient1 = -1;
                                            bestPatient2 = otherPatient2;
                                            pos = 5;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                                System.out.println("Sub Cost s5:" + subCost);
//                                                System.out.println("best Cost s5:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    }

                                    c1Routes[c2].set(k, otherPatient2);
                                    c1Routes[patientRouteIndex2].set(p2Index, patient);
                                }
                            }
                        }
                        swapPatients(c1Routes[c1], p1Index, j);
                    }
                } else {
                    for (int j = 0; j < c1Routes[c1].size(); j++) {
                        int otherPatient1 = c1Routes[c1].get(j);
                        Patient patient1 = allPatients[otherPatient1];
                        boolean firstSwap = swapIsPossible(cTemp, patientRouteIndex2, otherPatient1, patient1, patientRouteIndex1, c1);
                        if (firstSwap) {
                            c1Routes[c1].set(j, patient);
                            c1Routes[patientRouteIndex1].set(p1Index, otherPatient1);
                            //Todo
//                            Solution temp = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
//                            temp.setFirst(c1);
//                            temp.setFirstPosition(j);
//                            temp.setSecond(patientRouteIndex1);
//                            temp.setSecondPosition(p1Index);
//                            bestChromosome = evaluateMove(temp, bestChromosome, cTemp, isInvalid);
//                            System.out.println(cTemp);
//                            System.out.println("Patient 1: "+otherPatient1);
                            double tempCost = calSwapMoveCost(6, c1, j, patientRouteIndex1, p1Index, -1, -1, -1, -1, patient, otherPatient1, j, -1, -1, cTemp, bestCost, shifts, isInvalid);
//                            System.out.println("rtbest cost: " + tempCost+ " "+cTemp);
//                            System.out.println("1 Swap cost: " + tempCost);
//                            EvaluationFunction.EvaluateFitness(cTemp);
//                            double subCost = cTemp.getFitness();
//                            System.out.println("2 Swap cost: " + subCost+ " "+cTemp);
//                            System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                    " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                    " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
//                            EvaluationFunction.Evaluate(cTemp);
//                            System.out.println("3 Swap cost: " + cTemp.getFitness());
//                            int u =0 ;
//                            for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                System.out.println("Shift idle Time "+u+ " "+shift.getIdleTime());
//                                u++;
//                            }
//                            System.exit(1);
                            if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                bestCost = tempCost;
                                bestFirst = c1;
                                bestSecond = patientRouteIndex1;
                                bestThird = -1;
                                bestFirstPosition = j;
                                bestSecondPosition = p1Index;
                                bestThirdPosition = -1;
                                bestPatient1 = otherPatient1;
                                bestPatient2 = -1;
                                pos = 6;
//                                if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                    System.out.println("Sub Cost s6:" + subCost);
//                                    System.out.println("best Cost s6:" + bestCost);
//                                    EvaluationFunction.Evaluate(cTemp);
//                                    System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                    System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                    System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                    System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                    int u =0 ;
//                                    for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                        System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                        u++;
//                                    }
//                                    System.exit(1);
//                                }
                            }
                        }

                        int c2 = caregiverPair.getSecond();
                        if (c2 == patientRouteIndex2) {
                            for (int k = 0; k < c1Routes[c2].size(); k++) {
                                int otherPatient2 = c1Routes[c2].get(k);
                                swapPatients(c1Routes[c2], p2Index, k);
                                boolean secondSwap = otherPatient2 != patient;
                                if (secondSwap) {
                                    //Todo
//                                    Solution temp1 = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
                                    int secondPosition = Math.min(p2Index, k);
                                    if (firstSwap) {
//                                        temp1.setFirst(c1);
//                                        temp1.setFirstPosition(j);
//                                        temp1.setSecond(patientRouteIndex1);
//                                        temp1.setSecondPosition(p1Index);
//                                        temp1.setThird(c2);
//                                        temp1.setThirdPosition(second);
//                                        bestChromosome = evaluateMove(temp1, bestChromosome, cTemp, isInvalid);
                                        double tempCost = calSwapMoveCost(7, c1, j, patientRouteIndex1, p1Index, c2, secondPosition, -1, -1, patient, otherPatient1, j, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = c1;
                                            bestSecond = patientRouteIndex1;
                                            bestThird = c2;
                                            bestFirstPosition = j;
                                            bestSecondPosition = p1Index;
                                            bestThirdPosition = k;
                                            bestPatient1 = otherPatient1;
                                            bestPatient2 = otherPatient2;
                                            pos = 7;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                                System.out.println("Sub Cost s7:" + subCost);
//                                                System.out.println("best Cost s7:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    } else {
//                                        temp1.setFirst(c2);
//                                        temp1.setFirstPosition(second);
//                                        bestChromosome = evaluateMove(temp1, bestChromosome, cTemp, isInvalid);
                                        double tempCost = calSwapMoveCost(3,-1, -1, -1, -1, c2, secondPosition, -1, -1, patient, -1, -1, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = -1;
                                            bestSecond = -1;
                                            bestThird = c2;
                                            bestFirstPosition = -1;
                                            bestSecondPosition = -1;
                                            bestThirdPosition = k;
                                            bestPatient1 = -1;
                                            bestPatient2 = otherPatient2;
                                            pos = 3;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                                System.out.println("Sub Cost s8:" + subCost);
//                                                System.out.println("best Cost s8:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    }
                                }
                                swapPatients(c1Routes[c2], p2Index, k);
                            }
                        } else {
                            for (int k = 0; k < c1Routes[c2].size(); k++) {
                                int otherPatient2 = c1Routes[c2].get(k);
                                Patient patient2 = allPatients[otherPatient2];
                                boolean secondSwap = doubleSwapIsPossible(cTemp, otherPatient1, patientRouteIndex1, c1, firstSwap, otherPatient2, patient2, patientRouteIndex2, c2);
                                if (secondSwap) {
//                                    System.out.println("before secondSwap isPossible"+cTemp);
                                    c1Routes[c2].set(k, patient);
                                    c1Routes[patientRouteIndex2].set(p2Index, otherPatient2);
                                    //Todo
//                                    Solution temp1 = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
                                    if (firstSwap) {
//                                        temp1.setFirst(c1);
//                                        temp1.setFirstPosition(j);
//                                        temp1.setSecond(patientRouteIndex1);
//                                        temp1.setSecondPosition(p1Index);
//                                        temp1.setThird(c2);
//                                        temp1.setThirdPosition(k);
//                                        temp1.setFourth(patientRouteIndex2);
//                                        temp1.setFourthPosition(p2Index);
//                                        bestChromosome = evaluateMove(temp1, bestChromosome, cTemp, isInvalid);
                                        double tempCost = calSwapMoveCost(8, c1, j, patientRouteIndex1, p1Index, c2, k, patientRouteIndex2, p2Index, patient, otherPatient1, j, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = c1;
                                            bestSecond = patientRouteIndex1;
                                            bestThird = c2;
                                            bestFirstPosition = j;
                                            bestSecondPosition = p1Index;
                                            bestThirdPosition = k;
                                            bestPatient1 = otherPatient1;
                                            bestPatient2 = otherPatient2;
                                            pos = 8;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
////                                                for(List<Integer> L : c1Routes){
////                                                    System.out.println("route: "+L);
////                                                }
//                                                System.out.println(cTemp);
//                                                System.out.println("Sub Cost s9:" + subCost);
//                                                System.out.println("best Cost s9:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    } else {
//                                        temp1.setFirst(c2);
//                                        temp1.setFirstPosition(k);
//                                        temp1.setSecond(patientRouteIndex2);
//                                        temp1.setSecondPosition(p2Index);
//                                        bestChromosome = evaluateMove(temp1, bestChromosome, cTemp, isInvalid);
                                        double tempCost = calSwapMoveCost(5, -1, -1, -1, -1, c2, k, patientRouteIndex2, p2Index, patient, -1, -1, otherPatient2, k, cTemp, bestCost, shifts, isInvalid);
//                                        System.out.println("best cost: " + bestCost);
//                                        System.out.println("1 Swap cost: " + tempCost);
//                                        EvaluationFunction.EvaluateFitness(cTemp);
//                                        double subCost = cTemp.getFitness();
//                                        System.out.println("2 Swap cost: " + subCost);
//                                        System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                                " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                                " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                            bestCost = tempCost;
                                            bestFirst = -1;
                                            bestSecond = -1;
                                            bestThird = c2;
                                            bestFirstPosition = -1;
                                            bestSecondPosition = -1;
                                            bestThirdPosition = k;
                                            bestPatient1 = -1;
                                            bestPatient2 = otherPatient2;
                                            pos = 5;
//                                            if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                                System.out.println("Sub Cost s10:" + subCost);
//                                                System.out.println("best Cost s10:" + bestCost);
//                                                EvaluationFunction.Evaluate(cTemp);
//                                                System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                                System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                                System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                                System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                                int u =0 ;
//                                                for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                                    System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                                    u++;
//                                                }
//                                                System.exit(1);
//                                            }
                                        }
                                    }

                                    c1Routes[c2].set(k, otherPatient2);
                                    c1Routes[patientRouteIndex2].set(p2Index, patient);
                                }
                            }
                        }
                        if (firstSwap) {
                            c1Routes[c1].set(j, otherPatient1);
                            c1Routes[patientRouteIndex1].set(p1Index, patient);
                        }
                    }
                }
            }
            if (bestCost != Double.MAX_VALUE) {
                c1Routes = cTemp.getGenes();
                if(pos == 1){
                    swapPatients(c1Routes[bestFirst], p1Index, bestFirstPosition);
                }else if(pos == 2){
                    swapPatients(c1Routes[bestFirst], p1Index, bestFirstPosition);
                    swapPatients(c1Routes[bestThird], p2Index, bestThirdPosition);
                }else if(pos == 3){
                    swapPatients(c1Routes[bestThird], p2Index, bestThirdPosition);
                }
                else if(pos == 4){
                    swapPatients(c1Routes[bestFirst], p1Index, bestFirstPosition);
                    c1Routes[bestThird].set(bestThirdPosition, patient);
                    c1Routes[patientRouteIndex2].set(p2Index, bestPatient2);
                }else if(pos == 5){
                    c1Routes[bestThird].set(bestThirdPosition, patient);
                    c1Routes[patientRouteIndex2].set(p2Index, bestPatient2);
                } else if(pos == 6){
                    c1Routes[bestFirst].set(bestFirstPosition, patient);
                    c1Routes[patientRouteIndex1].set(p1Index, bestPatient1);
                } else if(pos == 7){
                    c1Routes[bestFirst].set(bestFirstPosition, patient);
                    c1Routes[patientRouteIndex1].set(p1Index, bestPatient1);
                    swapPatients(c1Routes[bestThird], p2Index, bestThirdPosition);
                }else {
                    c1Routes[bestFirst].set(bestFirstPosition, patient);
                    c1Routes[patientRouteIndex1].set(p1Index, bestPatient1);

                    c1Routes[bestThird].set(bestThirdPosition, patient);
                    c1Routes[patientRouteIndex2].set(p2Index, bestPatient2);
                }
//                System.out.println("DOut1: "+bestCost);
//                EvaluationFunction.EvaluateFitness(cTemp);
//                System.out.println("Out2: "+cTemp.getFitness());
//                EvaluationFunction.Evaluate(cTemp);
//                System.out.println("Out3: "+cTemp.getFitness());
            }

        } else {
            List<CaregiverPair> caregiverPairs = p.getAllPossibleCaregiverCombinations();
            Set<Integer> patientRouteIndexes = cTemp.getPatientRoutes(patient);
            int patientRouteIndex = -1;
            if (patientRouteIndexes == null) {
                System.out.println("Error");
                System.out.println("Patient :" + patient);
                System.out.println(cTemp);
                System.out.println("patientRouteIndexes is null");
            }
            for (int j : patientRouteIndexes) {
                patientRouteIndex = j;
            }
            if (patientRouteIndex == -1) {
                System.out.println("No caregiver found for patient in Genes");
                System.exit(1);
            }

            int pIndex = c1Routes[patientRouteIndex].indexOf(patient);

            for (int x = 0; x < caregiverPairs.size(); x++) {
                CaregiverPair caregiverPair = caregiverPairs.get(x);
                int c1 = caregiverPair.getFirst();
                if (c1 == patientRouteIndex) {
                    for (int k = 0; k < c1Routes[c1].size(); k++) {
                        int otherPatient = c1Routes[c1].get(k);
                        if (otherPatient != patient) {
                            swapPatients(c1Routes[c1], pIndex, k);
                            //TODO
                            int firstPosition = Math.min(pIndex, k);
//                            Solution temp = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
//                            temp.setFirst(c1);
//                            temp.setFirstPosition(firstPosition);
//                            bestChromosome = evaluateMove(temp, bestChromosome, cTemp, isInvalid);
                            double tempCost = calSwapMoveCost(1, c1, firstPosition, -1, -1, -1, -1, -1, -1, patient, otherPatient, k, -1, -1, cTemp, bestCost, shifts, isInvalid);
//                            System.out.println("best cost: " + bestCost);
//                            System.out.println("1 Swap cost: " + tempCost);
//                            EvaluationFunction.EvaluateFitness(cTemp);
//                            double subCost = cTemp.getFitness();
//                            System.out.println("2 Swap cost: " + subCost);
//                            System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                    " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                    " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                            if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                bestCost = tempCost;
                                bestFirst = c1;
                                bestSecond = -1;
                                bestFirstPosition = k;
                                bestSecondPosition = pIndex;
                                bestPatient1 = otherPatient;
//                                if(Math.abs(subCost - bestCost ) > 0.001&& subCost != Double.POSITIVE_INFINITY) {
//                                    System.out.println("Sub Cost s11:" + subCost);
//                                    System.out.println("best Cost s11:" + bestCost);
//                                    EvaluationFunction.Evaluate(cTemp);
//                                    System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                    System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                    System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                    System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                    int u =0 ;
//                                    for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                        System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                        u++;
//                                    }
//                                    System.exit(1);
//                                }
                            }
                            swapPatients(c1Routes[c1], pIndex, k);
                        }
                    }
                } else {
                    for (int k = 0; k < c1Routes[c1].size(); k++) {
                        int otherPatient = c1Routes[c1].get(k);
                        Patient patient2 = allPatients[otherPatient];
                        if (swapIsPossible(cTemp, -1, otherPatient, patient2, patientRouteIndex, c1)) {
                            c1Routes[c1].set(k, patient);
                            c1Routes[patientRouteIndex].set(pIndex, otherPatient);
                            //TODO
//                            Solution temp = isInvalid ? new Solution(c1Routes, 0.0, true) : new Solution(c1Routes, true);
//                            temp.setFirst(c1);
//                            temp.setFirstPosition(k);
//                            temp.setSecond(patientRouteIndex);
//                            temp.setSecondPosition(pIndex);
//                            bestChromosome = evaluateMove(temp, bestChromosome, cTemp, isInvalid);

                            double tempCost = calSwapMoveCost(6,c1, k, patientRouteIndex, pIndex, -1, -1, -1, -1, patient, otherPatient, k, -1, -1, cTemp, bestCost, shifts, isInvalid);
//                            System.out.println("best cost: " + bestCost);
//                            System.out.println("1 Swap cost: " + tempCost);
//                            EvaluationFunction.EvaluateFitness(cTemp);
//                            double subCost = cTemp.getFitness();
//                            System.out.println("2 Swap cost: " + subCost);
//                            System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                                    " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                                    " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
                            if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                bestCost = tempCost;
                                bestFirst = c1;
                                bestSecond = patientRouteIndex;
                                bestFirstPosition = k;
                                bestSecondPosition = pIndex;
                                bestPatient1 = otherPatient;

//                                if(Math.abs(subCost - bestCost ) > 0.001 && subCost != Double.POSITIVE_INFINITY) {
//                                    System.out.println("Sub Cost s12:" + subCost);
//                                    System.out.println("best Cost s12:" + bestCost);
//                                    EvaluationFunction.Evaluate(cTemp);
//                                    System.out.println("3 Swap cost: " + cTemp.getFitness());
//                                    System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                                    System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                                    System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                                    int u =0 ;
//                                    for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                        System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                        u++;
//                                    }
//                                    System.exit(1);
//                                }
                            }

                            c1Routes[c1].set(k, otherPatient);
                            c1Routes[patientRouteIndex].set(pIndex, patient);
                        }
                    }
                }
            }
            if (bestCost != Double.MAX_VALUE) {
                c1Routes = cTemp.getGenes();
                if (bestSecond != -1) {
                    c1Routes[bestFirst].set(bestFirstPosition, patient);
                    c1Routes[bestSecond].set(bestSecondPosition, bestPatient1);
                } else {
                    swapPatients(c1Routes[bestFirst], pIndex, bestFirstPosition);
                }
//                System.out.println("SOut1: "+bestCost);
                EvaluationFunction.EvaluateFitness(cTemp);
//                System.out.println("Out2: "+cTemp.getFitness());
//                EvaluationFunction.Evaluate(cTemp);
//                System.out.println("Out3: "+cTemp.getFitness());
            }
        }

        cTemp.setMoves(selectedPatient);
        return cTemp;
    }

    public static void swapPatients(List<Integer> route, int p1Index, int p2Index) {
        int p1 = route.get(p1Index);
        route.set(p1Index, route.get(p2Index));
        route.set(p2Index, p1);
    }

    private boolean swapIsPossible(Solution cTemp, int otherRouteIndex, int patient1Index, Patient patient1, int routeIndex, int otherPatientRouteIndex1) {
        if(otherRouteIndex == otherPatientRouteIndex1) {
            return false;
        }
        if (patient1.getRequired_caregivers().length > 1) {
            Set<Integer> routeIndexesOfOtherPatient = cTemp.getPatientRoutes(patient1Index);
            int otherPatientRouteIndex2 = -1;
            for (int r : routeIndexesOfOtherPatient) {
                if (r != otherPatientRouteIndex1) {
                    otherPatientRouteIndex2 = r;
                    break;
                }
            }
            if (otherPatientRouteIndex2 == -1 || routeIndex == otherPatientRouteIndex2) {
                return false;
            }
            Set<Integer> possibleFirstRoute = patient1.getPossibleFirstCaregiver();
            Set<Integer> possibleSecondRoute = patient1.getPossibleSecondCaregiver();
            return possibleFirstRoute.contains(otherPatientRouteIndex2) && possibleSecondRoute.contains(routeIndex)
                    || possibleFirstRoute.contains(routeIndex) && possibleSecondRoute.contains(otherPatientRouteIndex2);

        } else {
            return patient1.getPossibleFirstCaregiver().contains(routeIndex);
        }
    }

    private boolean doubleSwapIsPossible(Solution cTemp, int otherPatient1, int patientRouteIndex1, int c1, boolean firstSwap,
                                         int otherPatient2, Patient patient2, int patientRouteIndex2, int c2) {
        if (patient2.getRequired_caregivers().length > 1) {
            Set<Integer> routeIndexesOfOtherPatient2 = cTemp.getPatientRoutes(otherPatient2);
            int otherPatient2RouteIndex2 = -1;
            for (int r : routeIndexesOfOtherPatient2) {
                if (r != c2) {
                    otherPatient2RouteIndex2 = r;
                    break;
                }
            }
            if (otherPatient2RouteIndex2 == -1) {
                return false;
            }
            Set<Integer> possibleFirstRoute = patient2.getPossibleFirstCaregiver();
            Set<Integer> possibleSecondRoute = patient2.getPossibleSecondCaregiver();
            if (otherPatient1 == otherPatient2) {
                if (firstSwap) {
                    return possibleFirstRoute.contains(patientRouteIndex1) && possibleSecondRoute.contains(patientRouteIndex2)
                            || possibleFirstRoute.contains(patientRouteIndex2) && possibleSecondRoute.contains(patientRouteIndex1);
                } else {
                    if (c1 == patientRouteIndex2)
                        return false;
                    return possibleFirstRoute.contains(c1) && possibleSecondRoute.contains(patientRouteIndex2)
                            || possibleFirstRoute.contains(patientRouteIndex2) && possibleSecondRoute.contains(c1);
                }
            } else {
                if (patientRouteIndex2 == otherPatient2RouteIndex2) {
                    return false;
                }
                return possibleFirstRoute.contains(otherPatient2RouteIndex2) && possibleSecondRoute.contains(patientRouteIndex2)
                        || possibleFirstRoute.contains(patientRouteIndex2) && possibleSecondRoute.contains(otherPatient2RouteIndex2);
            }
        } else {
            return patient2.getPossibleFirstCaregiver().contains(patientRouteIndex2);
        }
    }

    private double calSwapMoveCost(int pos, int first, int m, int second, int n, int third, int x, int fourth,
                                   int y, int patient, int patient1, int patient1Position, int patient2,
                                   int patient2Position, Solution cTemp, double bestCost,
                                   Shift[] shifts, boolean isInvalid) {
        if (isInvalid) {
            EvaluationFunction.EvaluateFitness(cTemp);
//            System.out.println("Invalid solution");
//            System.out.println("Total Distance: "+ cTemp.getTotalTravelCost() + " Total tardiness: "+ cTemp.getTotalTardiness() +
//                    " Highest Tardiness: "+ cTemp.getHighestTardiness()+ " Total Waiting: "+ cTemp.getTotalWaitingTime()+
//                    " Total overtime: "+ cTemp.getOvertime()+ " Highest Idle time: "+ cTemp.getHighestIdleTime());
            return cTemp.getFitness();
        }
        Arrays.fill(routeEndPoint, -1);

        int size;
        int[] routeMove;
        int[] positionMove;
//        System.out.println("pos: "+pos);
        if(pos == 1){
            size = 1;
            routeEndPoint[first] = m;

            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = first;
            positionMove[0] = m;
//            for(int i= 0; i< routeMove.length; i++){
//                System.out.println(routeMove[i]+ " + "+ positionMove[i]);
//            }

        }else if(pos == 2){
            size = 2;
            routeEndPoint[first] = m;
            routeEndPoint[third] = x;

            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = first;
            positionMove[0] = m;
            routeMove[1] = third;
            positionMove[1] = x;
        }else if(pos == 3){
            size = 1;
            routeEndPoint[third] = x;

            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = third;
            positionMove[0] = x;
        }
        else  if(pos == 4){
            size = 3;
            routeEndPoint[first] = m;
//            routeEndPoint[third] = x;
//            routeEndPoint[fourth] = y;
            routeEndPoint[third] = routeEndPoint[third] > -1 && routeEndPoint[third] < x? routeEndPoint[third] : x;
            routeEndPoint[fourth] = routeEndPoint[fourth] > -1 && routeEndPoint[fourth]< y? routeEndPoint[fourth]: y;



            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = first;
            positionMove[0] = m;
            routeMove[1] = third;
            positionMove[1] = x;
            routeMove[2] = fourth;
            positionMove[2] = y;
        }else if(pos == 5){
            size = 2;
            routeEndPoint[third] = x;
            routeEndPoint[fourth] = y;

            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = third;
            positionMove[0] = x;
            routeMove[1] = fourth;
            positionMove[1] = y;
        } else if(pos == 6){
            size = 2;
            routeEndPoint[first] = m;
            routeEndPoint[second] = n;

            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = first;
            positionMove[0] = m;
            routeMove[1] = second;
            positionMove[1] = n;
//            for(int i= 0; i< routeMove.length; i++){
//                System.out.println(routeMove[i]+ " + "+ positionMove[i]);
//            }
        } else if(pos == 7){
            size = 3;
            routeEndPoint[first] = m;
//            routeEndPoint[second] = n;
//            routeEndPoint[third] = x;
            routeEndPoint[second] = routeEndPoint[second] > -1 && routeEndPoint[second] < n? routeEndPoint[second] : n;
            routeEndPoint[third] = routeEndPoint[third] > -1 && routeEndPoint[third] < x? routeEndPoint[third] : x;

            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = first;
            positionMove[0] = m;
            routeMove[1] = second;
            positionMove[1] = n;
            routeMove[2] = third;
            positionMove[2] = x;
        }else {
//            System.out.println("Patient: "+patient);
//            System.out.println("Patient position: "+n+" "+y);
//            System.out.println("Patient1: "+patient1);
//            System.out.println("Patient1 position: "+patient1Position);
//            System.out.println("Patient2: "+patient2);
//            System.out.println("Patient2 position: "+patient2Position);
//            System.out.println("route1: "+ first +" m: "+m);
//            System.out.println("route2: "+ second+ " n: "+n);
//            System.out.println("route3: "+ third+ " x: "+x);
//            System.out.println("route4: "+ fourth+" y: "+ y);
            size = 4;
            routeEndPoint[first] = m;
            routeEndPoint[second] = routeEndPoint[second] > -1 && routeEndPoint[second] < n? routeEndPoint[second] : n;
            routeEndPoint[third] = routeEndPoint[third] > -1 && routeEndPoint[third] < x? routeEndPoint[third] : x;
            routeEndPoint[fourth] = routeEndPoint[fourth] > -1 && routeEndPoint[fourth]< y? routeEndPoint[fourth]: y;

//            for (int i =0; i< routeEndPoint.length; i++){
//                System.out.print("route: "+i+" "+routeEndPoint[i] + " | ");
//            }
//            System.out.println();

            routeMove = new int[size];
            positionMove = new int[size];
            routeMove[0] = first;
            positionMove[0] = m;
            routeMove[1] = second;
            positionMove[1] = n;
            routeMove[2] = third;
            positionMove[2] = x;
            routeMove[3] = fourth;
            positionMove[3] = y;
        }


        removeAffectedPatientSwap(routeMove, positionMove, cTemp, p1, routeEndPoint);

//        if(pos == 8) {
//            System.out.println("after removal");
//            for (int i = 0; i < routeEndPoint.length; i++) {
//                System.out.print("route: " + i + " " + routeEndPoint[i] + " | ");
//            }
//            System.out.println();
//        }

        double totalTravelCost = 0.0;
        double highestIdleTime = 0.0;
        Arrays.fill(highestAndTotalTardiness, 0);
        double[] testDistance = new double[numOfCaregivers];
        for (int i = 0; i < routeEndPoint.length; i++) {
            Shift shift = shifts[i];
            List<Double> currentTime = shift.getCurrentTime();
            List<Double> travelCost = shift.getTravelCost();
            List<Double> tardiness = shift.getTardiness();
            List<Double> maxTardiness = shift.getMaxTardiness();

            if (routeEndPoint[i] != -1) {
                int index = routeEndPoint[i];
                totalTravelCost += travelCost.get(index);
                testDistance[i] = travelCost.get(index);
                routesCurrentTime[i] = currentTime.get(index);
                highestAndTotalTardiness[0] = Math.max(maxTardiness.get(index), highestAndTotalTardiness[0]);
                highestAndTotalTardiness[1] += tardiness.get(index);
//                System.out.println("o Shift Distance: "+ i + " " + travelCost);
//                System.out.println("t Shift Distance: "+ i + " " + testDistance[i]);
            } else {
                highestAndTotalTardiness[0] = Math.max(maxTardiness.get(maxTardiness.size() - 1), highestAndTotalTardiness[0]);
                highestAndTotalTardiness[1] += tardiness.get(tardiness.size() - 1);
                highestIdleTime = Math.max(shift.getIdleTime(), highestIdleTime);
                totalTravelCost += travelCost.get(travelCost.size() - 1);
//                System.out.println("a Shift: "+ i + " " + shift.getIdleTime() + " " + highestIdleTime);
//                System.out.println("a Shift Distance: "+ i + " " + travelCost.get(travelCost.size() - 1));
            }
//            if (i == first || i == second) {
//                int index = routeEndPoint[i];
//                totalTravelCost += travelCost.get(index);
//                testDistance[i] = travelCost.get(index);
//            } else {
//                totalTravelCost += travelCost.get(travelCost.size() - 1);
//                System.out.println("a Shift Distance: "+ i + " " + travelCost.get(travelCost.size() - 1));
//            }
        }


        //Distance calculation
        List<Integer>[] genes = cTemp.getGenes();

        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            int routeStartPoint = 0;
            if (routeEnd != -1) {
                for (int j = routeEnd; j <= route.size(); j++) {
                    if (j == 0) {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        totalTravelCost += distances[routeStartPoint][nextIndex];
                        testDistance[i] += distances[routeStartPoint][nextIndex];
                    } else if (j == route.size()) {
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        totalTravelCost += distances[prevIndex][routeStartPoint];
                        testDistance[i] += distances[prevIndex][routeStartPoint];
                    } else {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        totalTravelCost += distances[prevIndex][nextIndex];
                        testDistance[i] += distances[prevIndex][nextIndex];
                    }
                }
//                System.out.println("c Shift Distance: " + i + " " + testDistance[i]);
            }
        }

        double solutionCost = 1/ 3d * totalTravelCost + 1/3d * highestAndTotalTardiness[0] + 1/3d *highestAndTotalTardiness[1];
        if (solutionCost > bestCost) {
//            if(pos==8){
//                System.out.println("1Travel: "+totalTravelCost);
//                System.out.println("Tardiness: "+highestAndTotalTardiness[1]);
//                System.out.println("Highest Tardiness: "+highestAndTotalTardiness[0]);
//
//            }
            return solutionCost;
        }


        track.clear();
        //Tardiness calculation
        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            if (routeEnd != -1) {
                int routeStartingPoint = 0;
                for (int j = routeEnd; j < route.size(); j++) {
                    int current = j == 0 ? routeStartingPoint : route.get(j - 1) + numOfDepartingPoints;
                    solutionCost = patientIsAssigned(genes, i, current, route.get(j), totalTravelCost, routesCurrentTime, highestAndTotalTardiness, routeEndPoint, track);
                    if (solutionCost == Double.POSITIVE_INFINITY || solutionCost > bestCost) {
//                        if(pos==8){
//                            System.out.println("2Travel: "+totalTravelCost);
//                            System.out.println("Tardiness: "+highestAndTotalTardiness[1]);
//                            System.out.println("Highest Tardiness: "+highestAndTotalTardiness[0]);
//
//                        }
                        return solutionCost;
                    }
                    track.clear();
                }
            }
        }

        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            if (routeEnd != -1) {
                int routeStartingPoint = 0;
                int lastPatient = route.get(route.size() - 1) + numOfDepartingPoints;
                double distance = distances[lastPatient][routeStartingPoint];
                routesCurrentTime[i] += distance;
            }
        }

//        System.out.println("+Total Distance: "+ totalTravelCost + " Total tardiness: "+ highestAndTotalTardiness[1] +
//                " Highest Tardiness: "+ highestAndTotalTardiness[0] + " Total Waiting: "+ totalWaitingTime[numOfCaregivers] +
//                " Total overtime: "+ overallOvertime+ " Highest Idle time: "+ highestIdleTime);
//        if(pos==8){
//            System.out.println("3Travel: "+totalTravelCost);
//            System.out.println("Tardiness: "+highestAndTotalTardiness[1]);
//            System.out.println("Highest Tardiness: "+highestAndTotalTardiness[0]);
//
//        }
        return 1/3d * totalTravelCost + 1/3d *highestAndTotalTardiness[0] + 1/3d * highestAndTotalTardiness[1];
    }

    public static void removeAffectedPatientSwap(int[] routeMove, int[] positionMove, Solution base, Solution p1, int[] routeEndPoint) {
        Map<Integer, Set<Integer>> patientToRoutesMap = base.getPatientToRoutesMap();
        List<Integer>[] genes = p1 .getGenes();
//        for(int i= 0; i< routeMove.length; i++){
//            System.out.println(routeMove[i]+ " * "+ positionMove[i]);
//        }
        for (int j = 0; j < routeMove.length; j++) {
            int first = routeMove[j];
            int firstPosition = positionMove[j];
//            System.out.println(routeMove[j]+ " = "+ positionMove[j]);
//            System.out.println(" ======= ");
            List<Integer> currentRoute = genes[first];
//            System.out.println("currentRoute: "+currentRoute);
            for (int i = firstPosition; i < currentRoute.size(); i++) {
                int patient = currentRoute.get(i);
//                if (patient == iPatient) {
//                    continue;
//                }
                Patient p = allPatients[patient];
                if (p.getRequired_caregivers().length > 1) {
                    int routeIndex = getRouteIndexMethod(first, patientToRoutesMap.get(patient));
//                    System.out.println("Patient: "+ patient+ " routeIndex: "+routeIndex);
                    int patientIndex = genes[routeIndex].indexOf(patient);
//                    System.out.println("rouyer: "+genes[routeIndex]);
                    if (routeEndPoint[routeIndex] == -1 || routeEndPoint[routeIndex] > patientIndex) {
                        routeEndPoint[routeIndex] = patientIndex;
                        int[] newRouteMove = {routeIndex};
                        int[] newPositionMove = {patientIndex};
//                        System.out.println("Patient: "+ patientIndex+ " routeIndex: "+routeIndex);
//                        System.out.println("New route and position");
//                        for(int t= 0; t< newRouteMove.length; t++){
//                            System.out.println(newRouteMove[t]+ " %% "+ newPositionMove[t]);
//                        }
                        removeAffectedPatientSwap(newRouteMove, newPositionMove, base, p1, routeEndPoint);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        tabu.getRelocateSolutions().add(Start());
    }
}
