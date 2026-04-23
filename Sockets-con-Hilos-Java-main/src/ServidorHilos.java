import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorHilos {
    public static Map<String, DataOutputStream> clientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int puerto = 7005;
        try (ServerSocket server = new ServerSocket(puerto)) {
            server.setReuseAddress(true);
            log("SERVIDOR MULTIHILO CORRIENDO EN PUERTO " + puerto);

            while (true) {
                Socket socket = server.accept();
                log("Nueva conexión entrante desde " + socket.getInetAddress());
                new Thread(new Manejador(socket)).start();
            }
        } catch (IOException e) {
            log("Error en el servidor: " + e.getMessage());
        }
    }

    public static void log(String msg) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("[" + ts + "] " + msg);
    }

    // Asigna nombre único: si "Juan" está ocupado, prueba "Juan2", "Juan3", etc.
    public static synchronized String asignarNombre(String deseado) {
        if (deseado == null || deseado.isBlank()) deseado = "Usuario";
        if (!clientes.containsKey(deseado)) return deseado;
        int n = 2;
        while (clientes.containsKey(deseado + n)) n++;
        return deseado + n;
    }
}

class Manejador implements Runnable {
    private Socket socket;
    private String nombre;

    Manejador(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        DataOutputStream out = null;
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // Primer mensaje del cliente: nombre deseado
            String deseado = in.readUTF().trim();
            nombre = ServidorHilos.asignarNombre(deseado);
            ServidorHilos.clientes.put(nombre, out);
            ServidorHilos.log("[CONEXION] " + nombre + " conectado (solicitó: \"" + deseado + "\")");

            // Menú de bienvenida
            StringBuilder bienvenida = new StringBuilder();
            bienvenida.append("=== BIENVENIDO, ").append(nombre).append(" ===\n");
            if (!nombre.equals(deseado)) {
                bienvenida.append("(El nombre \"").append(deseado)
                          .append("\" ya estaba en uso. Se te asignó: ").append(nombre).append(")\n");
            }
            bienvenida.append("\nCOMANDOS DISPONIBLES:\n");
            bienvenida.append("  HORA                        Consultar fecha y hora actual\n");
            bienvenida.append("  CALC <expresion>            Resolver expresion matematica (ej: CALC 3+5*2)\n");
            bienvenida.append("  LIST                        Listar clientes conectados\n");
            bienvenida.append("  MSG <dest1>[,dest2] <msg>   Enviar mensaje privado a uno o dos usuarios\n");
            bienvenida.append("  ALL <msg>                   Enviar mensaje a todos los clientes\n");
            bienvenida.append("  salir                       Desconectarse\n");
            out.writeUTF(bienvenida.toString());

            String msg;
            while (true) {
                msg = in.readUTF();
                if (msg.equalsIgnoreCase("salir")) break;
                ServidorHilos.log("[" + nombre + "] " + msg);
                procesarComando(msg, out);
            }

            ServidorHilos.log("[DESCONEXION] " + nombre + " se desconecto.");
            out.writeUTF("Hasta luego, " + nombre + "!");

        } catch (IOException e) {
            ServidorHilos.log("[ERROR] " + (nombre != null ? nombre : "desconocido")
                              + " se desconecto abruptamente.");
        } finally {
            if (nombre != null) ServidorHilos.clientes.remove(nombre);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void procesarComando(String msg, DataOutputStream out) throws IOException {
        String upper = msg.trim().toUpperCase();

        if (upper.equals("HORA")) {
            String dt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            out.writeUTF("Fecha y hora: " + dt);

        } else if (upper.startsWith("CALC ")) {
            String expr = msg.substring(5).trim();
            try {
                double res = new ExprParser(expr.replaceAll("\\s+", "")).parse();
                String resStr = (res == Math.floor(res) && !Double.isInfinite(res))
                    ? String.valueOf((long) res) : String.valueOf(res);
                out.writeUTF("Resultado: [" + expr + "] = " + resStr);
            } catch (Exception e) {
                out.writeUTF("Error: expresion invalida \"" + expr + "\". Ejemplo: CALC 3+5*(2-1)");
            }

        } else if (upper.equals("LIST")) {
            Set<String> conectados = ServidorHilos.clientes.keySet();
            out.writeUTF("Clientes conectados (" + conectados.size() + "): "
                         + String.join(", ", conectados));

        } else if (upper.startsWith("MSG ")) {
            // MSG dest1[,dest2] mensaje
            String resto = msg.substring(4).trim();
            int espacio = resto.indexOf(' ');
            if (espacio == -1) {
                out.writeUTF("Uso: MSG <destino> <mensaje>   (ej: MSG Juan hola  o  MSG Juan,Pedro hola)");
                return;
            }
            String destinosStr = resto.substring(0, espacio);
            String contenido = resto.substring(espacio + 1);
            String[] destinos = destinosStr.split(",");

            List<String> enviados = new ArrayList<>();
            List<String> noEncontrados = new ArrayList<>();

            for (String dest : destinos) {
                dest = dest.trim();
                if (dest.isEmpty()) continue;
                DataOutputStream destOut = ServidorHilos.clientes.get(dest);
                if (destOut != null) {
                    destOut.writeUTF("[Privado de " + nombre + "]: " + contenido);
                    enviados.add(dest);
                } else {
                    noEncontrados.add(dest);
                }
            }

            StringBuilder respuesta = new StringBuilder();
            if (!enviados.isEmpty())
                respuesta.append("Mensaje enviado a: ").append(String.join(", ", enviados)).append("\n");
            if (!noEncontrados.isEmpty())
                respuesta.append("Usuario(s) no encontrado(s): ").append(String.join(", ", noEncontrados));
            out.writeUTF(respuesta.toString().trim());

        } else if (upper.startsWith("ALL ")) {
            String contenido = msg.substring(4);
            for (DataOutputStream d : ServidorHilos.clientes.values()) {
                d.writeUTF("[" + nombre + " -> Todos]: " + contenido);
            }

        } else {
            out.writeUTF("Comando desconocido: \"" + msg + "\". Comandos: HORA, CALC, LIST, MSG, ALL, salir");
        }
    }
}

// Evaluador de expresiones matematicas (+, -, *, /, %, parentesis, numeros decimales)
class ExprParser {
    private final String expr;
    private int pos;

