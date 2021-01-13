import javax.swing.*;
import java.net.ConnectException;
import java.util.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class upload extends Thread {
    private Socket upSocket;
    public static Boolean[] filelist;
    private String message;    //message received from the client
    private String MESSAGE;    //uppercase message send to the client
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
    private DataInputStream dis;
    private FileOutputStream fos;
    private FileInputStream fis;
    private DataOutputStream dos;
    int PORT;

    public upload(Boolean[] filelist, int PORT){
        println("[upload] total file Num:" + filelist.length);
        this.filelist = filelist;
        this.PORT = PORT;
    }

    public void run(){
        // always trying to connect to the upload neighbor
        while(true){
            try {
                Thread.sleep(500);
                upSocket = new Socket("127.0.0.1", PORT);
                println("[upload] upload neighbor connected");
                break;
            }
            catch (Exception e){
//                println(e+"");
//                println("Exception");
            }
        }
        // connected
        try{
            /*
             * main part of uploading file
             * compare list
             * upload missing file
             */
            out = new ObjectOutputStream(upSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(upSocket.getInputStream());

            while(true) {
                println("[upload] =======================================");
                message = receiveMessage();
                // send filelist
                if (message.equals("filelist")) {
                    println("[upload] my current file list is:" + filelist2String());
                    sendMessage(filelist2String());
                }
                // send specific file
                else if(message.charAt(0)>='0' && message.charAt(0)<='9'){
                    sendFile(message);
                }
                else if(message.equals("close")){
                    break;
                }
                else{
                    // do nothing
                }
                Thread.sleep(500);
                println("[upload] ======================================");
                println(" ");
            }
            close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    String filelist2String(){
        String res = "";
        for(int i=0; i<filelist.length; i++){
            res += (filelist[i]==true)?"1":"0";
        }
        return res;
    }

    private static void print(String s){
        System.out.print(s);
    }
    private static void println(String s){
        System.out.println(s);
    }

    void close() throws Exception{
        println("[upload] upload Thread closed");
        if(fis!=null) fis.close();
        if(dos!=null) dos.close();
        if(dis!=null) dis.close();
        if(fos!=null) fos.close();
        if(in!=null)  in.close();
        if(out!=null) out.close();
        upSocket.close();
        Thread.interrupted();
    }
    public String receiveMessage() throws Exception{
        message = (String)in.readObject();
        println("[upload] received message from download neighbor: " + message);
        return message;
    }
    public void sendMessage(String msg) {
        try{
            out.writeObject(msg);
            out.flush();
            println("[upload] send message to download neighbor: "+msg);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    //send file
    void sendFile(String filename){
        try{
            File file = new File("./"+filename);
            // if file exist
            if(file.exists()){
                //1. send filename and filesize
                sendMessage("[upload] file exist");
                fis = new FileInputStream(file);
                dos = new DataOutputStream(this.upSocket.getOutputStream());
                dos.writeUTF(filename);
                println("[upload] Sending file ["+filename+"] to upload neighbor");
                dos.flush();
                dos.writeLong(file.length());
                dos.flush();
                println("[upload] file length: "+file.length());

                //2.send real file
                println("[upload] =========begin transfer============");
                byte[] bytes = new byte[1024];
                long progress = 0;
                while(true){
                    int length = fis.read(bytes, 0, bytes.length);
                    if(length==-1) break;
                    dos.write(bytes, 0, length);

                    progress += length;
                    long percent = (100*progress/Math.max(file.length(), 1));
                    if(percent%50==0) println("[upload] send status: "+(100*progress/Math.max(file.length(), 1))+"%");
                }
                dos.flush();
                println("[upload] =========end transfer============");
            }
            else{
                //if file not exist
                sendMessage("[upload] file not exist");
                println("[upload] File not exist");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}