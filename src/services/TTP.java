package services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.zip.*;
import datatypes.Datagram;

public class TTP {
	private DatagramService ds;
	private Datagram datagram;
	private int base;
	private int nextseqnum;
	private int N;
	private int isn;
	private int ackn;
	private Time time;
	private static final int SYN = 0;
	private static final int ACK = 1;
	private static final int FIN = 2;
	private static final int SYNACK = 3;
				
	public TTP() {
		System.out.println("Enter Send/Receive Window");
		Scanner read = new Scanner(System.in);
		N = read.nextInt();
		
		System.out.println("Enter Retransmission Timer Interval in milliseconds");		
		time = new Time(read.nextLong());
	}
	
	public void open(InetAddress src, InetAddress dest, int srcPort, int destPort, int verbose) throws IOException {
		ds = new DatagramService(srcPort, verbose);
		datagram = new Datagram();
		datagram.setSrcaddr(src.toString());
		datagram.setDstaddr(dest.toString());
		datagram.setSrcport((short)srcPort);
		datagram.setDstport((short)destPort);
		datagram.setSize((short)9);
		datagram.setData(createPayloadHeader());
		datagram.setChecksum(calculateChecksum(datagram));
		
	}
	
	/**
	 * 
	 * @param flags- The flags list is-
	 * 					1) SYN
	 * 					2) ACK
	 * 					3) FIN
	 * @return
	 */
	  
	
	private byte[] createPayloadHeader(int flags) {
		byte[] header = new byte[9];
		byte[] isnBytes = ByteBuffer.allocate(4).putInt(isn).array();
		byte[] ackBytes = ByteBuffer.allocate(4).putInt(isn + 1).array(); 
		
		switch(flags) {
			case SYN: 
				for(int i=0; i<4; i++) {
					header[i] = isnBytes[i];
				}
				for(int i=4; i<8; i++) {
					header[i] = (byte)0;
				}
				header[8] = (byte)128;
				break;
			
			case ACK:
				for(int i=0; i<4; i++) {
					header[i] = (byte)0;
				}
				for(int i=4; i<8; i++) {
					header[i] = isnBytes[i-4];
				}
				header[8] = (byte)64;
				break;
			
			case FIN:
				for(int i=0; i<4; i++) {
					header[i] = ackBytes[i];
				}
				for(int i=4; i<8; i++) {
					header[i] = (byte)0;
				}
				header[8] = (byte)32;
				break;
				
		}

		return header;
	}

	public void sendData(InetAddress src, InetAddress dest, int srcPort, int destPort, byte[] data) throws IOException  {
		datagram.setData(data);
		datagram.setChecksum(calculateChecksum(datagram));			
		ds.sendDatagram(datagram);
		System.out.println("Sent datagram");
	}
	
	public short calculateChecksum(Datagram datagram) throws IOException {
		Checksum checksum = new CRC32();
		ByteArrayOutputStream bStream = new ByteArrayOutputStream(1500);
		ObjectOutputStream oStream = new ObjectOutputStream(bStream);
		oStream.writeObject(datagram);
		byte[] data = bStream.toByteArray();
		checksum.update(data,0,data.length);
		return (short)checksum.getValue();
	}
}
