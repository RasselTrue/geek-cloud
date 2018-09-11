import java.util.List;

public class FileListMessage extends AbstractMessage{
    private List<String> fileList;

    public FileListMessage(List<String> fileList) {
        this.fileList = fileList;
    }

    public List<String> getFileList() {
        return fileList;
    }
}
