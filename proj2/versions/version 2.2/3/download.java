import javax.swing.*;
import java.net.ConnectException;
import java.util.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class download extends Thread {
    private Socket downSocket;
    public static Boolean[] filelist;
    ObjectOutputStream out;         //stream write to the socket
    ObjectInputStream in;          //stream read from the socket
    String message;                //message send to the server
    String MESSAGE;                //capitalized message read from the server
    public static String testFileName;
    private FileInputStream fis;
    private DataOutputStream dos;
    private DataInputStream dis;
    private FileOutputStream fos;

    public download(Socket downSocket, Boolean[] filelist, String testFileName){
        this.downSocket = downSocket;
        this.filelist = filelist;
        this.testFileName = testFileName;
        println("[download] total file Num: " + filelist.length);
    }

    public void run(){
        try{
            /**
             * main body for downloading
             * download the missing part from the downing neighbor
             */
            println("[download] download neighbor connected");
            createSummaryFile();
            out = new ObjectOutputStream(downSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(downSocket.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            sendMessage("[download] hello download neighbor");
            while(!fileComplete()){
                println("[download] ===========================================");
                println("[download] now the file is still not complete, we need to query them from download neighbor");
                // Thread.sleep(500);
                // request filelist
                println("[download] request filelist from the download neighbor");
                sendMessage("filelist");
                Boolean[] hislist = Str2filelist(receiveMessage());
//                println("the filelist is: "+ receiveMessage());
                int reqFile = filecompare(filelist, hislist);

                if(reqFile==-1) continue;
                // request a missing file
                println("[download] the file we need to download from download neighbor is [" + reqFile+"]");
                sendMessage(reqFile+"");
                receiveFile();
                filelist[reqFile] = true;
                createSummaryFile();
                println("[download] now our filelist is: "+ filelist2String());
                println("[download] ===========================================");
                println(" ");
            }

            // File[] realfilelist = new File[filelist.length];
            // for(int j = 0; j < filelist.length; j++){
            //     realfilelist[j] = new File(j + "");
            // }
            
            // TODO
            reassemble(filelist, new File(testFileName));

            close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    void createSummaryFile() throws Exception{
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

    void close() throws Exception{
        sendMessage("close");
        println("[download] download Thread closed");
        if(fis!=null) fis.close();
        if(dos!=null) dos.close();
        if(dis!=null) dis.close();
        if(fos!=null) fos.close();
        if(in!=null)  in.close();
        if(out!=null) out.close();
        downSocket.close();
        Thread.interrupted();
    }
    String filelist2String(){
        String res = "";
        for(int i=0; i<filelist.length; i++){
            res += (filelist[i]==true)?"1":"0";
        }
        return res;
    }

    int filecompare(Boolean[] my, Boolean[] his){
        for(int i=0; i<my.length; i++){
            if(my[i]==false && his[i]==true) return i;
        }
        return -1;
    }

    String receiveMessage() throws Exception{
        message = (String)in.readObject();
        println("[download] receive message: "+message);
        return message;
    }

    Boolean[] Str2filelist(String message){
        Boolean[] res = new Boolean[filelist.length];
        for(int i=0; i<filelist.length; i++){
            res[i] = (message.charAt(i)=='1')?true:false;
        }
        return res;
    }

    void sendMessage(String msg)
    {
        try{
            //stream write the message
            out.writeObject(msg);
            out.flush();
            println("[download] sent message: "+msg);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    public boolean fileComplete(){
        for(int i=0; i<filelist.length; i++){
            if(filelist[i]==false) return false;
        }
        println("[download] now the file is complete, we are about to close");
        return true;
    }

    
    public static void reassemble(Boolean[] filelist, File mergeFile) throws IOException{

            String[] fpaths = new String[filelist.length];
            File[] realfilelist = new File[filelist.length];
            for(int j = 0; j < filelist.length; j++){
                fpaths[j] = j + "";
                realfilelist[j] = new File(fpaths[j]);
            }

            
            int totalreadcount = 0;

            try{
                // int bufSize = 100*1024;
                int bufSize = 1024;
                byte[] buffer = new byte[bufSize];
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(mergeFile));
                for (int i = 0; i < fpaths.length; i ++) {
                    System.out.println("woide: " + i);
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(realfilelist[i]));
                    int readcount;
                    while ((readcount = inputStream.read(buffer)) > 0) {
                        totalreadcount++;
                        byte[] buffer1 = new byte[readcount];
                        for (int j = 0; j < readcount; j++)
                            buffer1[j] = buffer[j];
                        outputStream.write(buffer1, 0, readcount);
                    }
                    System.out.println(totalreadcount);
                    inputStream.close();
                }
                outputStream.close();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
    }

    private static void print(String s){
        System.out.print(s);
    }
    private static void println(String s){
        System.out.println(s);
    }

    public void receiveFile(){
        try{
            MESSAGE = (String)in.readObject();
            // if file not exist, return;
            if(MESSAGE.equals("[download] file not exist")){
                println(MESSAGE);
                return;
            }
            // file exist, receive the file
            println("[download] file exist, begin file receive");
            println("[download] ======File Receive Begin======");
            //1. receive the filename and filesize
            dis = new DataInputStream(this.downSocket.getInputStream());
            String filename = dis.readUTF();
            long fileLength = dis.readLong();
            long progress = 0;
            File file = new File(filename);
            fos = new FileOutputStream(file);

            // receive the real file
            byte[] bytes = new byte[1024];
            println("[download] receiving filename: [" + filename + "]");
            println("[download] receiving file length: " + fileLength);

            while(true){
                int length = 0;
                if(dis!=null) {
                    length = dis.read(bytes);
                }
                progress += length;
                fos.write(bytes, 0, length);
                fos.flush();

                long percent = (100*progress/Math.max(fileLength, 1));
                if(percent%50==0) println("[download] receive status: "+percent+"%");
                if(length==-1 || progress==fileLength) break;
            }
            println("[download] ======File Receive success======");
//            println("received filename: "+filename);
//            println("received filesize: "+fileLength);
        }
        catch(Exception e){
            System.out.println("error");
            e.printStackTrace();
        }
    }
}