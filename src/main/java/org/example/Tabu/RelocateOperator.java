package org.example.Tabu;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;
import org.example.Main;

import java.util.*;

import static org.example.Tabu.Tabu.conflictCheck;
import static org.example.Tabu.EvaluationFunction.patientIsAssigned;

public class RelocateOperator implements Runnable {
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
    private final double[] highestAndTotalTardiness;;
    private final int selectedPatient;
    private final Set<Integer> track;

    public RelocateOperator(Tabu tabu, int selectedPatient, Solution p1, Random rand) {
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
//        System.out.println("Selected Patient: "+selectedPatient);
        // Initialize variables
        List<Integer>[] p1Routes = p1.getGenes();
        List<Integer>[] c1Routes = new ArrayList[p1Routes.length];

        //Remove patients of selected route from parents
        Patient p = allPatients[selectedPatient];
        int size = p.getRequired_caregivers().length;
        int[] routePostion = new int[size];
        int[] patientPostion = new int[size];
        int counter = 0;

        int move = -1;
        for (int i = 0; i < p1Routes.length; i++) {
            List<Integer> route = new ArrayList<>(p1Routes[i].size());
            for (int j = 0; j < p1Routes[i].size(); j++) {
                int patient = p1Routes[i].get(j);
                if (selectedPatient != patient) {
                    route.add(patient);
                } else {
                    move = patient;
                    routePostion[counter] = i;
                    patientPostion[counter] = j;
                    counter++;
                }
            }
            c1Routes[i] = route;
        }

        Solution cTemp = new Solution(c1Routes, 0.0, false);
        EvaluationFunction.Evaluate(cTemp);

        double bestCost = Double.MAX_VALUE;
        int bestFirst = -1;
        int bestSecond = -1;
        int bestM = -1;
        int bestN = -1;
        cTemp.buildPatientRouteMap();
        int patient = selectedPatient;
        Shift[] shifts = cTemp.getCaregiversRouteUp();
        boolean isInvalid = cTemp.getFitness() == Double.POSITIVE_INFINITY;
        List<CaregiverPair> caregiverPairs = p.getAllPossibleCaregiverCombinations();
        if (p.getRequired_caregivers().length > 1) {
            for (CaregiverPair caregiverPair : caregiverPairs) {
                int first = caregiverPair.getFirst();
                int second = caregiverPair.getSecond();
                int firstSize = c1Routes[first].size();
                int secondSize = c1Routes[second].size();
                c1Routes[first].add(0, patient);
                for (int m = 0; m <= firstSize; m++) {
                    if (m > 0) {
                        int otherPatient = c1Routes[first].get(m);
                        c1Routes[first].set(m, patient);
                        c1Routes[first].set(m - 1, otherPatient);
                    }
                    c1Routes[second].add(0, patient);
                    for (int n = 0; n <= secondSize; n++) {
                        if (n > 0) {
                            int otherPatient = c1Routes[second].get(n);
                            c1Routes[second].set(n, patient);
                            c1Routes[second].set(n - 1, otherPatient);
                        }

                        if(first == routePostion[0] && m == patientPostion[0] && second == routePostion[1] && n == patientPostion[1] ||
                                first == routePostion[1] && m == patientPostion[1] && second == routePostion[0] && n == patientPostion[0]) {
                            continue;
                        }
                        if (noEvaluationConflicts(c1Routes[first], c1Routes[second], m, n)) {
                            double tempCost = calMoveCost(first, m, second, n, patient, cTemp, bestCost, shifts, isInvalid);
//                            EvaluationFunction.EvaluateFitness(cTemp);
//                            double subCost = cTemp.getFitness();
                            if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
//                            if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY ) {
                                bestCost = tempCost;
                                bestFirst = first;
                                bestSecond = second;
                                bestM = m;
                                bestN = n;
//                                if(Math.abs(subCost - bestCost)> 0.001 && subCost != Double.POSITIVE_INFINITY) {
//                                    System.out.println("Sub Cost :" + subCost);
//                                    System.out.println(subCost+" != "+Double.POSITIVE_INFINITY);
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
                    }
                    c1Routes[second].remove(Integer.valueOf(patient));
                }
                c1Routes[first].remove(Integer.valueOf(patient));
            }
            if (bestCost != Double.MAX_VALUE) {
                c1Routes = cTemp.getGenes();
                c1Routes[bestFirst].add(bestM, patient);
                c1Routes[bestSecond].add(bestN, patient);
                EvaluationFunction.Evaluate(cTemp);
            } else {
                System.out.println("no route found");
            }
        } else {
            for (CaregiverPair caregiverPair : caregiverPairs) {
                int first = caregiverPair.getFirst();
                int firstSize = c1Routes[first].size();
                c1Routes[first].add(0, patient);
                for (int k = 0; k <= firstSize; k++) {
                    if (k > 0) {
                        int otherPatient = c1Routes[first].get(k);
                        c1Routes[first].set(k, patient);
                        c1Routes[first].set(k - 1, otherPatient);
                    }
                    if(first == routePostion[0] && k == patientPostion[0]) {
                        continue;
                    }
                    double tempCost = calMoveCost(first, k, -1, -1, patient, cTemp, bestCost, shifts, isInvalid);
//                    EvaluationFunction.EvaluateFitness(cTemp);
//                    double subCost = cTemp.getFitness();
//                    System.out.println(subCost+" > "+tempCost);
//                    if(Math.abs(subCost - tempCost)> 0.001) {
//                        System.out.println("Yzzzz");
//                        System.out.println("Sub Cost :" + subCost);
//                        System.out.println("Travel: " + cTemp.getTotalTravelCost());
//                        System.out.println("Tardiness: " + cTemp.getTotalTardiness());
//                        System.out.println("Highest Tardiness: " + cTemp.getHighestTardiness());
//                        System.exit(1);
//                    }
                    if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
//                    if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY ) {
                        bestCost = tempCost;
                        bestFirst = first;
                        bestM = k;
//                        if(Math.abs(subCost - bestCost)> 0.001 && subCost != Double.POSITIVE_INFINITY) {
//                            System.out.println(subCost>bestCost);
//                            System.out.println(subCost+" > "+bestCost);
//                            System.out.println("Temp Cost :" + bestCost);
//                            System.out.println("Sub Cost :" + subCost);
//                            EvaluationFunction.Evaluate(cTemp);
//                            System.out.println("3 Swap cost: " + cTemp.getFitness());
//                            System.out.println("Travel: "+cTemp.getTotalTravelCost());
//                            System.out.println("Tardiness: "+cTemp.getTotalTardiness());
//                            System.out.println("Highest Tardiness: "+cTemp.getHighestTardiness());
//                            int u =0 ;
//                            for (Shift shift : cTemp.getCaregiversRouteUp()) {
//                                System.out.println("Shift Travel Cost "+u+ " "+shift.getTravelCost());
//                                u++;
//                            }
//                            System.exit(1);
//                        }
                    }
//                    System.out.println("Route: "+first+" Position: "+k +" Fitness: "+tempCost);
                }
                c1Routes[first].remove(Integer.valueOf(patient));
            }
            if (bestCost != Double.MAX_VALUE) {
                c1Routes = cTemp.getGenes();
                c1Routes[bestFirst].add(bestM, patient);
                EvaluationFunction.EvaluateFitness(cTemp);
            } else {
                System.out.println("no route found " + caregiverPairs.size());
            }
        }

        cTemp.setMoves(move);
        return cTemp;
    }

