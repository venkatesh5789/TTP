package applications;

import java.io.IOException;
import java.util.Scanner;

import services.TTPClient;

public class FTPClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("Enter server port");
		Scanner read = new Scanner(System.in);
		int dstPort = read.nextInt();

		System.out.println("Enter local port");
		int srcPort = read.nextInt();
		
		System.out.println("Enter file name");
		Scanner readfile = new Scanner(System.in);
		String fileName = readfile.nextLine();

		TTPClient client = new TTPClient();
		try {
			client.open("127.0.0.1", "127.0.0.1", (short)srcPort, (short)dstPort, 10, TTPClient.SYN, null);
			client.sendData(fileName.getBytes());
			
			boolean listening = true;
			while (listening) {
				byte[] data = client.receiveData();
				for(byte b:data) {
					System.out.println(b);
				}
			}			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

}
