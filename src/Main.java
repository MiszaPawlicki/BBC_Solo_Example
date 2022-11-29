import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws InvalidMidiDataException, IOException {
        //initialise genetic algorithm object and run
        GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm();
        geneticAlgorithm.runGeneticAlgorithm("new.mid");
    }
}