    private double calMoveCost(int first, int m, int second, int n, int patient, Solution cTemp, double bestCost, Shift[] shifts, boolean isInvalid) {
        if (isInvalid) {
            List<Integer>[] c1Routes = cTemp.getGenes();
            Solution temp = new Solution(c1Routes, 0.0, false);
            EvaluationFunction.EvaluateFitness(temp);
//            System.out.println("1Travel: "+temp.getTotalTravelCost());
//            System.out.println("Tardiness: "+temp.getTotalTardiness());
//            System.out.println("Highest Tardiness: "+temp.getHighestTardiness());

            return temp.getFitness();
        }
        Arrays.fill(routeEndPoint, -1);
        int size = 1;
        routeEndPoint[first] = m;
        if (second != -1) {
            size++;
            routeEndPoint[second] = n;
        }

        int[] routeMove = new int[size];
        int[] positionMove = new int[size];
        routeMove[0] = first;
        positionMove[0] = m;
        if (size > 1) {
            routeMove[1] = second;
            positionMove[1] = n;
        }

        removeAffectedPatient(patient, routeMove, positionMove, cTemp, routeEndPoint);

        double totalTravelCost = 0.0;
        Arrays.fill(highestAndTotalTardiness, 0);
        for (int i = 0; i < routeEndPoint.length; i++) {
            Shift shift = shifts[i];
            List<Double> currentTime = shift.getCurrentTime();
            List<Double> travelCost = shift.getTravelCost();
            List<Double> tardiness = shift.getTardiness();
            List<Double> maxTardiness = shift.getMaxTardiness();

            if (routeEndPoint[i] != -1) {
                int index = routeEndPoint[i];
                routesCurrentTime[i] = currentTime.get(index);
                highestAndTotalTardiness[0] = Math.max(maxTardiness.get(index), highestAndTotalTardiness[0]);
                highestAndTotalTardiness[1] += tardiness.get(index);
            } else {
                highestAndTotalTardiness[0] = Math.max(maxTardiness.get(maxTardiness.size() - 1), highestAndTotalTardiness[0]);
                highestAndTotalTardiness[1] += tardiness.get(tardiness.size() - 1);

            }
            if (i == first || i == second) {
                int index = routeEndPoint[i];
                totalTravelCost += travelCost.get(index);
            } else {
                totalTravelCost += travelCost.get(travelCost.size() - 1);
            }
        }


        //Distance calculation
        List<Integer>[] genes = cTemp.getGenes();
        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            int routeStartPoint = 0;
            if (i == first || i == second) {
                for (int j = routeEnd; j <= route.size(); j++) {
                    if (j == 0) {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        totalTravelCost += distances[routeStartPoint][nextIndex];
                    } else if (j == route.size()) {
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        totalTravelCost += distances[prevIndex][routeStartPoint];
                    } else {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        totalTravelCost += distances[prevIndex][nextIndex];
                    }

                }
            }
        }

