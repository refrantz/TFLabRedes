// L� uma linha do teclado
// Envia o pacote (linha digitada) ao servidor

import java.io.*; // classes para input e output streams e
import java.net.*;// DatagramaSocket,InetAddress,DatagramaPacket
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.CRC32;

class UDPClient {

   public static void main(String args[]) throws Exception
   {

      //declare globals


      int chunkSize = 300;
      int paddingMetaSize = 4;
      int checkSumSize = 20;
      int seqNumSize = 10;

      byte[] sendData = new byte[chunkSize];
      byte[] receiveData = new byte[chunkSize];
      int acresc = 1;
      boolean AllACK = true;
      int congestWindow = 20;
      int retransmitCounter = 0;
      int lastFailedACKSeqNum = 0;
      boolean connected = false;



      // Load the file into chunks and append metadata

      Scanner scanner = new Scanner(System.in);  // Create a Scanner object
      System.out.println("Insira o nome do arquivo: ");

      String fileName = scanner.nextLine();  // Read user input
      File file = new File(fileName);  // Use the input to create a File object

      scanner.close();  // Don't forget to close the scanner

      FileInputStream fis = new FileInputStream(file);

      // Buffer to hold each chunk
      byte[] buffer = new byte[chunkSize-checkSumSize-seqNumSize-paddingMetaSize]; // 270 bytes for data, 20 bytes for checksum, 10 bytes for sequence number, 4 bytes to specify padding

      // Read the file chunk by chunk
      List<byte[]> packets = new ArrayList<>();
      int bytesRead;
      int sequenceNumber = 0;
      int padding = 0;
      while ((bytesRead = fis.read(buffer)) != -1) {
         // At this point, 'buffer' contains a chunk of the file,
         // and 'bytesRead' is the number of bytes read into the buffer.

         //data is smaller than chunk size
         if(bytesRead < buffer.length){
            padding = buffer.length - bytesRead;
         }

         // If the last chunk was smaller than the chunk size, adjust the buffer size accordingly.

         // Calculate the checksum
         CRC32 crc = new CRC32();
         crc.update(buffer);
         long checksum = crc.getValue();

         // Append the checksum and sequence number to the chunk
         String metadata = String.format("%4d%10d%20d", padding, sequenceNumber, checksum);
         byte[] metadataBytes = metadata.getBytes();
         byte[] packet = new byte[chunkSize];
         System.arraycopy(buffer, 0, packet, 0, buffer.length);
         System.arraycopy(metadataBytes, 0, packet, buffer.length, metadataBytes.length);
         packets.add(packet);
         sequenceNumber++;
         buffer = new byte[chunkSize-checkSumSize-seqNumSize-paddingMetaSize];
      }

      int totalPackets = (int) Math.ceil((double) file.length() / (chunkSize - checkSumSize - seqNumSize - paddingMetaSize));
      int sentPackets = 0;

      fis.close();


      //


      //Send initial connection


      // declara socket cliente
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.setSoTimeout(1000);

      // obtem endere�o IP do servidor com o DNS
      InetAddress IPAddress = InetAddress.getByName("localhost");

      while(!connected){
         sendData = ("Connect with size:" + totalPackets + "|name:" + fileName).getBytes();

         DatagramPacket initialPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

         clientSocket.send(initialPacket);

         DatagramPacket receiveInitialPacket = new DatagramPacket(receiveData, receiveData.length);
         
         try {
            clientSocket.receive(receiveInitialPacket);
            String initialConfirm = new String(receiveInitialPacket.getData()).trim();

            if (initialConfirm.equals("Confirm connection")){
               System.out.println("Connection succeded");
               connected = true;
            }else{
               System.out.println("Connection failed");
            }
            
         } catch (SocketTimeoutException e) {
            System.out.println("Connection failed");
         }

      }



      //


      //send packets

      sendData = new byte[chunkSize];
      receiveData = new byte[chunkSize];

      while (sentPackets < totalPackets){

         for (int i = 0; i < Math.min(acresc, totalPackets-sentPackets); i++){

            int packNumber = sentPackets+i; 
            sendData = packets.get(packNumber);
      
            // cria pacote com o dado, o endere�o do server e porta do servidor
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
                  
            //envia o pacote
            clientSocket.send(sendPacket);
         }

         for (int i = 0; i < acresc; i++){
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
               clientSocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
               System.out.println("Timeout");
               acresc = 1;
               AllACK = false;
               break;
            }
      
           //
            String sentenceACK = new String(receivePacket.getData());
            int ACKSeqNum = Integer.parseInt(sentenceACK.split(":")[1].trim());
           
            System.out.println(sentenceACK); 

            if(!(ACKSeqNum == sentPackets+1)){

               i--;
               AllACK = false;

               if(lastFailedACKSeqNum == ACKSeqNum){


                  System.out.println("Fast retransmit");
                  retransmitCounter++;

                  if (retransmitCounter == 3) {

                     sendData = packets.get(lastFailedACKSeqNum);
                     DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
                     clientSocket.send(sendPacket);
                  
                     // reset duplicate ACK counter
                     congestWindow /= 2;
                     retransmitCounter = 0;
                     break;
                  }
               }

               lastFailedACKSeqNum = ACKSeqNum;

            }else if(ACKSeqNum == sentPackets+1){
               retransmitCounter=0;
               sentPackets++;
               if (sentPackets >= totalPackets) {
                  sendData = "Terminate connection".getBytes();
                  DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
                  clientSocket.send(sendPacket);
                  break;
               }
            }

         }
         
         if(AllACK){
            if(acresc < congestWindow){
               acresc*=2;
            }else if(acresc >= congestWindow){
               acresc++;
            }
         }
        
      }

      // fecha o cliente
      System.out.println("Connection Terminated");
      clientSocket.close();
   }
}
