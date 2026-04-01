package org.example.Tabu;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;
import org.example.Main;

import java.util.*;

public class Solution {
    private List<Integer>[] genes;
    private int caregivers;
    private int crossIndex = -1;
    private int first=-1;
    private int second=-1;
    private int third=-1;
    private int fourth=-1;
    private int firstPosition =-1;
    private int secondPosition =-1;
    private int thirdPosition =-1;
    private int fourthPosition =-1;
    private double rank = -1;
    private int lastmove = -1;
    private double fitness;
    private double totalTravelCost;
    private double totalTardiness;
    private double highestTardiness;
    private double totalWaitingTime;
    private double HighestIdleTime;
    private double Overtime;
    private double scalarFitness;
    private double normScalarFitness;
    private Shift[] caregiversRouteUp;
    private final double[] objectives = new double[6];
    private final double[] normObjectives = new double[6];
    private final Map<Integer, Set<Integer>> patientToRoutesMap = new HashMap<>();
    private double[] lambdas = new double[6];;

    public Solution(List<Integer>[] genes, boolean newChromosome) {
        if (newChromosome) {
            this.genes = new ArrayList[genes.length];
            for (int i = 0; i < genes.length; i++) {
                this.genes[i] = new ArrayList(genes[i]);
            }
        }else {
            this.genes = genes;
        }

        this.caregivers = genes.length;
        this.fitness = 0.0;
        this.scalarFitness = 0.0;
        this.normScalarFitness = 0.0;
        caregiversRouteUp = new Shift[caregivers];
        this.totalTravelCost = 0;
        this.totalTardiness = 0;
        this.highestTardiness = 0;
        this.totalWaitingTime = 0;
        this.HighestIdleTime = 0;
        this.Overtime = 0;
    }

