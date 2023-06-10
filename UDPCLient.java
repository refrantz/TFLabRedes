// L� uma linha do teclado
// Envia o pacote (linha digitada) ao servidor

import java.io.*; // classes para input e output streams e
import java.net.*;// DatagramaSocket,InetAddress,DatagramaPacket

class UDPClient {

   public static void sendPackets(){
      
   }

   public static void main(String args[]) throws Exception
   {
      // declara socket cliente
      DatagramSocket clientSocket = new DatagramSocket();
      clientSocket.setSoTimeout(1000);

      // obtem endere�o IP do servidor com o DNS
      InetAddress IPAddress = InetAddress.getByName("localhost");

      byte[] sendData = new byte[1024];
      byte[] receiveData = new byte[1024];
      int acresc = 1;
      int totalPackets = 10;
      int sentPackets = 0;
      boolean AllACK = true;
      int congestWindow = 20;
      int retransmitCounter = 0;

      while (sentPackets < totalPackets){

         for (int i = 0; i < Math.min(acresc, totalPackets-sentPackets); i++){
            // l� uma linha do teclado
            int packNumber = sentPackets+i;
            String sentence = "gremio"+"|seq:"+packNumber;
            sendData = sentence.getBytes();
      
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
              acresc = 1;
              AllACK = false;
              break;
           }
      
            String sentenceACK = new String(receivePacket.getData());
            int ACKSeqNum = Integer.parseInt(sentenceACK.split(":")[1].trim());
            System.out.println(sentenceACK); 


            if(ACKSeqNum == sentPackets){

               AllACK = false;
               i--;

               retransmitCounter++;

               if (retransmitCounter == 3) {
                  // retransmit packet lastAckedPacket + 1
                  String sentence = "gremio" + "|seq:" + sentPackets;
                  sendData = sentence.getBytes();
                  DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
                  clientSocket.send(sendPacket);
               
                  // reset duplicate ACK counter
                  congestWindow /= 2;
                  retransmitCounter = 0;
                  break;
               }

            }else{
               retransmitCounter=0;
               sentPackets++;
               if (sentPackets >= totalPackets) {break;}
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
      clientSocket.close();
   }
}
