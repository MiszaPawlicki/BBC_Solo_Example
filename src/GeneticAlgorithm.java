import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class GeneticAlgorithm {

    private static GuitarTab[] population;
    private static ArrayList<Double> generational_fitness = new ArrayList<>();
    protected static MidiFileReader midiFileReader;

    public static GuitarTab[] getPopulation() {
        return population;
    }

    //reading the midi file
    private static void readMidiFile(String path) throws InvalidMidiDataException, IOException {
        midiFileReader = new MidiFileReader(path);
    }

    //a method to generate the population of guitar tabs
    private static void generatePopulation(int populationSize, String path) throws InvalidMidiDataException, IOException {
        readMidiFile(path);//read midi file
        population = new GuitarTab[populationSize];

        for(int i = 0; i<populationSize;i++){//generate a population of guitar tabs of size population_size
            GuitarTab guitarTab = new GuitarTab((int)midiFileReader.tickLength);
            guitarTab.generateTab(midiFileReader.notes);
            population[i] = guitarTab;
        }
    }

    //a method to calculate the fitness of each individual member of the population
    private static void calculateEachMemberFitness(){
        long currentTick = -1;
        int[] currentSimultaneousNotes = null;
        int[] previousSimultaneousNotes = null;

        double totalDistance; // stores the total euclidean distance of a tab
        double distance;//stores the distance of one set of notes to another

        //looping through all notes calculating the distance from one set to another
        for (GuitarTab guitarTab:population) {
            totalDistance = 0;
            for (Note note : midiFileReader.notes) {
                if(note.tick!=currentTick){
                    if(currentSimultaneousNotes==null){
                        currentSimultaneousNotes = getSimultaneousNotes((int)note.tick, guitarTab);
                        currentTick = note.tick;
                    }else{
                        previousSimultaneousNotes = currentSimultaneousNotes;
                        currentSimultaneousNotes = getSimultaneousNotes((int)note.tick, guitarTab);
                        distance = euclidianDistance(previousSimultaneousNotes, currentSimultaneousNotes);
                        totalDistance+=distance;
                    }
                }
            }
            guitarTab.setFitness(totalDistance);
        }

        //sort the population in order of fitness
        Arrays.sort(population);

    }

    // a function to get all notes played simultaniously
    private static int[] getSimultaneousNotes(int tick, GuitarTab guitarTab){
        int[] allStrings = {-1,-1,-1,-1,-1,-1}; // boolean array representing each note and whether the string is in use

        //check if any note is played on each string at a given tick
        if(guitarTab.bottomE[tick]!=-1){
            allStrings[0] = guitarTab.bottomE[tick];
        }
        if(guitarTab.aString[tick]!=-1){
            allStrings[1] = guitarTab.aString[tick];
        }
        if(guitarTab.dString[tick]!=-1){
            allStrings[2] = guitarTab.dString[tick];
        }
        if(guitarTab.gString[tick]!=-1){
            allStrings[3] = guitarTab.gString[tick];
        }
        if(guitarTab.bString[tick]!=-1){
            allStrings[4] = guitarTab.bString[tick];
        }
        if(guitarTab.topE[tick]!=-1){
            allStrings[5] = guitarTab.topE[tick];
        }
        return allStrings;
    }

    // a function to calculate the euclidean distance between two arrays
    private static double euclidianDistance(int[] x, int[] y){//for single notes

        int xCounter = 0;
        int yCounter = 0;

        double total = 0;

        for(int i=0; i<x.length; i++){
            if(x[i]!=-1){
                for(int j=0; j<y.length;j++){
                    if(y[j]!=-1){
                        int a = i+x[i];
                        int b = j+y[j];
                        double distance = Math.sqrt(Math.pow(a,2)+Math.pow(b,2));//x(string, fret), y(string, fret)///////i and y indicate string,
                        total+=distance;
                        yCounter++;
                    }
                }
                xCounter++;
            }
        }
        int numberOfConnections = xCounter+yCounter;
        total = total/numberOfConnections;
        //divide total distance by number of connections
        return total;
    }

    //randomly select two tabs and return the index of the tabs that one their individual comparasons
    private static ArrayList<Integer> tournamentSelection(int numSelections){
        Random random = new Random();
        ArrayList<Integer> indexOfFittest = new ArrayList<>();

        //run the selection process numSelection times
        for(int i = 0; i< numSelections; i++){
            int randomIndex1;
            int randomIndex2;

            while(true){
                //getting the random tabs
                randomIndex1 = random.nextInt(numSelections);
                randomIndex2 = random.nextInt(numSelections);
                //checking the tab hasnt already been selected
                if(!(indexOfFittest.contains(randomIndex1))&&!(indexOfFittest.contains(randomIndex2))){
                    break;
                }
            }

            //comparing both tabs and adding the index of the fittest to indexOfFittest list
            if(population[randomIndex1].getFitness()>population[randomIndex2].getFitness()){
                indexOfFittest.add(randomIndex1);
            }else{
                indexOfFittest.add(randomIndex2);
            }

        }
        return indexOfFittest;
    }

    //method to create 2 child tabs from 2 parent tabs
    static void reproduce(ArrayList<Integer> indexOfFittest){//for each 2 tabs selected in the tournament, swap the middle third to create new child tabs
        int counter = 0;
        GuitarTab[] newPopulation = new GuitarTab[population.length];
        Random rand = new Random();

        //loop through the index of the fittest, two tabs at a time
        //already random as the index of fittest list is randomly selected
        for (int n = 1; n<indexOfFittest.size();n+=2){
            int idx1 = n;
            int idx2 = n-1;

            // set the two parent tabs
            GuitarTab tab1 = population[indexOfFittest.get(idx1)];
            GuitarTab tab2 = population[indexOfFittest.get(idx2)];

            //crossover the two parent tabs to create the child tabs
            GuitarTab childTab1 = crossover(tab1,tab2,0.5);
            GuitarTab childTab2 = crossover(tab2,tab1,0.5);

            //randomly choose one child tab to mutate
            if(rand.nextInt(2)==1){
                childTab1 = mutate(childTab1);
            }else{
                childTab2 = mutate(childTab2);
            }

            //add new tabs to the population
            newPopulation[counter++] = tab1;
            newPopulation[counter++] = tab2;
            newPopulation[counter++] = childTab1;
            newPopulation[counter++] = childTab2;
        }
        population = newPopulation;

    }
    //a function to crossover to parent tabs to create a child tab
    static GuitarTab crossover(GuitarTab parent1,GuitarTab parent2, double crossover){
        GuitarTab childTab = new GuitarTab((int)midiFileReader.tickLength);

        //loop through a section of each tab specified by the crossover. Set the child tab to the same of a given section
        for(int i = 0; i<midiFileReader.tickLength*crossover;i++){
            childTab.bottomE[i]=parent1.bottomE[i];
            childTab.aString[i]=parent1.aString[i];
            childTab.dString[i]=parent1.dString[i];
            childTab.gString[i]=parent1.gString[i];
            childTab.bString[i]=parent1.bString[i];
            childTab.topE[i]=parent1.topE[i];

        }
        for(int i = (int) (midiFileReader.tickLength*crossover); i<midiFileReader.tickLength; i++){
            childTab.bottomE[i]=parent2.bottomE[i];
            childTab.aString[i]=parent2.aString[i];
            childTab.dString[i]=parent2.dString[i];
            childTab.gString[i]=parent2.gString[i];
            childTab.bString[i]=parent2.bString[i];
            childTab.topE[i]=parent2.topE[i];

        }
        return childTab;
    }

    // a function to randomly change one note within a guitar tab
    public static GuitarTab mutate(GuitarTab guitarTab){
        Random random = new Random();
        ArrayList<Integer> indexList = new ArrayList<>();
        //pick random string
        int randomStringIndex=random.nextInt(6);
        int[] string_array = getStringArray(randomStringIndex,guitarTab);//select a string at random

        //for each tick in the string add the tick number if the value is not -1
        for(int i = 0; i<string_array.length;i++){
            if(string_array[i]!=-1){//get index of all notes played on that string
                indexList.add(i);
            }
        }
        if(indexList.size()==0){
            return guitarTab;
        }

        int randomElement = random.nextInt(indexList.size());
        int randomTick = indexList.get(randomElement);//select a note index at random

        guitarTab.randomlyChangeNote(randomStringIndex, randomTick);

        return guitarTab;
    }

    //calculate the average fitness of a generation
    private static void calculateGenerationFitness(){
        double total = 0;

        for (GuitarTab guitarTab:population) {
            total+=guitarTab.fitness;
        }
        total=total/population.length;
        generational_fitness.add(total);

    }

    //a function to return the string array containing the notes of a given string. takes the string index and guitar tab as parameters
    private static int[] getStringArray(int index, GuitarTab tab){

        if(index==0){
            return tab.bottomE;
        }else if(index == 1){
            return tab.aString;
        }else if(index == 2){
            return tab.dString;
        }else if(index == 3){
            return tab.gString;
        }else if(index == 4){
            return tab.bString;
        }else if(index == 5){
            return tab.topE;
        }
        return null;
    }

    //method to run all steps in the genetic algorithm
    public void runGeneticAlgorithm(String path) throws InvalidMidiDataException, IOException {
        generatePopulation(100,path);

        for(int i = 0; i<500;i++){
            calculateEachMemberFitness();
            ArrayList<Integer> indexesOfFittest = tournamentSelection(50);
            calculateGenerationFitness();
            System.out.println(generational_fitness.get(i));

            reproduce(indexesOfFittest);
            calculateEachMemberFitness();
            Arrays.sort(population);

        }

        population[0].printTab(midiFileReader.resolution);
    }

    public static void main(String[] args) throws InvalidMidiDataException, IOException {


    }
}