    ExprParser(String expr) { this.expr = expr; this.pos = 0; }

    double parse() {
        double result = parseAdditiva();
        if (pos < expr.length())
            throw new RuntimeException("Caracter inesperado: " + expr.charAt(pos));
        return result;
    }

    private double parseAdditiva() {
        double left = parseMultiplicativa();
        while (pos < expr.length() && (expr.charAt(pos) == '+' || expr.charAt(pos) == '-')) {
            char op = expr.charAt(pos++);
            double right = parseMultiplicativa();
            left = (op == '+') ? left + right : left - right;
        }
        return left;
    }

    private double parseMultiplicativa() {
        double left = parseUnaria();
        while (pos < expr.length()
               && (expr.charAt(pos) == '*' || expr.charAt(pos) == '/' || expr.charAt(pos) == '%')) {
            char op = expr.charAt(pos++);
            double right = parseUnaria();
            if (op == '*') left *= right;
            else if (op == '/') {
                if (right == 0) throw new RuntimeException("Division por cero");
                left /= right;
            } else left %= right;
        }
        return left;
    }

    private double parseUnaria() {
        if (pos < expr.length() && expr.charAt(pos) == '-') { pos++; return -parsePrimaria(); }
        if (pos < expr.length() && expr.charAt(pos) == '+') { pos++; }
        return parsePrimaria();
    }

    private double parsePrimaria() {
        if (pos < expr.length() && expr.charAt(pos) == '(') {
            pos++;
            double val = parseAdditiva();
            if (pos >= expr.length() || expr.charAt(pos) != ')')
                throw new RuntimeException("Falta parentesis de cierre");
            pos++;
            return val;
        }
        int start = pos;
        while (pos < expr.length()
               && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) pos++;
        if (start == pos)
            throw new RuntimeException("Se esperaba un numero en posicion " + pos);
        return Double.parseDouble(expr.substring(start, pos));
    }
}
