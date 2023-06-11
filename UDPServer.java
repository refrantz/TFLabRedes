// Recebe um pacote de algum cliente
// Separa o dado, o endere�o IP e a porta deste cliente
// Imprime o dado na tela

import java.io.FileOutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.zip.CRC32;

class UDPServer {
   public static void main(String args[])  throws Exception{

      // cria socket do servidor com a porta 9876

      int chunkSize = 300;
      int chunkDataSize = 266;
      byte[] receiveData = new byte[chunkSize];
      byte[] sendData = new byte[chunkSize];

      DatagramSocket serverSocket = new DatagramSocket(9876);

      DatagramPacket initialPacket = new DatagramPacket(receiveData, receiveData.length);
      serverSocket.receive(initialPacket);

      InetAddress IPAddress = initialPacket.getAddress();
      int port = initialPacket.getPort();

      int totalPackets = Integer.parseInt(new String(initialPacket.getData()).trim().split(":")[1]);
      Boolean[] packetReceived = new Boolean[totalPackets];
      Arrays.fill(packetReceived, Boolean.FALSE);

      sendData = "Confirm connection".getBytes();
      DatagramPacket sendInitialPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
      serverSocket.send(sendInitialPacket);


      //


      sendData = new byte[chunkSize];
      receiveData = new byte[chunkSize];

      int lastACKNum = 0;

      FileOutputStream fos = new FileOutputStream("received_gremio.png");

      while(Arrays.asList(packetReceived).contains(false)){
            // declara o pacote a ser recebido
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // recebe o pacote do cliente
            serverSocket.receive(receivePacket);

            String sentence = new String(receivePacket.getData());
            if(sentence.trim().equals("Terminate connection")){
               break;
            }

            byte[] sentenceBytes = receivePacket.getData();

            // Extract the last 20 characters for the checksum
            byte[] checksumBytes = Arrays.copyOfRange(sentenceBytes, sentenceBytes.length - 20, sentenceBytes.length);
            String checksumStr = new String(checksumBytes);
            long checksum = Long.parseLong(checksumStr.trim());
            

            // Extract the 10 characters before the checksum for the sequence number
            byte[] seqNumBytes = Arrays.copyOfRange(sentenceBytes, sentenceBytes.length - 30, sentenceBytes.length - 20);
            String seqNumStr = new String(seqNumBytes);
            int seqNum = Integer.parseInt(seqNumStr.trim());

            System.out.println(seqNum);

            // Extract the 4 characters before the seqnum for the padding size
            byte[] paddingSizeBytes = Arrays.copyOfRange(sentenceBytes, sentenceBytes.length - 34, sentenceBytes.length - 30);
            String paddingSizeStr = new String(paddingSizeBytes);
            int paddingSize = Integer.parseInt(paddingSizeStr.trim());

            System.out.println(paddingSize);

            // Calculate the checksum
            CRC32 crc = new CRC32();
            crc.update(sentenceBytes, 0, chunkDataSize); // Only calculate checksum for the data part
            long calculatedChecksum = crc.getValue();

            System.out.println(checksum);
            System.out.println(calculatedChecksum);

            
            //for(byte b : sentenceBytes){
            //   System.out.println(b);
            //}

            
            if((seqNum == 0 || packetReceived[seqNum-1]) && (checksum == calculatedChecksum)){

               fos.write(sentenceBytes, 0, chunkDataSize-paddingSize); // Only write the data part

               packetReceived[seqNum] = true;
   
               String sentenceACK = "ACK|seq:"+(seqNum+1);
               sendData = sentenceACK.getBytes();
               DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
               serverSocket.send(sendPacket);
               lastACKNum = seqNum+1;

            }else{

               String sentenceACK = "ACK|seq:"+(lastACKNum);
               sendData = sentenceACK.getBytes();
               DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
               serverSocket.send(sendPacket);

            }

            //System.out.println("Mensagem recebida: " + sentence);
      }

      System.out.println("Connection terminated");
      serverSocket.close();

   }
}
