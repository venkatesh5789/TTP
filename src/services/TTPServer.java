package services;


import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

import datatypes.Datagram;

public class TTPServer {
	private DatagramService ds;
	
	public void listen(int srcPort, int verbose) throws SocketException {
		ds = new DatagramService(srcPort, verbose);
	}
	
	public TTPClient acceptConn() throws IOException, ClassNotFoundException {
		Datagram request = ds.receiveDatagram(); 
		byte[] data = (byte[]) request.getData();
		TTPClient server_ttp = null;
		
			if (data[8] == (byte)4) {
				server_ttp = new TTPClient();
				int acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
				server_ttp.setAcknNum(acknNum) ;
				server_ttp.setExpectedSeqNum(acknNum + 1);				
				server_ttp.open(request.getDstaddr(), request.getSrcaddr(), (short)new DatagramSocket().getPort(), request.getSrcport(), 10, TTPClient.SYNACK);
				System.out.println("Received SYN from:" + request.getSrcaddr() + ":" + request.getSrcport());
			}
			
			return server_ttp;	
	}	
	public static int byteArrayToInt(byte[] b) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}
		return value;
	}

	public static byte[] intToByteArray(int a) {
		byte[] ret = new byte[4];
		ret[0] = (byte) (a & 0xFF);
		ret[1] = (byte) ((a >> 8) & 0xFF);
		ret[2] = (byte) ((a >> 16) & 0xFF);
		ret[3] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}
}
