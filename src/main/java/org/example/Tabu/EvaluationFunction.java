package org.example.Tabu;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;
import org.example.Data.Required_Caregiver;
import org.example.Main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EvaluationFunction {
    private static final InstancesClass dataset = Main.instance;
    private static final double[][] distances = dataset.getDistances();
    private static final Patient[] allPatients = dataset.getPatients();
    private static final Caregiver[] allCaregivers = dataset.getCaregivers();
    private static final ThreadLocal<Set<Integer>> trackHolder = ThreadLocal.withInitial(() -> new HashSet<>(150));
    private final static ThreadLocal<double[]> routeStartTime = ThreadLocal.withInitial(() -> new double[allCaregivers.length]);

    public static void Evaluate(Solution ch) {
        Shift[] routes = ch.getCaregiversRouteUp();
        if (routes[0] == null) {
            ch.initializeCaregiversRoute();
        }
        for (int i = 0; i < routes.length; i++) {

            routes[i].resetShift();
        }
        ch.setCaregiversRouteUp(routes);
        ch.setHighestTardiness(0);
        ch.setTotalTardiness(0);
        ch.setTotalTravelCost(0);
        ch.setFitness(Double.POSITIVE_INFINITY);

        Set<Integer> track = trackHolder.get(); // Initial capacity for small patient sets
        List<Integer>[] genes = ch.getGenes();

        try {
            for (int i = 0; i < routes.length; i++) {
                List<Integer> route = genes[i];
                Shift caregiver1 = routes[i];

                for (int patient : route) {
                    if (!caregiver1.getRoute().contains(patient)) {
                        if (patientAssignment(ch, patient, caregiver1, routes, i, track)) {
                            ch.setFitness(Double.POSITIVE_INFINITY);
                            return;
                        }
                        track.clear();
                    }
                }
            }
        } finally {
            track.clear();
        }

        // Final cost calculations
        for (Shift s : routes) {
            int lastLocationId = s.getRoute().isEmpty() ? 0 : s.getRoute().get(s.getRoute().size() - 1) + 1;
            double returnCost = distances[lastLocationId][0];
            s.updateCurrentTime(returnCost);
            ch.updateTotalTravelCost(returnCost);
            s.updateTravelCost(returnCost);
        }

        UpdateCost(ch);
    }


    public static boolean patientAssignment(Solution ch, int patient, Shift caregiver1, Shift[] routes, int i, Set<Integer> track) {
        Patient p = allPatients[patient];
        double[] timeWindow = p.getTime_window();
        int currentLocation1 = caregiver1.getRoute().isEmpty() ? 0 : allPatients[caregiver1.getRoute().get(caregiver1.getRoute().size() - 1)].getDistance_matrix_index();
        int nextLocation = p.getDistance_matrix_index();
        double patientOpenTimeWindow = timeWindow[0];
        double patientCloseTimeWindow = timeWindow[1];
        double arrivalTime1 = caregiver1.getCurrentTime().get(caregiver1.getCurrentTime().size() - 1) + distances[currentLocation1][nextLocation];
        double startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);

        if (p.getRequired_caregivers().length > 1) {
            if (!track.add(patient)) { // Combined contains check and add
                return true;
            }

            int index = findSecondCaregiver(patient, i, routes, ch, track);
            if (index > allCaregivers.length - 1) {
                return true;
            }

            Shift caregiver2 = routes[index];
            Required_Caregiver[] requiredCaregivers = p.getRequired_caregivers();
            int secondDepot = 0;

            // Check and potentially swap caregivers based on qualifications
            if (shouldSwapCaregivers(p, caregiver1, caregiver2)) {
                Shift temp = caregiver1;
                caregiver1 = caregiver2;
                caregiver2 = temp;

                // Recalculate after swap
                currentLocation1 = caregiver1.getRoute().isEmpty() ? 0 : allPatients[caregiver1.getRoute().get(caregiver1.getRoute().size() - 1)].getDistance_matrix_index();
                arrivalTime1 = caregiver1.getCurrentTime().get(caregiver1.getCurrentTime().size() - 1) + distances[currentLocation1][nextLocation];
                startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);
            }

            int currentLocation2 = caregiver2.getRoute().isEmpty() ? secondDepot : allPatients[caregiver2.getRoute().get(caregiver2.getRoute().size() - 1)].getDistance_matrix_index();
            double arrivalTime2 = caregiver2.getCurrentTime().get(caregiver2.getCurrentTime().size() - 1) + distances[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, patientOpenTimeWindow);

            processSynchronization(ch, p, caregiver1, caregiver2, requiredCaregivers,
                    startTime1, startTime2, patientCloseTimeWindow);

            double travelCost = distances[currentLocation1][nextLocation] +
                    distances[currentLocation2][nextLocation];
            updateCaregiverRoutes(ch, caregiver1, caregiver2, patient,
                    distances[currentLocation1][nextLocation],
                    distances[currentLocation2][nextLocation],
                    travelCost);
        } else {
            processSingleCaregiver(ch, caregiver1, p, patient, startTime1, patientCloseTimeWindow,
                    distances[currentLocation1][nextLocation]);
        }
        return false;
    }

    private static void updateCaregiverRoutes(Solution ch, Shift caregiver1,
                                              Shift caregiver2, int patientId, double cost1, double cost2, double totalCost) {
        ch.updateTotalTravelCost(totalCost);
        caregiver1.updateRoute(patientId);
        caregiver1.updateTravelCost(cost1);
        caregiver2.updateRoute(patientId);
        caregiver2.updateTravelCost(cost2);
    }

    private static void processSingleCaregiver(Solution ch, Shift caregiver,
                                               Patient p, int patient, double startTime, double timeWindowEnd, double travelCost) {
        double tardiness = Math.max(0, startTime - timeWindowEnd);

        ch.setHighestTardiness(Math.max(tardiness, ch.getHighestTardiness()));
        ch.updateTotalTardiness(tardiness);
        ch.updateTotalTravelCost(travelCost);

        caregiver.setCurrentTime(startTime + p.getRequired_caregivers()[0].getDuration());
        caregiver.updateRoute(patient);
        caregiver.updateTravelCost(travelCost);
        caregiver.updateTardiness(tardiness);
    }

    private static void processSynchronization(Solution ch, Patient p, Shift caregiver1,
                                               Shift caregiver2, Required_Caregiver[] requiredCaregivers, double startTime1,
                                               double startTime2, double timeWindowEnd) {

        double tardiness1, tardiness2;
        if (p.getSynchronization().getType().equals("sequential")) {
            double[] syncDistances = p.getSynchronization().getDistance();
            startTime2 = Math.max(startTime2, startTime1 + syncDistances[0]);
            if (startTime2 - startTime1 > syncDistances[1]) {
                startTime1 = startTime2 - syncDistances[1];
            }
            tardiness1 = Math.max(0, startTime1 - timeWindowEnd);
            tardiness2 = Math.max(0, startTime2 - timeWindowEnd);
        } else {
            double startTime = Math.max(startTime1, startTime2);
            tardiness1 = tardiness2 = Math.max(0, startTime - timeWindowEnd);
            startTime1 = startTime2 = startTime;

        }

        double maxTardiness = Math.max(tardiness1, tardiness2);
        ch.updateTotalTardiness(tardiness1 + tardiness2);
        ch.setHighestTardiness(Math.max(maxTardiness, ch.getHighestTardiness()));

        caregiver1.setCurrentTime(startTime1 + requiredCaregivers[0].getDuration());
        caregiver1.updateTardiness(tardiness1);

        caregiver2.setCurrentTime(startTime2 + requiredCaregivers[1].getDuration());
        caregiver2.updateTardiness(tardiness2);
    }

    private static int findSecondCaregiver(int pIndex, int route1, Shift[] routes, Solution ch, Set<Integer> track) {
        List<Integer>[] genes = ch.getGenes();

        for (int i = 0; i < genes.length; i++) {
            if (i != route1 && genes[i].contains(pIndex)) {
                List<Integer> route = genes[i];
                Shift caregiver = routes[i];
                int patientPosition = route.indexOf(pIndex);

                // Process patients up to the target position
                int j = caregiver.getRoute().size();
                while (caregiver.getRoute().size() != patientPosition && j < route.size()) {
                    int patient = route.get(j);
                    if (patientAssignment(ch, patient, caregiver, routes, i, track))
                        return Integer.MAX_VALUE;
                    j++;
                }
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static boolean shouldSwapCaregivers(Patient p, Shift caregiver1, Shift caregiver2) {
        Required_Caregiver[] requiredCaregivers = p.getRequired_caregivers();
        String service1 = requiredCaregivers[0].getService();
        String service2 = requiredCaregivers[1].getService();

        Set<Integer> service1Routes = dataset.getQualifiedCaregiver(service1);
        Set<Integer> service2Routes = dataset.getQualifiedCaregiver(service2);

        int caregiver2Id = caregiver2.getCaregiver().getCacheId();
        int caregiver1Id = caregiver1.getCaregiver().getCacheId();

        return (service1Routes.contains(caregiver2Id) && !service2Routes.contains(caregiver2Id)) ||
                (service2Routes.contains(caregiver1Id) && !service1Routes.contains(caregiver1Id)) ||
                (service1Routes.contains(caregiver2Id) && !service1Routes.contains(caregiver1Id));
    }

    static void UpdateCost(Solution ch) {
        double cost = (1 / 3d * ch.getTotalTravelCost()) + (1 / 3d * ch.getTotalTardiness()) + (1 / 3d * ch.getHighestTardiness());
//        double cost = 1 /3.0 *ch.getTotalTravelCost() + 1/3.0* ch.getTotalTardiness() + 1/3.0 *ch.getHighestTardiness();
        ch.setFitness(cost);
    }

    public static void EvaluateFitness(List<Solution> population) {
        for (Solution Solution : population) {
            EvaluateFitness(Solution);
        }
    }
    public static void EvaluateFitness(Solution ch) {
        ch.setHighestTardiness(0);
        ch.setTotalTardiness(0);
        ch.setTotalTravelCost(0);
        ch.setFitness(Double.POSITIVE_INFINITY);
        double travelCost = 0;
        int caregiverLength = allCaregivers.length;
        double[] highestAndTotalTardiness = new double[2];
        int[] routeEndPoint = new int[caregiverLength];
        double[] routesCurrentTime = routeStartTime.get();
        for (int i = 0; i < caregiverLength; i++) {
            routesCurrentTime[i] = 0;
        }

        Set<Integer> track = trackHolder.get();
        List<Integer>[] genes = ch.getGenes();
        for (int i = 0; i < genes.length; i++) {
            List<Integer> route = genes[i];
            int routeStartingPoint = 0;
            for (int j = 0; j < route.size(); j++) {
                if (j == 0) {
                    int nextIndex = route.get(j) + 1;
                    travelCost += distances[routeStartingPoint][nextIndex];
                    if (route.size() == 1) {
                        travelCost += distances[nextIndex][routeStartingPoint];
                    }
                } else if (j == route.size() - 1) {
                    int prevIndex = route.get(j - 1) + 1;
                    int nextIndex = route.get(j) + 1;
                    travelCost += distances[prevIndex][nextIndex];
                    travelCost += distances[nextIndex][routeStartingPoint];
                } else {
                    int prevIndex = route.get(j - 1) + 1;
                    int nextIndex = route.get(j) + 1;
                    travelCost += distances[prevIndex][nextIndex];
                }
            }
        }

        ch.setTotalTravelCost(travelCost);

        try {
            double solutionCost;
            for (int i = 0; i < routeEndPoint.length; i++) {
                List<Integer> route = genes[i];
                int routeEnd = routeEndPoint[i];
                if (routeEnd != -1) {
                    for (int j = routeEnd; j < route.size(); j++) {
                        int current = j == 0 ? 0 : route.get(j - 1)+1;
                        solutionCost = patientIsAssigned(genes, i, current, route.get(j), travelCost, routesCurrentTime, highestAndTotalTardiness, routeEndPoint, track);
                        if (solutionCost == Double.POSITIVE_INFINITY) {
                            ch.setFitness(Double.POSITIVE_INFINITY);
                            return;
                        }
                        track.clear();
                    }
                }
            }


            for (int i = 0; i < routeEndPoint.length; i++) {
                List<Integer> route = genes[i];
                if (!route.isEmpty()) {
                    int lastPatient = route.get(route.size()-1) + 1;
                    double distance = distances[lastPatient][0];
                    routesCurrentTime[i] += distance;
                }
            }

            ch.setHighestTardiness(highestAndTotalTardiness[0]);
            ch.setTotalTardiness(highestAndTotalTardiness[1]);
            UpdateCost(ch);
//            System.out.println("Travel: "+ch.getTotalTravelCost());
//            System.out.println("Tardiness: "+ch.getTotalTardiness());
//            System.out.println("Highest Tardiness: "+ch.getHighestTardiness());

        } finally {
            track.clear();
        }
    }

    public static double patientIsAssigned(List<Integer>[] genes, int route1, int curPatientIndex1, int nextPatientIndex, double totalTravelCost, double[] routesCurrentTime, double[] highestAndTotalTardiness,  int[] routeEndPoint, Set<Integer> track) {
        Patient nextPatient = allPatients[nextPatientIndex];
        double[] timeWindow = nextPatient.getTime_window();
        double patientOpenTimeWindow = timeWindow[0];
        int currentLocation1 = curPatientIndex1;
        int nextLocation = nextPatient.getDistance_matrix_index();

        double arrivalTime1 = routesCurrentTime[route1] + distances[currentLocation1][nextLocation];
        double startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);

        if (nextPatient.getRequired_caregivers().length > 1) {
            if (!track.add(nextPatientIndex)) {
                return Double.POSITIVE_INFINITY;
            }

            int route2 = findOtherCaregiver(nextPatientIndex, route1, genes, totalTravelCost, routesCurrentTime, highestAndTotalTardiness, routeEndPoint, track);
            if (route2 > allCaregivers.length - 1) {
                return Double.POSITIVE_INFINITY;
            }

            int position2 = routeEndPoint[route2] - 1;
            int curPatient2;
            if (position2 == -1) {
                curPatient2 = 0;
            } else {
                curPatient2 = genes[route2].get(position2)+1;
            }

            if (SwapRoutes(nextPatient, route1, route2)) {
                int temp = route1;
                route1 = route2;
                route2 = temp;

                temp = curPatientIndex1;
                curPatientIndex1 = curPatient2;
                curPatient2 = temp;

                currentLocation1 = curPatientIndex1;
                arrivalTime1 = routesCurrentTime[route1] + distances[currentLocation1][nextLocation];
                startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);
            }

            int currentLocation2 = curPatient2;
            double arrivalTime2 = routesCurrentTime[route2] + distances[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, patientOpenTimeWindow);

            if (nextPatient.getSynchronization().getType().equals("sequential")) {
                double[] syncDistances = nextPatient.getSynchronization().getDistance();
                startTime2 = Math.max(startTime2, startTime1 + syncDistances[0]);
                if (startTime2 - startTime1 > syncDistances[1]) {
                    startTime1 = startTime2 - syncDistances[1];
                }
                double tardiness = Math.max(0, startTime1 - timeWindow[1]);
                highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
                highestAndTotalTardiness[1] += tardiness;
                tardiness = Math.max(0, startTime2 - timeWindow[1]);
                highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
                highestAndTotalTardiness[1] += tardiness;
            } else {
                double startTime = Math.max(startTime1, startTime2);
                double tardiness = Math.max(0, startTime - timeWindow[1]);
                highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
                highestAndTotalTardiness[1] += (2 * tardiness);
                startTime1 = startTime2 = startTime;

            }


            routeEndPoint[route1]++;
            routeEndPoint[route2]++;
            routesCurrentTime[route1] = startTime1 + nextPatient.getRequired_caregivers()[0].getDuration();
            routesCurrentTime[route2] = startTime2 + nextPatient.getRequired_caregivers()[1].getDuration();

        } else {

            double tardiness = Math.max(0, startTime1 - timeWindow[1]);
            highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
            highestAndTotalTardiness[1] += tardiness;
            routeEndPoint[route1]++;
            routesCurrentTime[route1] = startTime1 + nextPatient.getRequired_caregivers()[0].getDuration();
        }
        return 1/3d * totalTravelCost + 1/3d * highestAndTotalTardiness[0] + 1/3d * highestAndTotalTardiness[1];
    }

    private static int findOtherCaregiver(int patient, int route1, List<Integer>[] genes, double totalTravelCost, double[] routesCurrentTime, double[] highestAndTotalTardiness, int[] routeEndPoint, Set<Integer> track) {
        for (int i = 0; i < genes.length; i++) {
            if (i != route1 && genes[i].contains(patient)) {
                List<Integer> route = genes[i];
                int patientPosition = route.indexOf(patient);

                int j = routeEndPoint[i];
                while (routeEndPoint[i] != patientPosition && j < route.size()) {
                    int curPatient;
                    if (j == 0)
                        curPatient = 0;
                    else
                        curPatient = route.get(j - 1)+1;
                    if (patientIsAssigned(genes, i, curPatient, route.get(j), totalTravelCost, routesCurrentTime, highestAndTotalTardiness, routeEndPoint, track) == Double.POSITIVE_INFINITY)
                        return Integer.MAX_VALUE;
                    j++;
                }
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static boolean SwapRoutes(Patient p, int route1, int route2) {
        Required_Caregiver[] requiredCaregivers = p.getRequired_caregivers();
        String service1 = requiredCaregivers[0].getService();
        String service2 = requiredCaregivers[1].getService();

        Set<Integer> service1Routes = dataset.getQualifiedCaregiver(service1);
        Set<Integer> service2Routes = dataset.getQualifiedCaregiver(service2);

        return (service1Routes.contains(route2) && !service2Routes.contains(route2)) ||
                (service2Routes.contains(route1) && !service1Routes.contains(route1)) ||
                (service1Routes.contains(route2) && !service1Routes.contains(route1));
    }
}