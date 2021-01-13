//package Multithreaded;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Client {
	Socket requestSocket;           //socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //capitalized message read from the server
	private FileInputStream fis;
	private DataOutputStream dos;
	private DataInputStream dis;
	private FileOutputStream fos;


	public void Client() {}

	void run(String ServerIP, int ServerPort)
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket(ServerIP, ServerPort);
			System.out.println("Connected to localhost in port 8000");
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			//login procedure
			login();
			//after login
			while(true)
			{
				System.out.println("Please input one of the following command: dir, get, upload, exit");
				message = bufferedReader.readLine();
				sendMessage(message);
				//detail process
				if(message.equals("dir")){
					MESSAGE = (String)in.readObject();
					System.out.println("Receive message: \n" + MESSAGE);
				}
				else if(message.length()>6 && message.substring(0, 6).equals("upload")){
					sendFile();
				}
				else if(message.length()>3 && message.substring(0, 3).equals("get")){
					receiveFile();
				}
				else if(message.equals("exit")){
					break;
				}
				else{
					MESSAGE = (String)in.readObject();
//					System.out.println("Receive message: \n" + MESSAGE);
					System.out.println("Wrong Command!");
				}

			}
		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
            		System.err.println("Class not found");
        	} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
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
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	// login
	public void login(){
		try{
			while(true){
				System.out.println("Hello, please input a your username and password: ");
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
				message = bufferedReader.readLine();
//				message = "aaa 111";
				sendMessage(message);
				//Receive the upperCase sentence from the server
				MESSAGE = (String)in.readObject();
				//show the message to the user
				if(MESSAGE.equals("Login OK")){
					System.out.println(MESSAGE);
					break;
				}
				else{
					System.out.println(MESSAGE);
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public void receiveFile(){
		try{
			MESSAGE = (String)in.readObject();
			// if file not exist, return;
			if(MESSAGE.equals("file not exist")){
				System.out.println(MESSAGE);
				return;
			}
			// file exist, receive the file
			//1. receive the filename and filesize
			dis = new DataInputStream(this.requestSocket.getInputStream());
			String filename = dis.readUTF();
			long fileLength = dis.readLong();
			long progress = 0;
			File file = new File(filename);
			fos = new FileOutputStream(file);

			// receive the real file
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

	void sendFile(){
		try{

			String filename = "./"+message.split(" ")[1];
			File file = new File(filename);

			if(file.exists()){
				// 1. send filename and filesize
				sendMessage("file exist");
				fis = new FileInputStream(file);
				dos = new DataOutputStream(requestSocket.getOutputStream());
				dos.writeUTF(filename);
				System.out.println(filename);
				dos.flush();
				dos.writeLong(file.length());
				dos.flush();
				System.out.println(file.length());

				//2. send file
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
					sendMessage("file not exist");
					System.out.println("File not exist");
				}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	//main method
	public static void main(String args[]) throws IOException {
		// input IP and Port
		String ServerIP;
		String ServerPort;
		while(true){
			System.out.println("Please input the \"ftpclient [ServerIP] [ServerPort]\"");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			String[] read = bufferedReader.readLine().split(" ");
			if(read.length<3) continue;
			String ftpclient = read[0];
			ServerIP = read[1];
			ServerPort = read[2];
			if(ftpclient.equals("ftpclient") && ServerIP.equals("127.0.0.1") && ServerPort.equals("8000")) break;
		}

		// begin Client
		Client client = new Client();
		client.run(ServerIP, Integer.parseInt(ServerPort));
	}

}
