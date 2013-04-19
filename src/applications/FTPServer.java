package applications;

import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

import services.TTP;

public class FTPServer {

	public static void main(String[] args) {

		System.out.println("Enter port number");
		Scanner read = new Scanner(System.in);
		int port = read.nextInt();

		System.out.println("FTP Server is listening on Port " + port);

		try {	
			TTP ttp = new TTP();
			ttp.open(port,10);

			boolean listening = true;
			String fileName = null;

			while (listening) {
				byte[] fileRequest = ttp.receiveData();
				if (fileRequest != null) {
					fileName = fileRequest.toString();
				}
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
