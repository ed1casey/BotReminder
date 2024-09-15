public class UserState {
    private State state;
    private String reminderText;
    private String reminderTime;

    public enum State {
        ASKING_TEXT,
        ASKING_TIME,
        DELETING,
        NONE
    }


    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getReminderText() {
        return reminderText;
    }

    public void setReminderText(String reminderText) {
        this.reminderText = reminderText;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }
}
