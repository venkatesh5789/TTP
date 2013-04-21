package services;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import javax.swing.Timer;
import java.util.TimerTask;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import datatypes.Datagram;

public class TTPClient {
	private DatagramService ds;
	private Datagram datagram;
	private Datagram recdDatagram;
	private int base;
	private int nextSeqNum;
	private int N;
	private int acknNum;
	private int expectedSeqNum;
	private int time;
	private Timer clock;
	private HashMap<Integer,Datagram> unacknowledgedPackets;
	
	public static final int SYN = 0;
	public static final int ACK = 1;
	public static final int FIN = 2;
	public static final int DATA = 3;
	public static final int EOFDATA = 4;
	public static final int SYNACK = 5;

	public TTPClient() {
		datagram = new Datagram();
		recdDatagram = new Datagram();
		unacknowledgedPackets = new HashMap<Integer,Datagram>();
		
		System.out.println("Enter Send Window Size");
		Scanner read = new Scanner(System.in);
		N = read.nextInt();
		
		System.out.println("Enter Retransmission Timer Interval in milliseconds");		
		time = read.nextInt();
		
		clock = new Timer(time,listener);
		clock.setInitialDelay(time);
		
		Random rand = new Random();
		nextSeqNum = rand.nextInt(65536);
		System.out.println("ISN:" + nextSeqNum);
	}

	public void setAcknNum(int acknNum) {
		this.acknNum = acknNum;
	}

	public void setExpectedSeqNum(int expectedSeqNum) {
		this.expectedSeqNum = expectedSeqNum;
	}

