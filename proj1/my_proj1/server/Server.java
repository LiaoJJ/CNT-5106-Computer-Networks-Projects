//package Multithreaded;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Server {

	private static final int sPort = 8000;   //The server will be listening on this port number
	private static HashMap<String, String> map =  new HashMap<>();

	public static void main(String[] args) throws Exception {
		geneMap();
		System.out.println("The server is running."); 
		ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
		try {
			while(true) {
				new Handler(listener.accept(), clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
			}
		}
		finally {
				listener.close();
		}
 
	}

	// use a HashMap to store username and password
    private static void geneMap(){
        map.put("aaa", "111");
        map.put("bbb", "222");
        map.put("ccc", "333");
        map.put("ddd", "444");
        map.put("eee", "555");
        map.put("fff", "666");
        map.put("ggg", "777");
        map.put("hhh", "888");
        map.put("iii", "999");
    }

	/**
     	* A handler thread class.  Handlers are spawned from the listening
     	* loop and are responsible for dealing with a single client's requests.
     	*/
    	private static class Handler extends Thread {
        	private String message;    //message received from the client
			private String MESSAGE;    //uppercase message send to the client
			private Socket connection;
        	private ObjectInputStream in;	//stream read from the socket
        	private ObjectOutputStream out;    //stream write to the socket
			private int no;		//The index number of the client
            private String DIRECTORY = "./";
            // f for file stream; d for data stream
            private DataInputStream dis;
            private FileOutputStream fos;
			private FileInputStream fis;
			private DataOutputStream dos;

        	public Handler(Socket connection, int no) {
        		this.connection = connection;
	    		this.no = no;
        	}

		public void run() {
			try{
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
				try{
					// login process
					login();
					//after login
					while(true) {
						//receive the message sent from the client
						message = (String)in.readObject();
						//show the message to the user
						System.out.println("Receive message: " + message + " from client " + no);

						if(message.equals("dir")){
							File f1 = new File(DIRECTORY);
							String listdir = "";
							for(String temp: f1.list()){
								listdir += temp + "\n";
							}
							sendMessage(listdir);
						}
						else if(message.length()>6 && message.substring(0, 6).equals("upload")) {
							receiveFile();
						}
						else if(message.length()>3 && message.substring(0, 3).equals("get")){
							sendFile();
						}
						else if(message.equals("exit")){
							break;
						}
						else{
							sendMessage("wrong cmd");
						}
					}
				}
				catch(ClassNotFoundException classnot){
						System.err.println("Data received in unknown format");
					}
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
			}
			finally{
					//Close connections
					try{
						if(fis!=null) fis.close();
						if(dos!=null) dos.close();
						if(dis!=null) dis.close();
						if(fos!=null) fos.close();
						in.close();
						out.close();
						connection.close();
					}
					catch(IOException ioException){
						System.out.println("Disconnect with Client " + no);
					}
				}
		}

		//login process
		public void login(){
        		try{
					while(true) {
						//receive the message sent from the client
						message = (String)in.readObject();
						//show the message to the user
						String[] name_pwd = message.split(" ");
						// verify the username and passwd
						if(map.containsKey(name_pwd[0]) && map.get(name_pwd[0]).equals(name_pwd[1])){
							MESSAGE = "Login OK";
							sendMessage(MESSAGE);
							break;
						}
						else{
							MESSAGE = "Username or Password wrong";
							sendMessage(MESSAGE);
						}
					}
				}
        		catch(Exception e){
        			e.printStackTrace();
				}
		}

		// receive file
		public void receiveFile(){
            try{
				MESSAGE = (String)in.readObject();
				// if file not exist, return;
				if(MESSAGE.equals("file not exist")){
					System.out.println(MESSAGE);
					return;
				}
				// if file exist, receive it
				//1. receive filename and filesize
                dis = new DataInputStream(this.connection.getInputStream());
                String filename = dis.readUTF();
                long fileLength = dis.readLong();
                long progress = 0;
                File file = new File(filename);
                fos = new FileOutputStream(file);
				//2.receive real file
                byte[] bytes = new byte[1024];
                System.out.println(filename);
				System.out.println(fileLength);

                while(true){
                	int length = 0;
                	if(dis!=null) {
						length = dis.read(bytes);
					}
                    progress += length;
                    fos.write(bytes, 0, length);
                    fos.flush();

					System.out.println("status: "+(100*progress/Math.max(fileLength, 1))+"%");
					if(length==-1 || progress==fileLength) break;
                }
                System.out.println("======File Receive success======");
                System.out.println("filename: "+filename.substring(2));
                System.out.println("filesize: "+fileLength);
            }
            catch(Exception e){
            	System.out.println("error");
                e.printStackTrace();
            }
        }

        //send file
		void sendFile(){
			try{
				String filename = "./"+message.split(" ")[1];
				File file = new File(filename);
				// if file exist
				if(file.exists()){
					//1. send filename and filesize
					sendMessage("file exist");
					fis = new FileInputStream(file);
					dos = new DataOutputStream(this.connection.getOutputStream());
					dos.writeUTF(filename);
					System.out.println(filename);
					dos.flush();
					dos.writeLong(file.length());
					dos.flush();
					System.out.println(file.length());

					//2.send real file
					System.out.println("=========begin transfer============");
					byte[] bytes = new byte[1024];
					long progress = 0;
					while(true){
						int length = fis.read(bytes, 0, bytes.length);
						if(length==-1) break;
						dos.write(bytes, 0, length);

						progress += length;
						System.out.println("status: "+(100*progress/Math.max(file.length(), 1))+"%");
					}
					dos.flush();
					System.out.println("=========end transfer============");
				}
				else{
					//if file not exist
					sendMessage("file not exist");
					System.out.println("File not exist");
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}

		//send a message to the output stream
		public void sendMessage(String msg) {
			try{
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

    }

}
