package applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import services.TTPConnEndPoint;

public class ProxyFTPServer implements Runnable {
	private TTPConnEndPoint client_endpoint;
		
	public ProxyFTPServer(TTPConnEndPoint client_endpoint) {
		super();
		this.client_endpoint = client_endpoint;
	}

	@Override
	public void run() {
		byte[] data;
		String fileName = null;
		try {
			data = client_endpoint.receiveData();
			
			if (data!=null) {
				fileName = data.toString();
				System.out.println("File Requested:" + fileName);
				
				File file = new File(fileName);
				FileInputStream fs = new FileInputStream(file);
				byte[] fileData = new byte[(int)file.length()];
				fs.read(fileData, 0, (int)file.length());
				fs.close();
				
				client_endpoint.sendData(fileData);
				System.out.println("File Sent!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		
		

	}

}
