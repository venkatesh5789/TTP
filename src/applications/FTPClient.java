package applications;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import services.TTPConnEndPoint;

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
		String path = System.getProperty("user.dir") + "/ClientFiles/";

		TTPConnEndPoint client = new TTPConnEndPoint();
		try {
			client.open("127.0.0.1", "127.0.0.1", (short)srcPort, (short)dstPort, 10);
			client.sendData(fileName.getBytes());

			boolean listening = true;
			while (listening) {
				byte[] data = client.receiveData();

				if (data!=null) {
					System.out.println("Received file");
					File f = new File(path + fileName);
					f.createNewFile();
					FileOutputStream fs = new FileOutputStream(f);
				    BufferedOutputStream bs = new BufferedOutputStream(fs);
				    bs.write(data);
				    bs.close();
				    bs = null;
				}
			}			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

}
