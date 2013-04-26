package applications;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
		byte[] hashIndicator = "MD5-HASH".getBytes();
		byte[] startBytes = new byte[hashIndicator.length];
		byte[] md5hashRecd = new byte[16];

		TTPConnEndPoint client = new TTPConnEndPoint();
		try {
			client.open("127.0.0.1", "127.0.0.1", (short)srcPort, (short)dstPort, 10);
			client.sendData(fileName.getBytes());

			boolean listening = true;
			while (listening) {
				
				byte[] data = client.receiveData();

				if (data!=null) {
					System.arraycopy(data, 0, startBytes, 0, hashIndicator.length);	
					
					if (Arrays.equals(startBytes, hashIndicator)) {
						System.arraycopy(data, startBytes.length, md5hashRecd, 0, 16);
					} else {
						System.out.println("FTP Client received file!");

						MessageDigest md = MessageDigest.getInstance("MD5");
						byte[] md5HashComputed = md.digest(data);

						if (Arrays.equals(md5HashComputed,md5hashRecd)) {
							System.out.println("MD5 Hash verified!!");
							File f = new File(path + fileName);
							f.createNewFile();
							FileOutputStream fs = new FileOutputStream(f);
							BufferedOutputStream bs = new BufferedOutputStream(fs);
							bs.write(data);
							bs.close();
							bs = null;
						} else {
							System.out.println("Error in file received! MD5 digest does not match!");
						}
						client.close();
					}				
				}
			}			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}

}
