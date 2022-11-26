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

    private static void readMidiFile(String path) throws InvalidMidiDataException, IOException {
        midiFileReader = new MidiFileReader(path);
    }

    private static void generatePopulation(int populationSize, String path) throws InvalidMidiDataException, IOException {

        readMidiFile(path);//read midi file
        population = new GuitarTab[populationSize];

        for(int i = 0; i<populationSize;i++){//generate a population of guitar tabs of size population_size
            GuitarTab guitarTab = new GuitarTab((int)midiFileReader.tickLength);
            guitarTab.generateTab(midiFileReader.notes);
            population[i] = guitarTab;
        }
    }

    private static void calculateEachMemberFitness(){
        long currentTick = -1;
        int[] currentSimultaneousNotes = null;
        int[] previousSimultaneousNotes = null;

        double totalDistance;
        double distance;


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

        Arrays.sort(population);

    }

    private static int[] getSimultaneousNotes(int tick, GuitarTab guitarTab){
        int[] allStrings = {-1,-1,-1,-1,-1,-1};
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

    private static ArrayList<Integer> tournamentSelection(int numSelections){
        Random random = new Random();
        ArrayList<Integer> indexOfFittest = new ArrayList<>();

        for(int i = 0; i< numSelections; i++){
            int randomIndex1;
            int randomIndex2;

            while(true){
                randomIndex1 = random.nextInt(numSelections);
                randomIndex2 = random.nextInt(numSelections);
                if(!(indexOfFittest.contains(randomIndex1))&&!(indexOfFittest.contains(randomIndex2))){
                    break;
                }
            }

            if(population[randomIndex1].getFitness()>population[randomIndex2].getFitness()){
                indexOfFittest.add(randomIndex1);
            }else{
                indexOfFittest.add(randomIndex2);
            }

        }
        return indexOfFittest;
    }

    static void reproduce(ArrayList<Integer> indexOfFittest){//for each 2 tabs selected in the tournament, swap the middle third to create new child tabs
        int counter = 0;
        GuitarTab[] newPopulation = new GuitarTab[population.length];
        Random rand = new Random();

        for (int n = 1; n<indexOfFittest.size();n+=2){
            int idx1 = n;
            int idx2 = n-1;
            GuitarTab tab1 = population[indexOfFittest.get(idx1)];
            GuitarTab tab2 = population[indexOfFittest.get(idx2)];

            GuitarTab childTab1 = crossover(tab1,tab2,0.5);
            GuitarTab childTab2 = crossover(tab2,tab1,0.5);

            if(rand.nextInt(2)==1){
                childTab1 = mutate(childTab1);
            }else{
                childTab2 = mutate(childTab2);
            }


            newPopulation[counter++] = tab1;
            newPopulation[counter++] = tab2;
            newPopulation[counter++] = childTab1;
            newPopulation[counter++] = childTab2;
        }
        population = newPopulation;

    }

    static GuitarTab crossover(GuitarTab parent1,GuitarTab parent2, double crossover){
        GuitarTab childTab = new GuitarTab((int)midiFileReader.tickLength);

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

    private static void calculateGenerationFitness(){
        double total = 0;

        for (GuitarTab guitarTab:population) {
            total+=guitarTab.fitness;
        }
        total=total/population.length;
        generational_fitness.add(total);

    }

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

    public static void main(String[] args) throws InvalidMidiDataException, IOException {



        generatePopulation(100,"new.mid");

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
}

