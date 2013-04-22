package applications;
import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import services.TTPConnEndPoint;
import services.TTPServer;



public class FTPServer {

	public static void main(String[] args) {

		System.out.println("Enter port number");
		Scanner read = new Scanner(System.in);
		int port = read.nextInt();

		System.out.println("FTP Server is listening on Port " + port);
		
		TTPServer ttp_server = new TTPServer();
		
		boolean listening = true;
		
		try {
			ttp_server.open(port,10);
			
			while (listening) {
				Thread serviceClient = new Thread(new ProxyFTPServer(ttp_server.acceptConn()));
				serviceClient.start();
				System.out.println("FTP Server continues listening..");
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		
	
	}
}

