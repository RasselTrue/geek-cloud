import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Controller {

    public HBox authPanel;
    public TextField loginField;
    public PasswordField passField;
    public HBox localActionPanel;
    public HBox cloudActionPanel;
    public Label loginLabel;
    public ListView<String> localListView;
    public ListView<String> cloudListView;
    public ProgressBar operationProgress;

    private String rootDir = "D:\\GeekUniversity\\MyDropbox\\ClientStorage";
    private String userDir;
    private List<String> cloudFileList;

    private boolean isAuthorized;

    private void setAuthorized(boolean authorized) {
        isAuthorized = authorized;
        authPanel.setVisible(!isAuthorized);
        authPanel.setManaged(!isAuthorized);
        localActionPanel.setVisible(isAuthorized);
        localActionPanel.setManaged(isAuthorized);
        cloudActionPanel.setVisible(isAuthorized);
        cloudActionPanel.setManaged(isAuthorized);

        if (isAuthorized) {
            updateLocalFileList();
            requestCloudFileList();
        }
    }



    public void tryAuth(ActionEvent actionEvent) {
        openConnection();
        try {
            AuthMessage authMessage = new AuthMessage(loginField.getText(), passField.getText());
            Network.getInstance().sendData(authMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openConnection() {
        if (!Network.getInstance().isConnected()) {
            try {
                Network.getInstance().connect();
                setAuthorized(false);
                Thread t = new Thread(() -> {
                    try {
                        while (true) {
                            CommandMessage commandMessage = (CommandMessage) Network.getInstance().readData();
                            if (commandMessage.getCmd() == Command.AUTH_OK) {
                                Platform.runLater(() -> {
                                    String login = (String) commandMessage.getObjects()[0];
                                    loginLabel.setText(login);
                                    userDir = rootDir + "\\" + login;
                                    checkUserLocalDir();
                                });
                                setAuthorized(true);
                                break;
                            } else if (commandMessage.getCmd() == Command.AUTH_ERROR) {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "Неверный логин/пароль", ButtonType.OK);
                                    alert.showAndWait();
                                });
                            }
                        }

                        while (true) {
                            Object msg = Network.getInstance().readData();
                            if (msg instanceof FileListMessage) {
                                cloudFileList = ((FileListMessage) msg).getFileList();
                                updateCloudFileList();
                            } else if (msg instanceof FileMessage) {
                                saveFileToLocalStorage((FileMessage)msg);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        Network.getInstance().close();
                    }
                });
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkUserLocalDir() {
        Path path = Paths.get(userDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestCloudFileList() {
        try {
            Network.getInstance().sendData(new CommandMessage(Command.FILE_LIST));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCloudFileList() {
        Platform.runLater(() -> {
            cloudListView.getItems().clear();

            if (cloudFileList.size() > 0) {
                cloudListView.getItems().addAll(cloudFileList);
            } else {
                cloudListView.getItems().add("Пока ваше облачное хранилище пусто");
            }

        });
    }

    private void updateLocalFileList() {
        Platform.runLater(() -> {
            localListView.getItems().clear();
            localListView.getItems().add(0, "..");

            try {
                Files.newDirectoryStream(Paths.get(userDir)).forEach(
                        path -> localListView.getItems().add(path.getFileName().toString())
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private void saveFileToLocalStorage(FileMessage msg) {
        try {
            Path path = Paths.get(userDir + "\\" + msg.getFileName());
            // старый вариант, когда файл приходил целиком
//            if (Files.exists(path)) {
//                Files.write(path, msg.getData(), StandardOpenOption.TRUNCATE_EXISTING);
//            } else {
//                Files.write(path, msg.getData(), StandardOpenOption.CREATE);
//            }

            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
            FileChannel outChannel = raf.getChannel();
            outChannel.position(msg.getPartNumber() * FilePartitionWorker.PART_SIZE);
            ByteBuffer buf = ByteBuffer.allocate(msg.getData().length);
            buf.put(msg.getData());
            buf.flip();
            outChannel.write(buf);
            buf.clear();
            outChannel.close();
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void btnDownloadFileFromCloud(ActionEvent event) {
        String fileName = cloudListView.getItems().get(cloudListView.getFocusModel().getFocusedIndex()).toString();
        try {
            Network.getInstance().sendData(new CommandMessage(Command.DOWNLOAD_FILE, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateLocalFileList();
    }

    @FXML
    private void btnSendFileToCloud(ActionEvent event) {
        String fileName = localListView.getItems().get(localListView.getFocusModel().getFocusedIndex()).toString();
        FilePartitionWorker.sendFileToCloud(Paths.get(userDir + "\\" + fileName), Network.getInstance().getOut(), operationProgress);
        // старая отсылка файла целиком
//        try {
//            Network.getInstance().sendData(new FileMessage(Paths.get(userDir + "\\" + fileName)));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        requestCloudFileList();
    }

    @FXML
    private void btnUpdateLocalStorageView(ActionEvent event) {
        updateLocalFileList();
    }

    @FXML
    private void btnUpdateCloudStorageView(ActionEvent event) {
        requestCloudFileList();
    }

    @FXML
    private void btnDeleteFileFromCloud(ActionEvent event) {
        String fileName = cloudListView.getItems().get(cloudListView.getFocusModel().getFocusedIndex()).toString();
        try {
            Network.getInstance().sendData(new CommandMessage(Command.DELETE_FILE, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestCloudFileList();
    }

    @FXML
    private void btnDeleteFileFromLocalStorage(ActionEvent event) {
        String fileName = localListView.getItems().get(localListView.getFocusModel().getFocusedIndex()).toString();
        try {
            Files.delete(Paths.get(userDir + "\\" + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateLocalFileList();
    }


}
