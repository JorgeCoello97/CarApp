package com.arc.proyecto.clientecar_v3.Utils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;


public class VecinoGraficoHilo extends Thread {

    private DatagramSocket socketUDP_coordenada; //Socket (UDP) para recibir coordenadas y enviar ack al servidor
    private int port_udp_reconocimiento_server;
    private CoordenadaData[] vecinos;


    public VecinoGraficoHilo(DatagramSocket socketUDP_coordenada, int port_udp_reconocimiento_server, CoordenadaData[] vecinos) {
        this.socketUDP_coordenada = socketUDP_coordenada;
        this.port_udp_reconocimiento_server = port_udp_reconocimiento_server;
        this.vecinos = vecinos;
    }

    @Override
    public void run() {

        while (Thread.currentThread().isInterrupted() != true) {
            try {
                if (!socketUDP_coordenada.isClosed()) {
                    //---------------RECIBO COORDENADAS DEL SERVIDOR ( Servidor -> Vecino )--------------------
                    byte[] buffer = new byte[21];
                    DatagramPacket datagram_coordenada_nei = new DatagramPacket(buffer, buffer.length);
                    this.socketUDP_coordenada.receive(datagram_coordenada_nei); //Recibo de las coordenadas del cliente, retransmitidas por el servidor

                    String coordenada_recibida = new String(datagram_coordenada_nei.getData());
                    StringTokenizer st = new StringTokenizer(coordenada_recibida, ",");

                    int id_recibido = Integer.valueOf(st.nextToken());
                    int coord_x = Integer.valueOf(st.nextToken().trim());
                    int coord_y = Integer.valueOf(st.nextToken().trim());
                    System.err.println(coord_x);
                    System.err.println(coord_y);
                    CoordenadaData coord_recibido = new CoordenadaData(coord_x, coord_y);
                    vecinos[id_recibido]= coord_recibido;

                    //---------------ENVIO RECONOCIMIENTO AL SERVIDOR ( Vecino -> Servidor )--------------------
                    InetAddress addressServer = InetAddress.getByName("localhost");
                    String message = id_recibido + ",ACK";
                    byte[] buffer_mensaje = message.getBytes();

                    DatagramPacket datagram_ack = new DatagramPacket(buffer_mensaje, buffer_mensaje.length, addressServer, port_udp_reconocimiento_server);
                    socketUDP_coordenada.send(datagram_ack); //Envio del reconomiento al servidor

                }
            } catch (IOException e) {
                if (currentThread().isInterrupted() == false) {
                    e.printStackTrace();
                }
            }
        }
    }
}
