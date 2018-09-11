import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {

    private String login;
    private String rootDir = "D:\\GeekUniversity\\MyDropbox\\ServerStorage";
    private String userDir;

    public String getLogin() {
        return login;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
        // Send greeting for a new connection.
        // ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
        // ctx.write("It is " + new Date() + " now.\r\n");
        // ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;
            if (msg instanceof AbstractMessage) {
                processMsg((AbstractMessage)msg, ctx);
            } else {
                System.out.printf("Server received wrong object!");
                return;
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //ctx.flush();
        ctx.close();
    }

    private void processMsg(AbstractMessage msg, ChannelHandlerContext ctx) {
        if (msg instanceof AuthMessage) {
            String newLogin = DataBaseService.getLoginAndCheckPass(((AuthMessage) msg).getLogin(),
                    ((AuthMessage) msg).getPassword());
            if (newLogin != null) {
//                    if (!server.isLoginBusy(newLogin)) {
//
//                        }
                login = newLogin;
                checkUserDirInCloud(login);
                CommandMessage commandMessage = new CommandMessage(Command.AUTH_OK, login);
                ctx.write(commandMessage);
                ctx.flush();
                System.out.println(login + " authorized successfully");
            } else {
                CommandMessage commandMessage = new CommandMessage(Command.AUTH_ERROR);
                ctx.writeAndFlush(commandMessage);
            }
        } else if (msg instanceof FileMessage) {
            saveFileToCloud((FileMessage) msg);
        } else if (msg instanceof CommandMessage) {
            System.out.println("Server received command " + ((CommandMessage) msg).getCmd());
            processCommand((CommandMessage) msg, ctx);
        }
    }


    private void checkUserDirInCloud(String user) {
        Path path = Paths.get(rootDir + "\\" + user);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        userDir = path.toString();
    }

    /**
     * Метод обработки приходящих от клиента команд
     * @param msg
     * @param ctx
     */

    private void processCommand(CommandMessage msg, ChannelHandlerContext ctx) {
        switch (msg.getCmd()) {
            case FILE_LIST:
                ctx.writeAndFlush(new FileListMessage(getUserFileList()));
                break;
            case DELETE_FILE:
                deleteFileFromCloud((String) msg.getObjects()[0]);
                break;
            case DOWNLOAD_FILE:
                Path path = Paths.get(userDir + "\\" + (String) msg.getObjects()[0]);
                FilePartitionWorker.sendFileToClient(path, ctx.channel());

                // старая отсылка файла целиком
//                try {
//                    //ctx.writeAndFlush(new FileMessage(path)); - это первый вариант, файл шлется целиком
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                break;
            case CREATE_DIR:
                // TODO нужно реализовать
            default:
                System.out.println("Invalid command!");

        }
    }

    private List<String> getUserFileList() {
        List<String> fileList = new ArrayList<>();
        fileList.add(0, "..");
        try {
            Files.newDirectoryStream(Paths.get(userDir)).forEach(path -> fileList.add(path.getFileName().toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileList;
    }

    private void deleteFileFromCloud(String fileName) {
        try {
            Files.delete(Paths.get(userDir + "\\" + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFileToCloud(FileMessage msg) {
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

            if (msg.getPartNumber() == msg.getPartsCount()) {
                System.out.println("Server save file in cloud - " + ((FileMessage) msg).getFileName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
