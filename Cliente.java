import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {

    private static final String HOST = "localhost";
    private static final int PORT = 9090;
    private static final String EXIT_MSG = "salir";

    public static void main(String[] args) {
        System.out.println("Conectando al servidor " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conectado! Escribe mensajes para el servidor.");
            System.out.println("Usa RESOLVE \"<expresion>\" para resolver operaciones matematicas.");
            System.out.println("Escribe '" + EXIT_MSG + "' para desconectarte.\n");

            while (true) {
                System.out.print("Tu: ");
                String mensaje = scanner.nextLine();

                out.println(mensaje);

                String respuesta = in.readLine();
                System.out.println("Servidor: " + respuesta);

                if (mensaje.trim().equalsIgnoreCase(EXIT_MSG)) {
                    break;
                }
            }

        } catch (ConnectException e) {
            System.err.println("No se pudo conectar al servidor. Asegurate de que este ejecutandose.");
        } catch (IOException e) {
            System.err.println("Error de conexion: " + e.getMessage());
        }

        System.out.println("Conexion cerrada.");
    }
}
