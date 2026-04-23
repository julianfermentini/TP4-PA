import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorHilos {
    // Lista para guardar a todos los clientes conectados
    public static Map<String, DataOutputStream> clientes = new ConcurrentHashMap<>();
    private static int contador = 1;

    public static void main(String[] args) {
        int puerto = 7005;
        try (ServerSocket server = new ServerSocket(puerto)) {
            server.setReuseAddress(true);
            System.out.println(">>> SERVIDOR MULTIHILO CORRIENDO EN PUERTO " + puerto);

            while (true) {
                Socket socket = server.accept();
                String nombre = "User" + (contador++);
                System.out.println("[CONEXIÓN]: " + nombre + " se ha unido.");

                // Creamos un hilo para este cliente
                Thread hilo = new Thread(new Manejador(socket, nombre));
                hilo.start();
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }
}

class Manejador implements Runnable {
    private Socket socket;
    private String nombre;

    public Manejador(Socket socket, String nombre) {
        this.socket = socket;
        this.nombre = nombre;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            // Guardamos al cliente en el mapa global
            ServidorHilos.clientes.put(nombre, out);

            out.writeUTF("Bienvenido " + nombre + ". Comandos: ALL mensaje, LIST, salir.");

            String msg;
            while (!(msg = in.readUTF()).equalsIgnoreCase("salir")) {
                System.out.println("[LOG " + nombre + "]: " + msg);

                if (msg.startsWith("ALL ")) {
                    // Enviar a todos
                    String contenido = msg.substring(4);
                    for (DataOutputStream d : ServidorHilos.clientes.values()) {
                        d.writeUTF(nombre + " (Global): " + contenido);
                    }
                } else if (msg.equalsIgnoreCase("LIST")) {
                    out.writeUTF("Conectados: " + ServidorHilos.clientes.keySet());
                } else {
                    out.writeUTF("Servidor recibió: " + msg);
                }
            }
        } catch (IOException e) {
            System.out.println(nombre + " se desconectó bruscamente.");
        } finally {
            ServidorHilos.clientes.remove(nombre);
            try { socket.close(); } catch (IOException e) { }
        }
    }
}