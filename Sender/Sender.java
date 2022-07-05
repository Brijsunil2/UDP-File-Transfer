import java.io.* ;
import java.net.* ;
import javax.swing.*;  
import java.awt.*;
import java.awt.event.*;
import java.util.*;

final class Sender {
    private final int MAX_EX_IN_ROW = 5;

    private InetAddress rIP = null;
    private int rPortNum;
    private int sPortNum;
    private String fileName;
    private int mds;
    private int timeOut;

    public Sender(String receiverIP, int receiverPortNum, int senderPortNum, 
            String fileName, int mds, int timeOut) {
        this.rPortNum = receiverPortNum;
        this.sPortNum = senderPortNum;
        this.fileName = fileName;
        this.mds = mds;
        this.timeOut = timeOut / 1000;
        
        try {
            rIP = InetAddress.getByName(receiverIP);
        } catch (UnknownHostException e) {
            System.err.println("Error: No IP address of this host could not be found.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void sendFile() {
        DatagramPacket pktOut = null;
        DatagramPacket pktIn = null;
        boolean isTimeOut = false;
        int fileSent = 0;
        byte[] buff = null;
        byte[] ackBuff = null;
        byte seqNum = 0;
        int i, numExInRow = 0, j = 0;

        try {
            DatagramSocket socket = new DatagramSocket(sPortNum);
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            System.out.println("Waiting on Receiver...");
            socket.receive(new DatagramPacket(new byte[1], 1)); 
            System.out.println("Sending file to Receiver.");
            socket.setSoTimeout(timeOut);

            // Sends File 
            while (numExInRow < MAX_EX_IN_ROW && (fileSent != -1 || isTimeOut)) {
                try {
                    if (!isTimeOut) {
                        buff = new byte[mds + 1];
                        buff[0] = seqNum;

                        fileSent = fis.read(buff, 1, buff.length-1); 
        
                        pktOut = new DatagramPacket(buff, fileSent+1, rIP, rPortNum);
                    }
        
                    socket.send(pktOut);
        
                    pktIn = new DatagramPacket(new byte[1], 1);
                    socket.receive(pktIn);
                    ackBuff = pktIn.getData();
                    //System.out.print("Packet:" + (int)seqNum);
                    //System.out.println(" : " + (int)ackBuff[0]);
                    
                    if (Byte.compare(seqNum, ackBuff[0]) == 0) {
                        if (Byte.compare(seqNum, (byte)0) == 0) {
                            seqNum = 1;
                        } else {
                            seqNum = 0;
                        }
                        isTimeOut = false;
                    } else if (fileSent == -1) {
                        isTimeOut = false;
                    } else {
                        isTimeOut = true;
                    }
                    numExInRow = 0;
                } catch (SocketTimeoutException e) {
                    System.err.println("Error: Socket timeout occured at sequence number " + (int)seqNum + ". Resending packet.");
                    isTimeOut = true;
                    numExInRow++;
                } catch (IllegalArgumentException e) {
                    System.out.println("Last packet.");
                }
            }

            // Sends indication that file transfer is done
            isTimeOut = true;
            buff = new byte[1];
            buff[0] = 2;
            while (isTimeOut && numExInRow < MAX_EX_IN_ROW) {
                try {
                    pktOut = new DatagramPacket(buff, buff.length, rIP, rPortNum);
                    socket.send(pktOut);
                    socket.receive(new DatagramPacket(new byte[1], 1));
                    isTimeOut = false;
                    System.out.println("Sent EOT Packet.");
                } catch (SocketTimeoutException e) {
                    System.err.println("Error: Socket timeout occured. Resending done packet.");
                    isTimeOut = true;
                    numExInRow++;
                }
            } 
            socket.close();
            fis.close();

            System.out.println("File has been sent.");

        } catch (SocketException e) {
            System.err.println("Error: Socket could not open or bind to specified port.");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Error: File path name is either null or does not exist.");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("Error: File does not exist.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: Unexpected IO error has occured.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            String receiverIP = args[0];
            int receiverPortNum = Integer.parseInt(args[1]);
            int senderPortNum = Integer.parseInt(args[2]);
            String fileName = args[3];
            int mds = Integer.parseInt(args[4]);
            int timeOut = Integer.parseInt(args[5]);

            Sender sender = new Sender(receiverIP, receiverPortNum, senderPortNum, fileName, mds, timeOut);
            sender.sendFile();
            
        } catch (NumberFormatException e) {
            System.err.println("The port numbers, mds or time out must be an integer.");
        }

        System.out.println("Exited.");
    }
}