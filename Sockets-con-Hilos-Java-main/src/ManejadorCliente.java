import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ManejadorCliente extends Thread {
    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream salida;
    private String nombreUsuario;
    private Map<String, ManejadorCliente> clientesConectados;

    public ManejadorCliente(Socket socket, String nombre, Map<String, ManejadorCliente> clientes) {
        this.socket = socket;
        this.nombreUsuario = nombre;
        this.clientesConectados = clientes;
    }

    @Override
    public void run() {
        try {
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            // Menú de bienvenida
            salida.writeUTF("=== BIENVENIDO " + nombreUsuario + " ===\n" +
                    "Comandos disponibles:\n" +
                    "- TIME: Ver fecha y hora\n" +
                    "- RESOLVE [operacion]: Resolver matemática\n" +
                    "- LIST: Listar usuarios conectados\n" +
                    "- [NombreUsuario] [Mensaje]: Mensaje privado\n" +
                    "- ALL [Mensaje]: Mensaje a todos\n" +
                    "- salir: Desconectarse");

            String mensaje;
            while (true) {
                mensaje = entrada.readUTF();
                System.out.println("[LOG " + nombreUsuario + "]: " + mensaje);

                if (mensaje.equalsIgnoreCase("salir")) break;

                procesarComando(mensaje);
            }
        } catch (IOException e) {
            System.out.println("Conexión perdida con " + nombreUsuario);
        } finally {
            desconectar();
        }
    }

    private void procesarComando(String msg) throws IOException {
        String msgUpper = msg.toUpperCase();

        if (msgUpper.equals("TIME")) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            salida.writeUTF("Servidor: La fecha y hora es " + dtf.format(LocalDateTime.now()));

        } else if (msgUpper.equals("LIST")) {
            salida.writeUTF("Conectados: " + clientesConectados.keySet().toString());

        } else if (msgUpper.startsWith("RESOLVE")) {
            salida.writeUTF("Servidor: [Cálculo procesado]"); // Aquí iría tu lógica matemática

        } else if (msgUpper.startsWith("ALL ")) {
            String texto = msg.substring(4);
            broadcast(nombreUsuario + " (Global): " + texto);

        } else {
            // Lógica de mensaje privado
            String[] partes = msg.split(" ", 2);
            if (partes.length == 2) {
                String destino = partes[0];
                String texto = partes[1];
                enviarPrivado(destino, texto);
            } else {
                salida.writeUTF("Servidor: Comando no reconocido.");
            }
        }
    }

    private void enviarPrivado(String destino, String texto) throws IOException {
        ManejadorCliente receptor = clientesConectados.get(destino);
        if (receptor != null) {
            receptor.salida.writeUTF(nombreUsuario + " (privado): " + texto);
        } else {
            salida.writeUTF("Error: El usuario " + destino + " no existe.");
        }
    }

    private void broadcast(String mensaje) throws IOException {
        for (ManejadorCliente cliente : clientesConectados.values()) {
            cliente.salida.writeUTF(mensaje);
        }
    }

    private void desconectar() {
        try {
            clientesConectados.remove(nombreUsuario);
            broadcast("Servidor: " + nombreUsuario + " ha salido del chat.");
            socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}