import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class GuitarTab implements Comparable<GuitarTab> {
    protected int[] bottomE;
    protected int[] aString;
    protected int[] dString;
    protected int[] gString;
    protected int[] bString;
    protected int[] topE;

    public final int[][] allStrings;// = {bottomE,aString,dString,gString,bString,topE};

    protected int numTicks;
    protected double fitness;


    //all notes on a guitar in standard tuning
    public static final String[] BOTTOM_E_NOTE_NAMES = {"E3", "F3", "F#3", "G3", "G#3", "A3", "A#3", "B3","C4", "C#4", "D4", "D#4","E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4","C5", "C#5", "D5", "D#5","E5"};
    public static final String[] A_STRING_NOTE_NAMES = {"A3", "A#3", "B3","C4", "C#4", "D4", "D#4","E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4","C5", "C#5", "D5", "D#5","E5","F5","F#5","G5","G#5","A5"};
    public static final String[] D_STRING_NOTE_NAMES = {"D4", "D#4","E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4","C5", "C#5", "D5", "D#5","E5","F5","F#5","G5","G#5","A5","A#5","B5","C6","C#6","D6"};
    public static final String[] G_STRING_NOTE_NAMES = {"G4", "G#4", "A4", "A#4", "B4","C5", "C#5", "D5", "D#5","E5","F5","F#5","G5","G#5","A5","A#5","B5","C6","C#6","D6","D#6","E6","F6","F#6","G6"};
    public static final String[] B_STRING_NOTE_NAMES = {"B4","C5", "C#5", "D5", "D#5","E5","F5","F#5","G5","G#5","A5","A#5","B5","C6","C#6","D6","D#6","E6","F6","F#6","G6","G#6","A6","A#6","B6"};
    public static final String[] TOP_E_NOTE_NAMES = {"E5", "F5", "F#5", "G5", "G#5", "A5", "A#5", "B5","C6", "C#6", "D6", "D#6","E6", "F6", "F#6", "G6", "G#6", "A6", "A#6", "B6","C7", "C#7", "D7", "D#7","E7"};


    public GuitarTab(int numTicks) {
        this.bottomE = new int[numTicks];
        this.aString = new int[numTicks];
        this.dString = new int[numTicks];
        this.gString = new int[numTicks];
        this.bString = new int[numTicks];
        this.topE = new int[numTicks];

        Arrays.fill(this.bottomE, -1);
        Arrays.fill(this.aString, -1);
        Arrays.fill(this.dString, -1);
        Arrays.fill(this.gString, -1);
        Arrays.fill(this.bString, -1);
        Arrays.fill(this.topE, -1);

        this.numTicks = numTicks;
        allStrings = new int[][]{bottomE, aString, dString, gString, bString, topE};

        fitness = 0;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    //randomly generate a guitar tab given a list of notes
    public void generateTab(ArrayList<Note> notes){

        int last_tick = -1;
        ArrayList<Note> simultaneousNotes = new ArrayList<>();

        for (Note note:notes) {//loop through each note in the midi file
            if(note.tick!=last_tick){//check you are not at the last tick in the file

                //get all notes that are played at a given tick
                simultaneousNotes = getSimultaneousNotes((int)note.tick,notes);

                if(simultaneousNotes.size()==1){//if there is only one note played at a given tick
                    placeSingleNote(simultaneousNotes.get(0));
                }else{//if there is more than one note being played at a given tick

                    boolean[][] note_matrix = configureChord(simultaneousNotes);//find a valid permutation of strings the notes can be played on

                    for(int i = 0; i<note_matrix.length;i++){
                        randomlyPlaceNote(note_matrix[i],simultaneousNotes.get(i).fullNoteName,(int)simultaneousNotes.get(i).tick);
                    }
                }
                last_tick = (int)note.tick;
            }

        }
    }

    //a function to return an array list of all notes played at a given tick
    public static ArrayList<Note> getSimultaneousNotes(int tick, ArrayList<Note> notes){
        ArrayList<Note> simultaneousNotes = new ArrayList<>();

        //for each note in the note list. if the tick is equal to the tick parameter, add note to the simultaneous notes list
        for (Note note:notes) {
            if(note.tick==tick){
                simultaneousNotes.add(note);
            }
        }
        return simultaneousNotes;
    }

    public static boolean[][] configureChord(ArrayList<Note> notes){//currently working on the assumption that all notes are in range and only up to six notes played at once
        //create boolean array of size [number of notes][6]
        boolean[][] noteMatrix = new boolean[notes.size()][6];
        for(int i = 0; i<noteMatrix.length;i++){
            noteMatrix[i] = findFretLocations(notes.get(i).fullNoteName);
        }
        ArrayList<Integer> setChordList = new ArrayList<>();
        while(!checkMatrix(noteMatrix)){//do until each array in note matrix only has 1 true value
            //select array with lowes true_count // if draw latest
            int lowestCountIndex = lowestTrueCount(noteMatrix, setChordList);

            int random_index = pickRandomIndex(noteMatrix[lowestCountIndex]);
            //set all others to false
            set(noteMatrix,random_index,lowestCountIndex);
            //add index of set note to list
            setChordList.add(lowestCountIndex);

        }
        return noteMatrix;
    }


    static boolean checkMatrix(boolean[][] note_matrix){//function to see if each array in the matrix has a true count of one true_count is 1 for each note, returns true
        for (boolean[] string_array:note_matrix) {
            int true_count = 0;
            for (boolean string:string_array) {
                if (string){
                    true_count++;
                    if(true_count>1){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    //a function to return note with the least number of string available to play on
    static int lowestTrueCount(boolean[][] noteMatrix, ArrayList<Integer> setChordList){//loop through the matrix and return the note with the lowest true count
        int lowestCountIndex = 0;
        int lowestCount = 7;
        int trueCount = 0;
        int indexCounter = 0;

        //loops through each array in the note matrix and counts the total number of true bool vars
        for (boolean[] stringArray:noteMatrix) {
            if((!setChordList.contains(indexCounter))){
                trueCount = 0;
                for(int i=0; i<=5;i++){
                    if(stringArray[i]){
                        trueCount++;
                    }
                }
                if(trueCount<lowestCount){
                    lowestCount=trueCount;
                    lowestCountIndex=indexCounter;
                }
            }

            indexCounter++;
        }


        return lowestCountIndex;
    }

    //select random index from playable notes
    static int pickRandomIndex(boolean[] stringArray){
        int bound;
        int randomIndex;
        ArrayList<Integer> trueIndexList = new ArrayList<>();

        for(int i = 0; i<=5;i++){
            if(stringArray[i]){
                trueIndexList.add(i);
            }
        }

        Random random = new Random();
        randomIndex = random.nextInt(trueIndexList.size());

        return trueIndexList.get(randomIndex);
    }


    public static boolean[][] set(boolean[][] noteMatrix,int stringIndex, int noteIndex){//string index - index of the string being set, note index - index of the note being set
        /////FUNCTION TO SET ALL NOTES BUT ONE TO TRUE OF A CERTAIN STRING VALUE // need to alter to set the string itself.
        for(int i=0; i<noteMatrix.length;i++){
            if(i!=noteIndex){
                for(int j=0; j<=5;j++){
                    if(j==stringIndex){
                        noteMatrix[i][j]=false;
                    }
                }
            }else{
                for(int j=0; j<=5; j++){
                    if(j!=stringIndex){
                        noteMatrix[i][j]=false;
                    }
                }
            }

        }
        return noteMatrix;
    }
    public void placeSingleNote(Note note){
        boolean noteArray[];//initialise array to show if a note can be played (from bottom E to top E)
        noteArray = findFretLocations(note.fullNoteName);//find possible fret locations
        //no need to check if string is empty as it is the only note being placed
        randomlyPlaceNote(noteArray, note.fullNoteName, (int)note.tick);//place the note according to which notes are free
    }

    //a function to return a boolean array representing whether a note can be played on each string
    public static boolean[] findFretLocations(String fullNoteName){
        boolean bottomE = false;
        boolean aString = false;
        boolean dString = false;
        boolean gString = false;
        boolean bString = false;
        boolean topE = false;
        int count = 0;



        if(Arrays.asList(BOTTOM_E_NOTE_NAMES).contains(fullNoteName)){
            bottomE=true;
            count++;
        }
        if(Arrays.asList(A_STRING_NOTE_NAMES).contains(fullNoteName)){
            aString=true;
            count++;
        }
        if(Arrays.asList(D_STRING_NOTE_NAMES).contains(fullNoteName)){
            dString=true;
            count++;
        }
        if(Arrays.asList(G_STRING_NOTE_NAMES).contains(fullNoteName)){
            gString=true;
            count++;
        }
        if(Arrays.asList(B_STRING_NOTE_NAMES).contains(fullNoteName)){
            bString=true;
            count++;
        }
        if(Arrays.asList(TOP_E_NOTE_NAMES).contains(fullNoteName)){
            topE=true;
            count++;
        }


        return new boolean[]{bottomE, aString, dString, gString, bString, topE};
    }

    //given a note, the note is randomly placed on a string where it can be played
    public void randomlyPlaceNote(boolean[] boolNotes, String note, int tick){
        Random rand = new Random();

        checkIfPlayable(boolNotes);

        while(true){
            int i = rand.nextInt(0,6);
            if(boolNotes[i]){
                //need to check if string is empty or not
                switch (i) {
                    case (0) -> this.bottomE[tick] = Arrays.asList(BOTTOM_E_NOTE_NAMES).indexOf(note);
                    case (1) -> this.aString[tick] = Arrays.asList(A_STRING_NOTE_NAMES).indexOf(note);
                    case (2) -> this.dString[tick] = Arrays.asList(D_STRING_NOTE_NAMES).indexOf(note);
                    case (3) -> this.gString[tick] = Arrays.asList(G_STRING_NOTE_NAMES).indexOf(note);
                    case (4) -> this.bString[tick] = Arrays.asList(B_STRING_NOTE_NAMES).indexOf(note);
                    case (5) -> this.topE[tick] = Arrays.asList(TOP_E_NOTE_NAMES).indexOf(note);
                }
                //System.out.println(tick);
                break;
            }
        }
    }

    //print the tab in ascii form
    public void printTab(int resolution) {
        printGuitarString(this.topE,resolution);
        printGuitarString(this.bString,resolution);
        printGuitarString(this.gString,resolution);
        printGuitarString(this.dString,resolution);
        printGuitarString(this.aString,resolution);
        printGuitarString(this.bottomE, resolution);
    }

    //function to print an individual string in ascii form
    public void printGuitarString(int[] guitarString, int resolution){
        int counter = 1;
        for(int note : guitarString){
            if(note==-1){
                counter++;
                if(counter == resolution/4){
                    System.out.print("-");
                    counter=0;
                }
            }else{
                System.out.print(note);
            }
        }
        System.out.println("");
    }

    //a function to check if any values in a noteArray are true
    public static boolean checkIfPlayable(boolean[] noteArray){
        if(noteArray==new boolean[]{false,false,false,false,false,false}){
            try {
                throw new Exception();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    //A function to randomly change a note position to another valid place to play the same note
    public boolean randomlyChangeNote(int stringIndex, int noteIndex){
        //get note name
        int fretNumber;
        String noteName = "";
        switch (stringIndex){
            case 0:
                fretNumber = this.bottomE[noteIndex];
                noteName = BOTTOM_E_NOTE_NAMES[fretNumber];
                break;
            case 1:
                fretNumber = this.aString[noteIndex];
                noteName = A_STRING_NOTE_NAMES[fretNumber];
                break;
            case 2:
                fretNumber = this.dString[noteIndex];
                noteName = D_STRING_NOTE_NAMES[fretNumber];
                break;
            case 3:
                fretNumber = this.gString[noteIndex];
                noteName = G_STRING_NOTE_NAMES[fretNumber];
                break;
            case 4:
                fretNumber = this.bString[noteIndex];
                noteName = B_STRING_NOTE_NAMES[fretNumber];
                break;
            case 5:
                fretNumber = this.topE[noteIndex];
                noteName = TOP_E_NOTE_NAMES[fretNumber];
                break;

        }

        //check if playable elsewhere
        boolean[] stringBoolean = findFretLocations(noteName);
        int trueCount = 0;
        for(boolean bool : stringBoolean){
            if(bool){
                trueCount++;
            }
        }
        if(trueCount>1){
            int[][] stringArray = {bottomE,aString,dString,gString,bString,topE};
            int counter = 0;


            for (int[] string: stringArray) {//for each string
                if(string[noteIndex]!=-1){//checks to see if note played at given tick
                    stringBoolean[counter]=false;
                }
                counter++;
            }
            trueCount = 0;
            for(boolean bool : stringBoolean){
                if(bool){
                    trueCount++;
                }
            }
            if(trueCount>0){
                randomlyPlaceNote(stringBoolean,noteName,noteIndex);
                switch (stringIndex){
                    case 0:
                        this.bottomE[noteIndex]=-1;
                        break;
                    case 1:
                        this.aString[noteIndex]=-1;
                        break;
                    case 2:
                        this.dString[noteIndex]=-1;
                        break;
                    case 3:
                        this.gString[noteIndex]=-1;
                        break;
                    case 4:
                        this.bString[noteIndex]=-1;
                        break;
                    case 5:
                        this.topE[noteIndex]=-1;
                        break;

                }
            }else{
                return false;
            }
        }
        return false;
    }

    public static void main(String[] args){

    }


    @Override
    public int compareTo(GuitarTab o) {
        return Double.compare(fitness,o.getFitness());
    }
}
