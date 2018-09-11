import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network {

    final String IP_ADDRESS = "localhost";
    final int PORT = 8189;

    private static Network ourInstance = new Network();

    public static Network getInstance() {
        return ourInstance;
    }

    private Socket socket;
    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;

    public ObjectEncoderOutputStream getOut() {
        return out;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    private Network() {

    }

    public void connect() throws IOException {
        socket = new Socket(IP_ADDRESS, PORT);
        in = new ObjectDecoderInputStream(socket.getInputStream());
        out = new ObjectEncoderOutputStream(socket.getOutputStream());
    }

    public void sendData(Object data) throws IOException{
        out.writeObject(data);
    }

    public Object readData() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
