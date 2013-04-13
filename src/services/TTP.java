package services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

import datatypes.Datagram;

public class TTP {
	private DatagramService ds;
	
	public void open(int port, int verbose) throws SocketException {
		ds = new DatagramService(port, verbose);
	}
	public void sendData(InetAddress src, InetAddress dest, int srcPort, int destPort, Object data) throws IOException  {
		Datagram datagram = new Datagram();
		datagram.setSrcaddr(src.toString());
		datagram.setDstaddr(dest.toString());
		datagram.setSrcport((short)srcPort);
		datagram.setDstport((short)destPort);
		datagram.setSize((short)0);
		datagram.setData(data);
		datagram.setChecksum(calculateChecksum(datagram));
			
		ds.sendDatagram(datagram);
		System.out.println("Sent datagram");
	}
	public short calculateChecksum(Datagram datagram) {
		return 0;		
	}
}
