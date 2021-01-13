import javax.swing.*;
import java.net.ConnectException;
import java.util.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


public class peer {
    public static Boolean[] filelist;
    static int PORT = 5000;
    public static String testFileName;
    Socket requestSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    String message;
    String MESSAGE;
    boolean login = false;
    String dir;
    File folder;
    public static int totalFileSize;
    public void run() {
        try
        {
            authorization();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void authorization ()
    {
        try
        {
            dir = new File(".").getCanonicalPath();
            folder = new File(dir);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            String address = "";
            int port = 0;
//            System.out.print("Please enter a address : ");
//            address = bufferedReader.readLine();
//            System.out.print("Please enter a port : ");
//            port = Integer.parseInt(bufferedReader.readLine());
            requestSocket = new Socket("127.0.0.1",PORT);

            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());
            totalFileSize = Integer.parseInt((String) in.readObject());
            println("the total file size is " + totalFileSize);
            filelist = new Boolean[totalFileSize];
            for(int i=0; i<totalFileSize; i++) filelist[i] = false;
            int number = Integer.parseInt((String) in.readObject());
            testFileName = (String) in.readObject();
            while(number-- > 0)
            {
                Object temp = in.readObject();// 从 server 传来的信息
                System.out.println("dfghdgjfhkgjh");
                if (temp instanceof File) {
                    File newFile = (File) temp;
                } else if (temp instanceof String) {
                    MESSAGE = (String) temp;
                    System.out.println(MESSAGE);
                } else if (temp instanceof byte[]) { // byte[]
                    String fileName = (String) in.readObject();
                    filelist[Integer.parseInt(fileName)] = true;
                    File test = new File(dir + "/" + fileName);
                    OutputStream fos = new FileOutputStream(test);
                    fos.write((byte[])temp);
                    fos.close();
                    System.out.println("Getting " + fileName);
                } else {
                    System.out.println("???");
                }

            }
        }
        catch(Exception e)
        {
            System.err.println("Connection refused. You need to initiate a server first");
        }

        try
        {
            if (!(in == null)) {
                in.close();
            }
            if (!(out == null)) {
                out.close();
            }
            if (!(requestSocket == null)) {
                requestSocket.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    void sendMessage(String meg)
    {
        try {
            out.writeObject(meg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // e.g. {0:trur},..., {4:false}


    public static void main(String[] args) throws Exception{
        // connect to the server and get chunks
        PORT = Integer.parseInt(args[0]);
        peer client = new peer();
        client.run();

        init();
        createSummaryFile();
        Thread.sleep(500);
        // keep listening to a port, waiting for peers to connect, upload file to peers
        new upload(filelist, Integer.parseInt(args[2])).start();
        // download file from a specific peer
        new listening(filelist, Integer.parseInt(args[1]), testFileName).start();
        println("main thread exit");
        // while(true);
    }

    static void createSummaryFile() throws Exception{
        String out = "";
        for(int i=0; i<filelist.length; i++){
            out += i+" ";
            out += filelist[i]?"exist":"not exist";
            out += "\n";
        }
        File fileName = new File("./SummaryFile.txt");
//        fileName.createNewFile(); //
        BufferedWriter ooo = new BufferedWriter(new FileWriter(fileName));
        ooo.write(out);
        ooo.flush();
        ooo.close();
        println("SummaryFile.txt is created");
    }

    public static void init(){
       // filelist = new Boolean[totalFileSize];
       // for(int i=0; i<totalFileSize; i++){
       //     filelist[i] = false;
       // }
    }
    private static void print(String s){
        System.out.print(s);
    }
    private static void println(String s){
        System.out.println(s);
    }
}


class listening extends Thread{
    private Socket downSocket;
    private ServerSocket listener;
    public static Boolean[] filelist;
    public static String testFileName;

    public listening(Boolean[] filelist, int port, String testFileName) throws Exception{
        listener = new ServerSocket(port);
        this.filelist = filelist;
        this.testFileName = testFileName;
        println("listening...");
    }
    public void run(){
        try{
            // while(true){
            new download(listener.accept(), filelist, testFileName).start();
            println("download neighbor establishing.....");
            // }
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }
    private static void print(String s){
        System.out.print(s);
    }
    private static void println(String s){
        System.out.println(s);
    }
}
