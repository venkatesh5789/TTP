package applications;
import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import services.TTPConnEndPoint;
import services.TTPServer;



public class FTPServer {

	public static void main(String[] args) {

		System.out.println("FTP Server is listening on Port 2221");

		TTPServer ttp_server = new TTPServer();

		boolean listening = true;

		try {
			ttp_server.open(2221,10);

			while (listening) {
				byte[] request = ttp_server.receive();
				if (request != null) {
					System.out.println("FTP Server received file request!");
					Thread serviceClient = new Thread(new ProxyFTPServer(ttp_server,request));
					serviceClient.start();
				}
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

