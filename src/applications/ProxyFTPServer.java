package applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import services.TTPServer;

public class ProxyFTPServer implements Runnable {
	private TTPServer ttp;
	private byte[] data;

	public ProxyFTPServer(TTPServer ttp, byte[] data) {
		super();
		this.ttp = ttp;
		this.data = data;
	}

	@Override
	public void run() {
		System.out.println("Servicing FTP Client...");
		try {
			byte[] temp = new byte[data.length - 5];
			System.arraycopy(data, 5, temp, 0, data.length-5);
			
			String fileName = new String(temp,"US-ASCII");

			System.out.println("File Requested:" + fileName);

			byte[] clientInfo = new byte[5];
			System.arraycopy(data, 0, clientInfo, 0, 5);

			File file = new File(fileName.toString());
			FileInputStream fs = new FileInputStream(file);
			byte[] fileData = new byte[(int)file.length()];

			fs.read(fileData, 0, (int)file.length());

			fs.close();
			System.out.println("Will send requested file shortly..!");
			
			byte[] totalData = new byte[(int)file.length() + 5];
			System.arraycopy(clientInfo, 0, totalData, 0, 5);
			System.arraycopy(fileData, 0, totalData, 5, (int)file.length());

			ttp.send(totalData);
			System.out.println("File Sent!");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}





	}

}
