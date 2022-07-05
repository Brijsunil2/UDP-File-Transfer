# UDP File Transfer

## Description
A simple file transfer project using java that allows the user to transfer files 
between users. Files transfered could be text, images, audio and small videos.

## How to Test
One user will have Sender.java and another will have Receiver.java.
Use a terminal and cd to the file locations and compile them.

Running Sender.java: java Sender reciever_ip receiver_port_num sender_port_num filename mds timeout
  - Note mds and timeout are entered in milliseconds.

Running Receiver.java: java Receiver