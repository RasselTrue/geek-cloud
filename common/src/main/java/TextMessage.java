import java.io.Serializable;

public class TextMessage extends AbstractMessage {

    private String text;

    public String getText() {
        return text;
    }

    public TextMessage(String text) {
        this.text = text;
    }
}
