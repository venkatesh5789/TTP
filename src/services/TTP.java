package services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import datatypes.Datagram;

public class TTP {
	private DatagramService ds;
	private Datagram datagram;
	private Datagram recdDatagram;
	private Datagram responseDatagram;
	private int base;
	private int nextSeqNum;
	private int N;
	private int acknNum;
	private int expectedSeqNum;
	private Time time;
	private Timer clock = new Timer();
	private HashMap<Integer,Datagram> unacknowledgedPackets;
	private static final int SYN = 0;
	private static final int ACK = 1;
	private static final int FIN = 2;
	private static final int DATA = 3;
	
	public TTP() {
		System.out.println("Enter Send/Receive Window");
		Scanner read = new Scanner(System.in);
		N = read.nextInt();
		
		System.out.println("Enter Retransmission Timer Interval in milliseconds");		
		time = new Time(read.nextLong());
	}
	
	public void open(InetAddress src, InetAddress dest, int srcPort, int destPort, int verbose) throws IOException {
		ds = new DatagramService(srcPort, verbose);
		datagram.setSrcaddr(src.toString());
		datagram.setDstaddr(dest.toString());
		datagram.setSrcport((short)srcPort);
		datagram.setDstport((short)destPort);
		datagram.setSize((short)9);
		datagram.setData(createPayloadHeader(TTP.SYN));
		datagram.setChecksum(calculateChecksum(datagram));	
		ds.sendDatagram(datagram);
		
		if(base == nextSeqNum) {
			clock.schedule(new RetransmitTask(), time.getTime());
		}
			
		unacknowledgedPackets.put(nextSeqNum, datagram);
		nextSeqNum++;
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
		byte[] isnBytes = ByteBuffer.allocate(4).putInt(nextSeqNum).array();
		byte[] ackBytes = ByteBuffer.allocate(4).putInt(acknNum).array(); 
		
		switch(flags) {
			case SYN: 
				for(int i=0; i<4; i++) {
					header[i] = isnBytes[i];
				}
				for(int i=4; i<8; i++) {
					header[i] = (byte)0;
				}
				header[8] = (byte)4;
				break;
			
			case ACK:
				for(int i=0; i<4; i++) {
					header[i] = (byte)0;
				}
				for(int i=4; i<8; i++) {
					header[i] = ackBytes[i-4];
				}
				header[8] = (byte)2;
				isnBytes = ByteBuffer.allocate(4).putInt(-1).array();
				break;
			
			case FIN:
				for(int i=0; i<4; i++) {
					header[i] = (byte)0;
				}
				for(int i=4; i<8; i++) {
					header[i] = (byte)0;
				}
				header[8] = (byte)1;
				break;	
			
			case DATA:
				for(int i=0; i<4; i++) {
					header[i] = isnBytes[i];
				}
				for(int i=4; i<8; i++) {
					header[i] = (byte)0;
				}
				header[8] = (byte)0;
				break;
				
		}

		return header;
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
	
	public void sendData(byte[] data) throws IOException  {
		
		if(nextSeqNum< base + N) {
			byte[] header = createPayloadHeader(TTP.DATA);
			
			byte[] headerPlusData = new byte[data.length + header.length];
			System.arraycopy(header, 0, headerPlusData, 0, header.length);
			System.arraycopy(data, 0, headerPlusData, header.length, data.length);
			
			datagram.setData(headerPlusData);
			datagram.setSize((short)headerPlusData.length);
			datagram.setChecksum(calculateChecksum(datagram));			
			ds.sendDatagram(datagram);
			
			if(base == nextSeqNum) {
				clock.schedule(new RetransmitTask(), time.getTime());
			}
			
			unacknowledgedPackets.put(nextSeqNum, datagram);
			nextSeqNum++;
		}
		else {
			refuse_data(data);
		}
		System.out.println("Sent datagram");
	}
	
	private void refuse_data(byte[] data) {
		// TODO Auto-generated method stub
		
	}

	public byte[] receiveData() throws IOException, ClassNotFoundException {
		recdDatagram = ds.receiveDatagram(); 
		
		byte[] data = (byte[]) recdDatagram.getData();
		byte[] app_data = new byte[data.length - 9];
		
		if (recdDatagram.getSize() > 9) {
			for (int i=0; i < app_data.length; i++) {
				app_data[i] = data[i+9];
			}
		} else {
			if (data[8] == (byte)4) {				
				acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
				expectedSeqNum =  acknNum + 1;
				responseDatagram.setSrcaddr(recdDatagram.getDstaddr());
				responseDatagram.setDstaddr(recdDatagram.getSrcaddr());
				responseDatagram.setSrcport(recdDatagram.getDstport());
				responseDatagram.setDstport(recdDatagram.getSrcport());
				responseDatagram.setSize((short)9);
				sendAcknowledgement();
			}
			if(data[8]== (byte)2) {
				base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
				if(base == nextSeqNum) {
					clock.cancel();
				} else {
					clock.cancel();
					clock.schedule(new RetransmitTask(), time.getTime());
				}
			}
		}
		return app_data;
	}
	public void sendAcknowledgement() {
		responseDatagram.setData(createPayloadHeader(ACK));
	}
	public static int byteArrayToInt(byte[] b) 
	{
	    int value = 0;
	    for (int i = 0; i < 4; i++) {
	        int shift = (4 - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
	}

	public static byte[] intToByteArray(int a)
	{
	    byte[] ret = new byte[4];
	    ret[0] = (byte) (a & 0xFF);   
	    ret[1] = (byte) ((a >> 8) & 0xFF);   
	    ret[2] = (byte) ((a >> 16) & 0xFF);   
	    ret[3] = (byte) ((a >> 24) & 0xFF);
	    return ret;
	}
	
	class RetransmitTask extends TimerTask {

		@Override
		public void run() {
			
		}
		
	}
}
