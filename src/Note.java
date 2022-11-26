public class Note {
    public Note(long tick, String note, int noteOctave) {
        this.tick = tick;
        this.note = note;
        this.noteOctave = noteOctave;
        this.fullNoteName = note+noteOctave;
    }

    public void setNoteOctave(int noteOctave) {
        this.noteOctave = noteOctave;
        this.fullNoteName = note+ noteOctave;
    }

    public void setNote(String note) {
        this.note = note;
        this.fullNoteName = note+ noteOctave;
    }

    long tick;
    String note;
    int noteOctave;
    String fullNoteName;





}
