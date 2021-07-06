package com.arc.proyecto.clientecar_v3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toolbar;

import com.arc.proyecto.clientecar_v3.Fragments.GraficaFragment;
import com.arc.proyecto.clientecar_v3.Fragments.LogoFragment;
import com.arc.proyecto.clientecar_v3.Utils.CoordenadaData;
import com.arc.proyecto.clientecar_v3.Utils.VecinoGraficoHilo;
import com.dd.processbutton.iml.ActionProcessButton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {
    private TextView textViewID;
    private EditText editTextIP, editTextPuerto;
    private ActionProcessButton buttonConectar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(threadPolicy);

        textViewID = (TextView) findViewById(R.id.tvIdCliente);
        editTextIP = (EditText) findViewById(R.id.etIpServidor);
        editTextPuerto = (EditText) findViewById(R.id.etPuertoServidor);
        buttonConectar = (ActionProcessButton) findViewById(R.id.btnConectar);


        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        editTextPuerto.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO){
                    buttonConectar.performClick();
                    return true;
                }
                return false;
            }
        });

        buttonConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean correcto = false;
                //COMPROBACIÓN DE IP
                String direccionServidor = editTextIP.getText().toString();
                if ( direccionServidor.isEmpty()){
                    correcto = false;
                    editTextIP.setError("Campo Obligatorio");
                    Toasty.warning(getBaseContext(),"Se necesita especificar la dirección del Servidor", Toasty.LENGTH_LONG).show();
                }else{
                    Pattern pattern = Pattern.compile("(0.|([1-9][0-9]?[0-9]?.)){3}(0|[1-9][0-9]?[0-9]?)");
                    Matcher matcher = pattern.matcher(direccionServidor);
                    if (matcher.matches()){
                        correcto = true;
                    }else{
                        correcto = false;
                        editTextIP.setText("192.168.");
                        Toasty.error(getBaseContext(),"Dirección del Servidor no válida!", Toasty.LENGTH_LONG).show();
                    }
                }

                //COMPROBACIÓN DE PUERTO
                String puertoServidor = editTextPuerto.getText().toString();
                if (correcto){
                    if (puertoServidor.isEmpty()){
                        editTextPuerto.setError("Campo Obligatorio");
                        correcto = false;
                        Toasty.warning(getBaseContext(),"Se necesita especificar el puerto del Servidor", Toasty.LENGTH_LONG).show();
                    }else{
                        Pattern pattern = Pattern.compile("[1-9]([0-9]{0,4})?");
                        Matcher matcher = pattern.matcher(puertoServidor);
                        if (matcher.matches()){
                            int puerto = Integer.parseInt(puertoServidor);
                            if (puerto > 65335){
                                correcto = false;
                                editTextPuerto.setText("");
                                Toasty.error(getBaseContext(),"Puerto no válido!", Toasty.LENGTH_LONG).show();
                            }
                            else{
                                correcto = true;
                            }
                        }else{
                            correcto = false;
                            editTextPuerto.setText("");
                            Toasty.error(getBaseContext(),"Puerto no válido!", Toasty.LENGTH_LONG).show();
                        }
                    }
                }

                if (correcto && !puertoServidor.isEmpty()){
                    ClienteHilo clienteHilo = new ClienteHilo(direccionServidor,Integer.parseInt(puertoServidor),20000,30000);
                    clienteHilo.start();
                }
            }
        });

        LogoFragment logoFragment = LogoFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container_fragment, logoFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.commit();
    }

    //CLIENTE HILO GRÁFICO
    class ClienteHilo extends Thread{
        private CoordenadaData[] vecinos;
        private String IP_SERVIDOR; //Dirección del Servidor
        private int PORT_TCP; //Puerto del Servidor
        private int PORT_UDP_COORDENADA;
        private int PORT_UDP_RECONOCIMIENTO;

        private int PORT_UDP_COORDENADA_SERVER;  //En la fase de simulacion (enviarCoordenadas al servidor)
        private int PORT_UDP_RECONOCIMIENTO_SERVER; // En la fase de simulacion (enviarReconocimientos al servidor)
        private Socket socketTCP; //Socket (TCP) para conectar al servidor fase inicial
        private DatagramSocket socketUDP_coordenada; //Socket (UDP) para recibir coordenadas y enviar ack al servidor
        private DatagramSocket socketUDP_ack; //Socket (UDP) para recibir los acks del servidor
        private DataInputStream dis; //Flujo de datos de entrada
        private DataOutputStream dos; //Flujo de datos de salida
        private VecinoGraficoHilo vecinoGraficoHilo;

        private int num_vecinos; // V (Nº de vecinos que tiene el cliente)
        private int iteraciones; // S (Nº de iteraciones que se realizará la fase de simulación)
        private int id; // ID del Cliente
        private int id_group; // ID del grupo de trabajo del Cliente
        private int ack_recibidos = 0; // contador de reconocimientos recibidos

        /*COORDENADAS DEL CLIENTE*/
        private int x;
        private int y;
        private final int MAX_X = 100;
        private final int MAX_Y = 100;
        private float media_tiempo = 0; // almacenamos el tiempo medio de cada iteración

        private LocalDateTime initial_ciclo;
        private LocalDateTime final_ciclo;
        private LocalDateTime initial_time; //Tiempo en el cual se envio las coordenadas
        private LocalDateTime final_time; //Tiempo al recibir el ultimo reconocimiento


        public ClienteHilo(String ip_servidor, int port_tcp, int port_upd_coord, int port_udp_ack) {
            this.IP_SERVIDOR = ip_servidor;
            this.PORT_TCP = port_tcp;
            this.PORT_UDP_COORDENADA = port_upd_coord;
            this.PORT_UDP_RECONOCIMIENTO = port_udp_ack;
            try {
                socketTCP = new Socket(ip_servidor, PORT_TCP);
                socketUDP_coordenada = new DatagramSocket(PORT_UDP_COORDENADA);
                socketUDP_ack = new DatagramSocket(PORT_UDP_RECONOCIMIENTO);
            } catch (IOException ex) {
                buttonConectar.setProgress(0);
                buttonConectar.setBackgroundColor(getColor(R.color.Rojo));
                buttonConectar.setText("DESCONECTADO");
                System.out.println(ex.getMessage());
            }
        }

        @Override
        public void run() {

            try {
                //-------------------FASE DE INICIO (TCP)-----------------------------
                dis = new DataInputStream(socketTCP.getInputStream());
                dos = new DataOutputStream(socketTCP.getOutputStream());

                dos.writeInt(PORT_UDP_COORDENADA);      //Puerto UDP del cliente para las coordenadas
                dos.writeInt(PORT_UDP_RECONOCIMIENTO);  //Puerto UDP del cliente para los reconocimientos

                id = dis.readInt(); //El servidor nos asigna un ID de cliente
                id_group = dis.readInt(); //El servidor nos asigna un ID del grupo de trabajo de este cliente
                num_vecinos = dis.readInt(); //El servidor nos indica el nº de clientes que hay en mi grupo de trabajo (contandome a mi también)
                num_vecinos = num_vecinos - 1; //Yo como cliente no me cuento como un vecino más de ese grupo de trabajo.
                this.vecinos = new CoordenadaData[num_vecinos + 1];
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        GraficaFragment graficaFragmentInicial = GraficaFragment.newInstance(num_vecinos,id_group,vecinos, true,MAX_X,MAX_Y);
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.container_fragment, graficaFragmentInicial);
                        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                        transaction.commit();
                        buttonConectar.setProgress(0);
                        buttonConectar.setBackgroundColor(getColor(R.color.Verde));
                        buttonConectar.setText("CONECTADO");
                        buttonConectar.setEnabled(false);
                        textViewID.setText(String.valueOf(id));

                        Toasty.success(getApplicationContext(),"Conexión establecida", Toasty.LENGTH_LONG).show();
                    }
                });
                for (int i = 0; i < vecinos.length; i++) {
                    CoordenadaData nuevaCoordenada  = new CoordenadaData(3, 3);
                    vecinos[i] = nuevaCoordenada;
                }

                iteraciones = dis.readInt();  //El servidor nos indica el nº de iteraciones

                PORT_TCP = dis.readInt();
                PORT_UDP_COORDENADA_SERVER = dis.readInt(); //Puerto UDP del servidor para las coordenadas
                PORT_UDP_RECONOCIMIENTO_SERVER = dis.readInt(); //Puerto UDP del servidor para los reconocimientos

                String senyal_inicial = dis.readUTF(); //El servidor nos indica la señal de inicio de la fase de simulación
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buttonConectar.setProgress(1);
                        buttonConectar.setBackgroundColor(getColor(R.color.Amarillo));
                        buttonConectar.setBackgroundTintMode(PorterDuff.Mode.ADD);
                        buttonConectar.setText("SIMULANDO");

                        Toasty.warning(getApplicationContext(),"Simulación Iniciada", Toasty.LENGTH_SHORT).show();
                    }
                });


                vecinoGraficoHilo = new VecinoGraficoHilo(socketUDP_coordenada, PORT_UDP_RECONOCIMIENTO_SERVER, this.vecinos);
                vecinoGraficoHilo.start();

                int it = 0;
                while (it != iteraciones) {
                    generarCoordenadas(); //Se genera en cada iteración nuevas coordenadas para este cliente
                    enviarCoordenadas(); //Se envian las coordenadas del Cliente al Servidor


                    ack_recibidos = 0;
                    final_ciclo = LocalDateTime.now();
                    try {
                        while ( (ack_recibidos != num_vecinos) ) {
                            final_ciclo = LocalDateTime.now();

                            byte[] buffer = new byte[1024];
                            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                            socketUDP_ack.setSoTimeout(2000);
                            socketUDP_ack.receive(datagram); //Recibo los reconocimientos de los vecinos retransmitidos por el servidor
                            ack_recibidos++;

                        }

                    }
                    catch (SocketTimeoutException e)
                    {
                        // System.err.println("SocketCerrado");
                    }
                    catch (IOException e)
                    {
                        //e.printStackTrace();
                    }

                    final_time = LocalDateTime.now(); //Guardamos el tiempo final, al recibir el ultimo reconocimiento del vecino.



                    actualizarMediaTiempo();//Actualizo media de tiempo de este ciclo

                    it++; //Incremento la iteración
                }
                ////------------------FASE DE CÁLCULO (TCP)----------------

                enviarMediaFinal();

            } catch (IOException e) {
                buttonConectar.setProgress(0);
                buttonConectar.setBackgroundColor(getColor(R.color.Rojo));
                buttonConectar.setText("ERROR");
                System.out.println(e.getMessage());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i= 0; i < vecinos.length;i++){
                        Log.e("GRAFICA","("+vecinos[i].getX()+","+vecinos[i].getY()+")");
                    }
                    GraficaFragment graficaFragmentFinal = GraficaFragment.newInstance(num_vecinos,id_group,vecinos, false,MAX_X,MAX_Y);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.container_fragment, graficaFragmentFinal);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    transaction.commit();

                    buttonConectar.setProgress(0);
                    buttonConectar.setBackgroundColor(getColor(R.color.RojoRosa));
                    buttonConectar.setText("FINALIZADO");
                    Toasty.success(getApplicationContext(),"Simulación Finalizada", Toasty.LENGTH_LONG).show();
                }
            });
            //cerrarSockets();
        }

        public void generarCoordenadas() {
            Random posX = new Random();
            Random posY = new Random();

            this.x = posX.nextInt(MAX_X);
            this.y = posY.nextInt(MAX_Y);

            CoordenadaData coord_recibido = new CoordenadaData(this.x, this.y);
            vecinos[this.id]= coord_recibido;
        }

        private void enviarCoordenadas() {
            initial_time = LocalDateTime.now(); //Guardamos el tiempo inicial del envio de las coordenadas
            initial_ciclo = LocalDateTime.now();

            try {
                InetAddress addressServer = InetAddress.getByName(IP_SERVIDOR);
                String coordenada = id + "," + x + "," + y + ",";
                byte[] buffer = coordenada.getBytes();
                DatagramPacket datagram_coordenada = new DatagramPacket(buffer, buffer.length, addressServer, PORT_UDP_COORDENADA_SERVER);
                socketUDP_coordenada.send(datagram_coordenada); //Envio de las coordenadas al servidor

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        private void actualizarMediaTiempo() {
            media_tiempo += Duration.between(initial_time, final_time).toMillis();
        }

        private void enviarMediaFinal() {
            try {

                System.out.println("envio media: " + media_tiempo);
                DataOutputStream dataOutputStream = new DataOutputStream(socketTCP.getOutputStream());

                dataOutputStream.writeInt((int) media_tiempo);

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        private void cerrarSockets() {
            try {
                vecinoGraficoHilo.interrupt();
                socketTCP.close();
                socketUDP_coordenada.close();
                socketUDP_ack.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
