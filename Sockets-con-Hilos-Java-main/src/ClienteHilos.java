import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteHilos {
    public static void main(String[] args) {
        try (Socket s = new Socket("127.0.0.1", 7005);
             DataInputStream in = new DataInputStream(s.getInputStream());
             DataOutputStream out = new DataOutputStream(s.getOutputStream())) {

            Scanner sc = new Scanner(System.in);

            // Enviar nombre deseado al servidor
            System.out.print("Ingresa tu nombre de usuario: ");
            String nombreDeseado = sc.nextLine().trim();
            if (nombreDeseado.isEmpty()) nombreDeseado = "Usuario";
            out.writeUTF(nombreDeseado);

            // Hilo receptor: imprime mensajes que llegan del servidor
            Thread receptor = new Thread(() -> {
                try {
                    while (true) {
                        System.out.println("\n" + in.readUTF());
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.out.println("Conexion cerrada por el servidor.");
                }
            });
            receptor.setDaemon(true);
            receptor.start();

            // Hilo principal: envía comandos al servidor
            String input = "";
            while (!input.equalsIgnoreCase("salir")) {
                System.out.print("> ");
                input = sc.nextLine().trim();
                if (!input.isEmpty()) {
                    out.writeUTF(input);
                }
            }

        } catch (IOException e) {
            System.out.println("Error de conexion: " + e.getMessage());
        }
    }
}