        double solutionCost = (1/3d * totalTravelCost) + (1/3d * highestAndTotalTardiness[0]) + (1/3d * highestAndTotalTardiness[1]);
        if (solutionCost > bestCost) {
//            System.out.println("2Travel: "+totalTravelCost);
//            System.out.println("Tardiness: "+highestAndTotalTardiness[1]);
//            System.out.println("Highest Tardiness: "+highestAndTotalTardiness[0]);

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
//                        System.out.println("3Travel: "+totalTravelCost);
//                        System.out.println("Tardiness: "+highestAndTotalTardiness[1]);
//                        System.out.println("Highest Tardiness: "+highestAndTotalTardiness[0]);
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

//        System.out.println("4Travel: "+totalTravelCost);
//        System.out.println("Tardiness: "+highestAndTotalTardiness[1]);
//        System.out.println("Highest Tardiness: "+highestAndTotalTardiness[0]);
        return (1/3d * totalTravelCost) + (1/3d * highestAndTotalTardiness[0]) + (1/3d * highestAndTotalTardiness[1]);
    }

    private boolean noEvaluationConflicts(List<Integer> c1Route, List<Integer> c2Route, int m, int n) {
        return conflictCheck(c1Route, c2Route, m, n);
    }

    public static void removeAffectedPatient(int iPatient, int[] routeMove, int[] positionMove, Solution base, int[] routeEndPoint) {
        Map<Integer, Set<Integer>> patientToRoutesMap = base.getPatientToRoutesMap();
        List<Integer>[] genes = base.getGenes();
        for (int j = 0; j < routeMove.length; j++) {
            int first = routeMove[j];
            int firstPosition = positionMove[j];
            List<Integer> currentRoute = genes[first];
            for (int i = firstPosition; i < currentRoute.size(); i++) {
                int patient = currentRoute.get(i);
                if (patient == iPatient) {
                    continue;
                }
                Patient p = allPatients[patient];
                if (p.getRequired_caregivers().length > 1) {
                    int routeIndex = getRouteIndexMethod(first, patientToRoutesMap.get(patient));
                    int patientIndex = genes[routeIndex].indexOf(patient);
                    if (routeEndPoint[routeIndex] == -1 || routeEndPoint[routeIndex] > patientIndex) {
                        routeEndPoint[routeIndex] = patientIndex;
                        int[] newRouteMove = {routeIndex};
                        int[] newPositionMove = {patientIndex};
                        removeAffectedPatient(iPatient, newRouteMove, newPositionMove, base, routeEndPoint);
                    }
                }
            }
        }
    }

    static int getRouteIndexMethod(int first, Set<Integer> routes) {
        if (routes == null) return -1; // Patient not found
        for (int route : routes) {
            if (route != first) {
                return route; // Return the first alternative route
            }
        }
        return -1; // other route not found
    }

    @Override
    public void run() {
        tabu.getRelocateSolutions().add(Start());
    }
}