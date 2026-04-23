import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteHilos {
    public static void main(String[] args) {
        try (Socket s = new Socket("127.0.0.1", 7005);
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            // Hilo para escuchar lo que llega del servidor
            new Thread(() -> {
                try {
                    while (true) {
                        System.out.println("\n" + in.readUTF());
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.out.println("Conexión cerrada.");
                }
            }).start();

            // Hilo principal para enviar mensajes
            Scanner sc = new Scanner(System.in);
            String input = "";
            while (!input.equalsIgnoreCase("salir")) {
                input = sc.nextLine();
                out.writeUTF(input);
            }
        } catch (IOException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
    }
}