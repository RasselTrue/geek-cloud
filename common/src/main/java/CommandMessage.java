public class CommandMessage extends AbstractMessage {
    private Command cmd;
    private Object[] objects;

    public CommandMessage(Command cmd, Object... objects) {
        this.cmd = cmd;
        this.objects = objects;
    }

    public Command getCmd() {
        return cmd;
    }

    public Object[] getObjects() {
        return objects;
    }
}
