import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.midi.*;

public class MidiFileReader {

    public static final int NOTE_ON = 0x90; //value for note on
    public static final int NOTE_OFF = 0x80;//value for note off
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"}; //all possible notes regardless of octave that can be played
    public static String path;
    public static long tickLength;
    public static int resolution;
    public static ArrayList<Note> notes;

    public MidiFileReader(String path) throws InvalidMidiDataException, IOException {
        this.path = path;
        this.resolution = getResolution(path);
        this.tickLength = getTickLength(path);
        this.notes = readMidiFile(path);
    }

    public static ArrayList<Note> readMidiFile(String path) throws InvalidMidiDataException, IOException {
        //read midi file in
        Sequence sequence = MidiSystem.getSequence(new File(path));

        ArrayList<Note> noteArrayList = new ArrayList<>();
        //loop through each track
        for(Track track : sequence.getTracks()){


            //loop through each track event
            for(int i=0; i<track.size();i++){
                MidiEvent event = track.get(i);


                MidiMessage message = event.getMessage();
                if(message instanceof ShortMessage){
                    //print info including the tick, event type and note information
                    //System.out.print("Tick: " + event.getTick()+" - ");
                    ShortMessage sm = (ShortMessage) message;
                    //System.out.print("Channel: " + sm.getChannel() + " - ");
                    if (sm.getCommand() == NOTE_ON) { //print data for note on
                        int key = sm.getData1();
                        int octave = (key / 12)-1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];

                        Note tempNote = new Note(event.getTick(),noteName,octave);

                        noteArrayList.add(tempNote);

                        //System.out.println("Note added, " + noteName + octave + " key=" + key);
                    }
                }


            }

        }
        return noteArrayList;
    }

    public static long getTickLength(String path) throws InvalidMidiDataException, IOException {
        Sequence sequence = MidiSystem.getSequence(new File(path));
        return sequence.getTickLength();
    };
    public static int getResolution(String path) throws InvalidMidiDataException, IOException {
        Sequence sequence = MidiSystem.getSequence(new File(path));
        return sequence.getResolution();
    }
    public static void main(String[] args) throws Exception{
        String path = "new.mid";

        MidiFileReader midiFileReader = new MidiFileReader(path);

        for (Note n: notes) {
            System.out.println("NOTE: "+n.fullNoteName + " - TICK: "+n.tick);
        }

        GuitarTab guitarTab = new GuitarTab((int) tickLength);
        guitarTab.generateTab(notes);
        guitarTab.printTab(resolution);


    }

}
