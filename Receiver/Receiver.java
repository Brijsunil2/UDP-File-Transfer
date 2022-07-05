import java.io.* ;
import java.net.* ;
import javax.swing.*;  
import java.awt.*;
import java.awt.event.*;

final class Receiver implements ActionListener {
    final static String CRLF = "\r\n";
    final static int MAX_SIZE = 1024;
    final static int PKT_TO_DROP = 10;

    private DatagramSocket socket = null;
    private File file = null;
    private FileOutputStream fos = null;

    private JTextField sIPTxt = new JTextField();
    private JTextField sPortNumTxt = new JTextField();
    private JTextField rPortNumTxt = new JTextField();
    private JTextField fileNameTxt = new JTextField();

    private InetAddress sIP = null;
    private int sPortNum;
    private int rPortNum;
    private String filename = null;

    private JTextArea textArea = new JTextArea("");
    private JScrollPane areaScroll = new JScrollPane(textArea);

    private JCheckBox unreliable = new JCheckBox("Unreliable Transfer");
    private boolean reliable;

    private JButton receiveBtn = new JButton("RECEIVE");

    public Receiver() {
        appGUI();
    }

    private void appGUI() {
        JFrame frame = new JFrame("Receive File");
        JPanel panel = new JPanel();
        JPanel backPanel = new JPanel();
        
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        receiveBtn.addActionListener(this);

        panel.setLayout(new GridLayout(6, 2, 5, 5));
        backPanel.setLayout(new GridLayout(1, 2, 5, 5));

        panel.add(new JLabel("Sender IP Address:"));
        panel.add(sIPTxt);
        panel.add(new JLabel("Sender Port Number:"));
        panel.add(sPortNumTxt);
        panel.add(new JLabel("Your Port Number:"));
        panel.add(rPortNumTxt);
        panel.add(new JLabel("File Name:"));
        panel.add(fileNameTxt);
        panel.add(unreliable);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));
        panel.add(receiveBtn);

        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        backPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        textArea.setEditable(false);

        backPanel.add(panel);
        backPanel.add(areaScroll);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
            System.err.println("Could not use UI of the current OS. Using default UI.");
        } 

        frame.add(backPanel);
        frame.setSize(760, 260);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
    }

    private void retrieveFile() {
        String originalText = textArea.getText();
        boolean done = false;
        byte[] ackBuff = new byte[1];
        byte seqNum = 1;
        int dropPktCounter = 0;
        int numPackets = 0;
        long startTime, endTime, totalTime;

        try {
            DatagramPacket pktOut = null;
            DatagramPacket pktIn = null;
            socket.setSoTimeout(5000);

            ackBuff[0] = seqNum;
            pktOut = new DatagramPacket(ackBuff, ackBuff.length, sIP, sPortNum);
            socket.send(pktOut);

            startTime = System.currentTimeMillis();

            while (!done) {
                pktIn = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                socket.receive(pktIn);

                if (!(unreliable.isSelected() && dropPktCounter == PKT_TO_DROP)) {
                    byte[] buff = pktIn.getData();

                    if ((int) buff[0] == 2) {
                        done = true;
                    } else if (Byte.compare(buff[0], seqNum) != 0) {

                        try {
                            fos.write(buff, 1, pktIn.getLength()-1); 
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println("Last Packet. Sending final ack to close.");
                        }

                        seqNum = buff[0];
                        numPackets++;

                        textArea.setText(originalText + "Current Number of Received In-Order Packets: " + numPackets + CRLF);
                    } 

                    ackBuff[0] = seqNum;
                    socket.send(new DatagramPacket(ackBuff, ackBuff.length, sIP, sPortNum));
                    dropPktCounter++;

                } else {
                    dropPktCounter = 0;
                    System.out.println("Packet has been dropped due to \"Unreliable Transfer\" being enabled.");
                }
            }

            endTime = System.currentTimeMillis();
            totalTime = endTime - startTime;
            System.out.println("File transfer complete.");
            System.out.println("Total time file transfer took: " + totalTime + " ms");
            textArea.append("File transfer complete." + CRLF + "Total time file transfer took: " + totalTime + " ms" + CRLF);

        } catch (SocketTimeoutException e) {
            System.err.println("Error: Socket timeout occured. Error on sender side.");
            textArea.append("Error: Socket timeout occured. Error on sender side." + CRLF);
            textArea.append("Please try again by restarting or clicking RECEIVE again." + CRLF);
        } catch (SocketException e) {
            System.err.println("Error: Socket timeout error.");
            textArea.append("Error: Socket timeout error." + CRLF);
            textArea.append("Please try again by restarting or clicking RECEIVE again." + CRLF);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: Unexpected IO error has occured.");
            textArea.append("Error: Unexpected IO error has occured." + CRLF);
            textArea.append("Please try again by restarting or clicking RECEIVE again." + CRLF);
            e.printStackTrace();
        }

        try {
            socket.close();
            fos.close();
        } catch (IOException e) {
            System.err.println("Error: Unexpected IO error has occured.");
            textArea.append("Error: Unexpected IO error has occured." + CRLF);
            textArea.append("Please try again by restarting or clicking RECEIVE again." + CRLF);
            e.printStackTrace();
        }
    }

    private boolean checkAndConnect() {
        boolean valid = true;

        if (sIPTxt.getText().equals("")) {
            valid = false;
            System.err.println("Error: Please enter the senders IP address.");
            textArea.append("Error: Please enter the senders IP address." + CRLF);
        } 
        
        if (sPortNumTxt.getText().equals("") || rPortNumTxt.getText().equals("")) {
            valid = false;
            System.err.println("Error: Please fill out the port number fields.");
            textArea.append("Error: Please fill out the port number fields." + CRLF);
        }

        if (fileNameTxt.getText().equals("")) {
            valid = false;
            System.err.println("Error: Please enter a name for the file.");
            textArea.append("Error: Please enter a name for the file." + CRLF);
        }

        if (valid) {
            try {
                sIP = InetAddress.getByName(sIPTxt.getText());
                sPortNum = Integer.parseInt(sPortNumTxt.getText());
                rPortNum = Integer.parseInt(rPortNumTxt.getText());
                filename = fileNameTxt.getText();
                
                if (unreliable.isSelected()) {
                    reliable = false;
                } else {
                    reliable = true;
                }

                socket = new DatagramSocket(rPortNum);
                file = new File(filename);
                file.createNewFile();
                fos = new FileOutputStream(file);

            }  catch (SocketException e) {
                System.err.println("Error: Socket could not be opened or could not bind to port.");
                textArea.append("Error: Socket could not be opened or could not bind to port." + CRLF);
                valid = false;
            }  catch (UnknownHostException e) {
                System.err.println("Error: Unknown IP address.");
                textArea.append("Error: Unknown IP address." + CRLF);
                valid = false;
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid Port Number.");
                textArea.append("Error: Invalid Port Number." + CRLF);
                valid = false;
            } catch (IOException e) {
                System.err.println("Error: Unknown IO error.");
                textArea.append("Error: Unknown IO error." + CRLF);
                valid = false;
            } 
        }

        return valid;
    }

    public void actionPerformed(ActionEvent e) {
        Boolean valid;

        receiveBtn.setText("Please Wait...");
        receiveBtn.setEnabled(false);

        valid = checkAndConnect();

        if (valid) {
            System.out.println("File transfer has started.");
            textArea.append("File transfer has started." + CRLF);
            retrieveFile();
        }

        System.out.println("-----------------------------------------------");
        textArea.append("-----------------------------------------------" + CRLF);
        receiveBtn.setEnabled(true);
        receiveBtn.setText("RECEIVE");
    }

    public static void main(String[] args) {
        new Receiver();
    }
}