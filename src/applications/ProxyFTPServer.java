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
		//Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		System.out.println("Servicing FTP Client...");
		try {
			byte[] temp = new byte[data.length - 6];
			System.arraycopy(data, 6, temp, 0, data.length-6);
			
			String fileName = new String(temp,"US-ASCII");

			System.out.println("File Requested:" + fileName);

			byte[] clientInfo = new byte[6];
			System.arraycopy(data, 0, clientInfo, 0, 6);

			File file = new File(fileName.toString());
			FileInputStream fs = new FileInputStream(file);
			byte[] fileData = new byte[(int)file.length()];

			fs.read(fileData, 0, (int)file.length());

			fs.close();
			System.out.println("Will send requested file shortly..!");
			
			byte[] totalData = new byte[(int)file.length() + 6];
			System.arraycopy(clientInfo, 0, totalData, 0, 6);
			System.arraycopy(fileData, 0, totalData, 6, (int)file.length());

			ttp.send(totalData);
			System.out.println("File Sent!");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}





	}

}
