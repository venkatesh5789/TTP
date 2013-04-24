package services;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import datatypes.Datagram;

public class TTPServer {
	private DatagramService ds;
	private HashMap<String, TTPConnEndPoint> openConnections= new HashMap<String, TTPConnEndPoint>();
	private LinkedList<byte[]> buffer = new LinkedList<byte[]>();

	public TTPServer() {
		super();
	}

	public void addData(byte[] data) {
		buffer.add(data);
	}

	public void open(int srcPort, int verbose) throws SocketException {
		ds = new DatagramService(srcPort, verbose);
	}

	public byte[] receive() throws IOException, ClassNotFoundException {
		Datagram request = ds.receiveDatagram(); 
		byte[] data = (byte[]) request.getData();
		TTPConnEndPoint server_endPoint = null;
		String sourceKey = request.getSrcaddr() + ":" + request.getSrcport();

		if (data[8] == (byte)4) {
			if(!openConnections.containsKey(sourceKey)) {
				server_endPoint = new TTPConnEndPoint(ds);
				openConnections.put(sourceKey, server_endPoint);
				Thread serviceThread = new Thread(new ServiceClient(server_endPoint,request, this));
				serviceThread.start();
				System.out.println("Received SYN from:" + sourceKey);
			}
			else {
				System.out.println("Connection already exists !");
			}
		} else {
			if(openConnections.containsKey(sourceKey)) {
				System.out.println("Received ACK/DATA from existing client");
				Thread serviceThread = new Thread(new ServiceClient(openConnections.get(sourceKey),request, this));
				serviceThread.start();
			}
		}

		if (!buffer.isEmpty()) {
			System.out.println("Data sent to FTP");
			return buffer.pop();
		} else
			return null;
	}	
	public void send(byte[] data) throws IOException {
		System.out.println("TTP Server received data from FTP");
		StringBuffer key = new StringBuffer();

		for(int i=0;i<4;i++) {
			key.append(data[i]);
		}
		key.append(":" + data[4]);
		byte[] temp = new byte[data.length - 5];
		System.arraycopy(data, 5, temp, 0, data.length - 5);
		Thread serviceThread = new Thread(new ServiceClient(openConnections.get(key),temp));
		serviceThread.start();
	}
}
class ServiceClient implements Runnable {
	private TTPConnEndPoint ttp;
	private Datagram datagram;
	private TTPServer parent;
	private byte[] data;

	public ServiceClient(TTPConnEndPoint ttp, Datagram datagram, TTPServer parent) {
		super();
		this.ttp = ttp;
		this.datagram = datagram;
		this.parent = parent;
	}
	public ServiceClient(TTPConnEndPoint ttp, byte[] data) {
		super();
		this.ttp = ttp;
		this.data = data;
	}

	@Override
	public void run() {
		try {
			if (datagram != null) {
				ttp.respond(datagram,parent);
			} else if (data != null) {
				System.out.println("Sending data to ttp server endpoint...");
				ttp.sendData(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}	
}
