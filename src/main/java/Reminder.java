import java.util.Date;

public class Reminder {
    private String text;
    private Date time;

    public Reminder(String text, Date time) {
        this.text = text;
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public Date getTime() {
        return time;
    }
}
