import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    private String fileName;
    private byte[] data;
    private int partsCount;
    private int partNumber;

    public int getPartsCount() {
        return partsCount;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }

    public FileMessage(Path path) throws IOException {
        this.fileName = path.getFileName().toString();
        this.data = Files.readAllBytes(path);
    }

    public FileMessage(String fileName, byte[] data, int partsCount, int partNumber) {
        this.fileName = fileName;
        this.partNumber = partNumber;
        this.partsCount = partsCount;
        this.data = data;
    }
}