    public Solution(List<Integer>[] genes, double fitness, boolean newChromosome) {
        if (newChromosome) {
            this.genes = new ArrayList[genes.length];
            for (int i = 0; i < genes.length; i++) {
                this.genes[i] = new ArrayList(genes[i]);
            }
        }else {
            this.genes = genes;
        }

        this.caregivers = genes.length;
        this.fitness = fitness;
        this.scalarFitness = 0.0;
        this.normScalarFitness = 0.0;
        caregiversRouteUp = new Shift[caregivers];
        this.totalTravelCost = 0;
        this.totalTardiness = 0;
        this.highestTardiness = 0;
        this.totalWaitingTime = 0;
        this.HighestIdleTime = 0;
        this.Overtime = 0;
    }
    public Solution(List<Integer>[] genes, double fitness, double scalarFitness, double normScalarFitness, boolean newChromosome, Shift[] routes) {
        if (newChromosome) {
            this.genes = new ArrayList[genes.length];
            for (int i = 0; i < genes.length; i++) {
                this.genes[i] = new ArrayList(genes[i]);
            }
        }else {
            this.genes = genes;
        }

        this.caregivers = genes.length;
        this.fitness = fitness;
        this.scalarFitness = scalarFitness;
        this.normScalarFitness = normScalarFitness;
        caregiversRouteUp = routes;
        this.totalTravelCost = 0;
        this.totalTardiness = 0;
        this.highestTardiness = 0;
        this.totalWaitingTime = 0;
        this.HighestIdleTime = 0;
        this.Overtime = 0;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public int getCrossIndex() {
        return crossIndex;
    }

    public void setCrossIndex(int crossIndex) {
        this.crossIndex = crossIndex;
    }

    public int getFirst() {
        return first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public int getFirstPosition() {
        return firstPosition;
    }

    public void setFirstPosition(int firstPosition) {
        this.firstPosition = firstPosition;
    }

    public int getSecondPosition() {
        return secondPosition;
    }

    public void setSecondPosition(int secondPosition) {
        this.secondPosition = secondPosition;
    }

    public int getThird() {
        return third;
    }

    public void setThird(int third) {
        this.third = third;
    }

    public int getFourth() {
        return fourth;
    }

    public void setFourth(int fourth) {
        this.fourth = fourth;
    }

    public int getThirdPosition() {
        return thirdPosition;
    }

    public void setThirdPosition(int thirdPosition) {
        this.thirdPosition = thirdPosition;
    }

    public int getFourthPosition() {
        return fourthPosition;
    }

    public void setFourthPosition(int fourthPosition) {
        this.fourthPosition = fourthPosition;
    }

    public int getMove() {
        return lastmove;
    }

    public void setMoves(int move) {
        this.lastmove = move;
    }

    public void initializeCaregiversRoute() {
        InstancesClass instance = Main.instance;
        for(int i =0; i<caregivers;i++){
            Caregiver caregiver = instance.getCaregivers()[i];
            Shift s = new Shift(caregiver,new ArrayList<>(), caregiver.getWorking_shift()[0]);
            caregiversRouteUp[i] = s;
        }
    }

    public int getCaregivers() {
        return caregivers;
    }
    // Call this once when `genes` is initialized or updated
    public void buildPatientRouteMap() {
        patientToRoutesMap.clear();
        Set<Integer> patients;
        for (int i = 0; i < genes.length; i++) {
            patients = new HashSet<>(genes[i]);
            for (int patient : patients) {
                patientToRoutesMap.computeIfAbsent(patient, k -> new HashSet<>()).add(i);
            }
        }
    }

    public  Map<Integer, Set<Integer>> getPatientToRoutesMap(){
        return patientToRoutesMap;
    }

    public Set<Integer> getPatientRoutes(int patient) {
        return patientToRoutesMap.get(patient);
    }

    public void setCaregivers(int caregivers) {
        this.caregivers = caregivers;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public List<Integer>[] getGenes() {
        return genes;
    }

    public void setGenes(List<Integer>[] genes) {
        this.genes = genes;
    }

    public void setCaregiversRouteUp(Shift[] caregiversRouteUp) {
        this.caregiversRouteUp = caregiversRouteUp;
    }
    public Shift[] getCaregiversRouteUp() {
        return caregiversRouteUp;
    }

    public double getTotalTravelCost() {
        return totalTravelCost;
    }

    public void setTotalTravelCost(double totalTravelCost) {
        this.totalTravelCost = totalTravelCost;
    }
    public void updateTotalTravelCost(double totalTravelCost) {
        this.totalTravelCost += totalTravelCost;
    }

    public double getTotalTardiness() {
        return totalTardiness;
    }

    public void setTotalTardiness(double totalTardiness) {
        this.totalTardiness = totalTardiness;
    }

    public double getHighestTardiness() {
        return highestTardiness;
    }

    public void setHighestTardiness(double highestTardiness) {
        this.highestTardiness = highestTardiness;
    }
    public void updateTotalTardiness(double totalTardiness) {
        this.totalTardiness += totalTardiness;
    }

    public double getTotalWaitingTime() {return totalWaitingTime;}
    public void setTotalWaitingTime(double totalWaitingTime) {this.totalWaitingTime = totalWaitingTime;}
    public void updateWaitingTime(double waitingTime) {this.totalWaitingTime += waitingTime;}

    public double getHighestIdleTime() {return HighestIdleTime;}
    public void setHighestIdleTime(double highestIdleTime) {this.HighestIdleTime = highestIdleTime;}
    public void updateHighestIdleTime(double highestIdleTime) {this.HighestIdleTime = Math.max(highestIdleTime,this.HighestIdleTime);}

    public double getOvertime() {return Overtime;}
    public void setOvertime(double overtime) {this.Overtime = overtime;}
    public void updateOvertime(double overtime) {this.Overtime += overtime;}

    public void showSolution(int index) {
        System.out.print("\n Best Solution : "+index+"\n");
        for (int i =0; i< genes.length; i++) {
            List<Integer> route = genes[i];
            Shift Caregiver = caregiversRouteUp[i];
            System.out.println(Caregiver.getCaregiver().getId() +" - "+ route);
            System.out.println("Travel Cost to patients\n"+Caregiver.getTravelCost());
            System.out.println("Service completed time at patients\n"+Caregiver.getCurrentTime());
//            System.out.println("Route total tardiness: "+Caregiver.getTardiness());
            System.out.println("Route total tardiness: "+Caregiver.getTardiness().get(Caregiver.getTardiness().size()-1)+" Route Highest tardiness: "+Caregiver.getMaxTardiness().get(Caregiver.getMaxTardiness().size()-1));
            System.out.println("Waiting Time: "+Caregiver.getTotalWaitingTime());
            System.out.println("Overtime: "+Caregiver.getOvertime());
            System.out.println();
        }

    }
    @Override
    public String toString() {
        StringBuilder genesStrings= new StringBuilder();
        for(List<Integer> c :genes){
            genesStrings.append(c.toString());
        }
        return genesStrings.toString();
    }

    public double[] getObjective() {
        objectives[0] = totalTravelCost;
        objectives[1] = totalTardiness;
        objectives[2] = highestTardiness;
        objectives[3] = totalWaitingTime;
        objectives[4] = HighestIdleTime;
        objectives[5] = Overtime;
        return objectives;
    }

    public double[] getLambdas(){
        return lambdas;
    }
    public void setLambdas(double[] lambdas){
        this.lambdas = lambdas;
    }

    public double getScalarFitness() {
        return scalarFitness;
    }

    public void setScalarFitness(double scalarFitness) {
        this.scalarFitness = scalarFitness;
    }

    public double getNormScalarFitness() {
        return normScalarFitness;
    }

    public void setNormScalarFitness(double normScalarFitness) {
        this.normScalarFitness = normScalarFitness;
    }

    public double[] getNormObjectives() {
        return normObjectives;
    }

    public static Solution initialize(int solutionLength){
        InstancesClass instances = Main.instance;
        Caregiver[] allCaregivers = instances.getCaregivers();
        int caregiverNumber = allCaregivers.length;
        double[][] distanceMatrix = instances.getDistances();
        Patient[] patients = instances.getPatients();
        CaregiverPair[] clusterPatient = new CaregiverPair[solutionLength];
        List<Integer> patientOrder= new ArrayList<>();
        for (int s = 0; s < solutionLength; s++) {
            patientOrder.add(s);
        }
        for (int s = 0; s < solutionLength; s++) {
            Patient patient = patients[s];
            clusterPatient[s] = getClosestCaregiver(patient, allCaregivers, distanceMatrix);;
        }

        Collections.shuffle(patientOrder);
        List<Integer>[] genes = new ArrayList[caregiverNumber];
        for(int j = 0; j < caregiverNumber; j++){
            genes[j] = new ArrayList<>();
        }
        for(int p : patientOrder){
            CaregiverPair caregiverPair = clusterPatient[p];
            genes[caregiverPair.getFirst()].add(p);
            if(caregiverPair.getSecond()!=-1){
                genes[caregiverPair.getSecond()].add(p);
            }
        }
        return new Solution(genes,false);
    }
    private static CaregiverPair getClosestCaregiver(Patient patient, Caregiver[] allCaregivers, double[][] distanceMatrix) {
        double bestCaregiverDistance = Double.MAX_VALUE;
        CaregiverPair bestCaregiverPair = null;
        for(CaregiverPair caregiverPair : patient.getAllPossibleCaregiverCombinations()){
            double caregiverDistance = distanceMatrix[caregiverPair.getFirst()][patient.getDistance_matrix_index()];
            if(patient.getRequired_caregivers().length > 1){
                caregiverDistance += distanceMatrix[caregiverPair.getSecond()][patient.getDistance_matrix_index()];
            }
            if(caregiverDistance < bestCaregiverDistance || caregiverDistance == bestCaregiverDistance && Math.random() > 0.5){
                bestCaregiverDistance = caregiverDistance;
                bestCaregiverPair = caregiverPair;
            }
        }
        return bestCaregiverPair;
    }
}