	public void open(String src, String dest, short srcPort,
			short destPort, int verbose, int flag) throws IOException, ClassNotFoundException {
			
		this.ds = new DatagramService(srcPort, verbose);

		datagram.setSrcaddr(src);
		datagram.setDstaddr(dest);
		datagram.setSrcport((short) srcPort);
		datagram.setDstport((short) destPort);
		datagram.setSize((short) 9);
		datagram.setData(createPayloadHeader(flag));
		datagram.setChecksum(calculateChecksum(datagram));
		this.ds.sendDatagram(datagram);
		System.out.println("SYN/SYNACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport());
		
		base = nextSeqNum;
		clock.start();
	
		unacknowledgedPackets.put(nextSeqNum, datagram);
		nextSeqNum++;
		
		receiveData();
	}
	
	/**
	 * 
	 * @param flags
	 * 					1) SYN
	 * 					2) ACK
	 * 					3) FIN
	 * 					4) EOF
	 * @return
	 */
	private byte[] createPayloadHeader(int flags) {
		byte[] header = new byte[9];
		byte[] isnBytes = ByteBuffer.allocate(4).putInt(nextSeqNum).array();
		byte[] ackBytes = ByteBuffer.allocate(4).putInt(acknNum).array();

		switch (flags) {
		case SYN:
			for (int i = 0; i < 4; i++) {
				header[i] = isnBytes[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 4;
			break;

		case ACK:
			for (int i = 0; i < 4; i++) {
				header[i] = isnBytes[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = ackBytes[i - 4];
			}
			header[8] = (byte) 2;
			break;

		case FIN:
			for (int i = 0; i < 4; i++) {
				header[i] = (byte) 0;
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 1;
			break;

		case DATA:
			for (int i = 0; i < 4; i++) {
				header[i] = isnBytes[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 0;
			break;

		case EOFDATA:
			for (int i = 0; i < 4; i++) {
				header[i] = isnBytes[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 8;
			break;
		case SYNACK:
			for (int i = 0; i < 4; i++) {
				header[i] = isnBytes[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = ackBytes[i - 4];
			}
			header[8] = (byte) 6;
			break;
		}
		System.out.println("Payload Header:" + header.toString());
		System.out.println("ISN:" + nextSeqNum);
		return header;
	}

	public short calculateChecksum(Datagram datagram) throws IOException {
		Checksum checksum = new CRC32();
		ByteArrayOutputStream bStream = new ByteArrayOutputStream(1500);
		ObjectOutputStream oStream = new ObjectOutputStream(bStream);
		oStream.writeObject(datagram);
		byte[] data = bStream.toByteArray();
		checksum.update(data, 0, data.length);
		return (short) checksum.getValue();
	}

	public void sendData(byte[] data) throws IOException {

		if (nextSeqNum < base + N) {

			int lengthOfData = data.length;
			byte[] fragment = new byte[1451];
			int dataCounter = 0;
			int currentCounter;

			if (lengthOfData > 1451) {

				while (lengthOfData > 0) {
					lengthOfData -= 1451;
					currentCounter = dataCounter;

					for (int i = currentCounter; i < currentCounter + 1451; dataCounter++, i++) {
						fragment[i % 1451] = data[i];
					}

					if (lengthOfData > 1451)
						encapsulateAndSendFragment(fragment, false);
					else
						encapsulateAndSendFragment(fragment, true);
				}
			} else {
				fragment = data.clone();
				encapsulateAndSendFragment(fragment, true);
			}
		} else {
			refuse_data(data);
		}
	}

	private void encapsulateAndSendFragment (byte[] fragment, boolean lastFragment) throws IOException {

		byte[] header = new byte[9];
		if (lastFragment) {
			header = createPayloadHeader(TTPClient.EOFDATA);
		} else {
			header = createPayloadHeader(TTPClient.DATA);
		}

		byte[] headerPlusData = new byte[fragment.length + header.length];
		System.arraycopy(header, 0, headerPlusData, 0, header.length);
		System.arraycopy(fragment, 0, headerPlusData, header.length, fragment.length);

		datagram.setData(headerPlusData);
		datagram.setChecksum(calculateChecksum(datagram));
		ds.sendDatagram(datagram);
		System.out.println("Sent datagram with sequence no:" + nextSeqNum);

		if (base == nextSeqNum) {
			clock.restart();
		}

		unacknowledgedPackets.put(nextSeqNum, datagram);
		nextSeqNum++;
	}

	private void refuse_data(byte[] data) {

	}

	public byte[] receiveData() throws IOException, ClassNotFoundException {
		recdDatagram = ds.receiveDatagram(); 

		byte[] data = (byte[]) recdDatagram.getData();
		byte[] app_data = null;
		
		if (recdDatagram.getSize() > 9) {
			if(byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]}) == expectedSeqNum) {
				acknNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
				if(data[8]==8) {
					app_data = new byte[data.length - 9];
					for (int i=0; i < app_data.length; i++) {
						app_data[i] = data[i+9];
					}
					sendAcknowledgement();
					expectedSeqNum++;
				}
				else if(data[8]== 0) {
					ArrayList<Byte> dataList = reassemble(data);
					app_data = new byte[dataList.size()];
					System.arraycopy(dataList, 0, app_data, 0, app_data.length);
				}
			}
			else {
				sendAcknowledgement();
			}
			
			
		} else {
			if (data[8] == (byte)6) {				
				acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
				expectedSeqNum =  acknNum + 1;
				base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
				clock.stop();
				System.out.println("Received SYNACK with seq no:" + acknNum);
				sendAcknowledgement();
			}
			if(data[8]== (byte)2) {
				base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
				System.out.println("Received ACK for packet no:" + byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}));
			}
			if(base == nextSeqNum) {
				clock.stop();
			} else {
				clock.restart();
			}
		}
		return app_data;
	}

	private ArrayList<Byte> reassemble(byte[] data2) throws IOException, ClassNotFoundException {
		ArrayList<Byte> reassembledData = new ArrayList<Byte>();

		for(byte nextbyte : data2) {
			reassembledData.add(nextbyte);
		}

		while(true) {
			recdDatagram = ds.receiveDatagram(); 
			byte[] data = (byte[]) recdDatagram.getData();

			if(byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]}) == expectedSeqNum) {
				acknNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});

				for(byte nextbyte : data2) {
					reassembledData.add(nextbyte);
				}

				sendAcknowledgement();
				nextSeqNum++;

				if(data[8]==0) {
					continue;
				}
				else if(data[8]==8) {
					break;
				}

			}
			else {
				sendAcknowledgement();
			}
		}

		return reassembledData;

	}

	public void sendAcknowledgement() throws IOException {
		datagram.setData(createPayloadHeader(ACK));
		datagram.setChecksum(calculateChecksum(datagram));
		ds.sendDatagram(datagram);
		System.out.println("Acknowledgement sent! No:" + acknNum);
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

	/*class RetransmitTask extends TimerTask {

		@Override
		public void run() {
			for (Datagram d: unacknowledgedPackets.values()) {
				try {
					ds.sendDatagram(d);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			clock.schedule(new RetransmitTask(), time.getTime());
		}

	}*/
	ActionListener listener = new ActionListener(){
		  public void actionPerformed(ActionEvent event){
			  for (Datagram d: unacknowledgedPackets.values()) {
					try {
						ds.sendDatagram(d);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			  clock.restart();
		  }
		};
}
