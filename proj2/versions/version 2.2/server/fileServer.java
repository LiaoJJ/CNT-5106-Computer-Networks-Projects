import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.rmi.ServerError;
import java.util.*;
import java.nio.file.Files;


public class fileServer {
    private static final int sPort = 5000;
    // current directory
    private static String dir;
    private static File folder;
    private static List<File> group;
    private static int totalFileSize;
    

    public static void main(String[] args) throws Exception {
        group = new ArrayList<>();
        File file = new File(args[1]);
        String testFileName = file.getName();
        splitFile(file);
        // mergeFile(group);
        ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));


        int index = 0;// group index
        for(int i = 0; i < 5; i++){// thread index
            List<File> listOfFile = new ArrayList<>();
            Socket socket = listener.accept();
            for(int j = 1; j <= group.size()/5; j ++){
                listOfFile.add(group.get(index++));
            }
            while (i == 4 && index < group.size()) {
                listOfFile.add(group.get(index++));
            }
            Handler thread = new Handler(socket,listOfFile, totalFileSize, testFileName);
            thread.start();
        }
        Thread.sleep(5000);
        listener.close();
    }
    public static void splitFile(File f) throws Exception {
        int partCounter = 0;

        int sizeOfFiles = 1024;// 1KB
        int numOfChunk = (int)Math.ceil(f.length()/(sizeOfFiles*100));
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = "";
        long fileSize = f.length();
        // System.out.println(fileSize);

        //try-with-resources to ensure closing stream
        FileInputStream fis = new FileInputStream(f);
            //&& bytesAmount < fileSize
        for (int a = 0; a <= numOfChunk; a++) {
                //write each chunk of data into separate file with different number in name
            String filePartName = fileName + "" + a;
            File newFile = new File(filePartName);
            FileOutputStream ois = new FileOutputStream(newFile);
            for (int i = 0; i < 100; i++) {
                int bytesAmount = fis.read(buffer);
                if (bytesAmount == -1)
                    break;
                byte[] buffer1 = new byte[bytesAmount];
                for (int j = 0; j < bytesAmount; j++) {
                    buffer1[j] = buffer[j];
                }
                ois.write(buffer1);
            }      
            System.out.println(filePartName);
            group.add(newFile);
            ois.close();
        }
        totalFileSize = numOfChunk + 1;
    }
}



class Handler extends Thread
{
    private String message;// 从client传来的信息
    private String MESSAGE;// 准备发送给client的信息
    private Socket connection;
    private ObjectInputStream in;// client -> server
    private ObjectOutputStream out;// sever -> client
    private int No;// 客户的编号
    // private String dir;
    boolean login = false;
    List<File> listOfFile = new ArrayList<>();
    String testFileName;
    //      Connect c = new Connect();
    int totalFileSize;
    public Handler(Socket connection,List<File> listOfFile, int fileSize, String testFileName)
    {
        this.connection = connection;
        this.listOfFile = listOfFile;
        this.totalFileSize = fileSize;
        this.testFileName = testFileName;
    }
// 建立一个小的数据库， 存储账号和密码

    public void run() {
        try {
            for (File file : listOfFile){
                System.out.println(file.getName());
            }
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());
            try {
                // infoCheck();
                sendMessage(totalFileSize+"");
                sendMessage(listOfFile.size() + "");
                sendMessage(testFileName);
                System.out.println("==============finished=====================");
                sendFile(listOfFile);
            } catch (Exception e) {
                e.printStackTrace();
            }

//              checkDirectory();

        } catch (IOException e) {
            System.err.println("Disconnect with Peer " + No);
        }
        finally {
            try
            {
                in.close();
                out.close();
                connection.close();
            }
            catch (IOException e)
            {
                System.out.println("Disconnect with Peer " + No);
            }
        }
    }

    public void sendFile(List<File> f) throws IOException{
        for(int i = 0; i < listOfFile.size(); i ++){
            // FileInputStream fileOutput = new FileInputStream(listOfFile.get(i));
            byte[] test = Files.readAllBytes(listOfFile.get(i).toPath());
            out.writeObject(test);
            out.writeObject(listOfFile.get(i).getName());
            out.flush();
            
        }
        sendMessage("chunks passed by fileOwner");
    }


    private void sendMessage(String meg) {
        try
        {
            out.writeObject(meg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
