import java.io.*;
import java.net.*;

public class Servidor {

    private static final int PORT = 9090;
    private static final String EXIT_MSG = "salir";

    public static void main(String[] args) {
        System.out.println("Servidor iniciado en puerto " + PORT + ". Esperando cliente...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                System.out.println("[LOG] Cliente: " + mensaje);

                if (mensaje.trim().equalsIgnoreCase(EXIT_MSG)) {
                    out.println("Conexion cerrada. Hasta luego!");
                    System.out.println("El cliente cerro la conexion.");
                    break;
                }

                String respuesta = procesarMensaje(mensaje);
                out.println(respuesta);
                System.out.println("[LOG] Servidor responde: " + respuesta);
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }

        System.out.println("Servidor finalizado.");
    }

    private static String procesarMensaje(String mensaje) {
        String upper = mensaje.trim().toUpperCase();

        if (upper.startsWith("RESOLVE")) {
            String expresion = mensaje.trim().substring(7).trim();
            // Eliminar comillas opcionales
            if (expresion.startsWith("\"") && expresion.endsWith("\"")) {
                expresion = expresion.substring(1, expresion.length() - 1);
            }
            try {
                double resultado = evaluar(expresion);
                // Mostrar sin decimales si es entero
                if (resultado == Math.floor(resultado) && !Double.isInfinite(resultado)) {
                    return "Resultado: " + (long) resultado;
                }
                return "Resultado: " + resultado;
            } catch (Exception e) {
                return "Error al evaluar la expresion: " + e.getMessage();
            }
        }

        return "Servidor recibio: " + mensaje;
    }

    // --- Evaluador de expresiones matematicas ---

    private static int pos;
    private static String expr;

    private static double evaluar(String expresion) {
        pos = 0;
        expr = expresion.replaceAll("\\s+", "");
        double resultado = parseExpresion();
        if (pos != expr.length()) {
            throw new RuntimeException("caracter inesperado en posicion " + pos);
        }
        return resultado;
    }

    // Suma y resta
    private static double parseExpresion() {
        double resultado = parseTerm();
        while (pos < expr.length() && (expr.charAt(pos) == '+' || expr.charAt(pos) == '-')) {
            char op = expr.charAt(pos++);
            double derecha = parseTerm();
            if (op == '+') resultado += derecha;
            else resultado -= derecha;
        }
        return resultado;
    }

    // Multiplicacion y division
    private static double parseTerm() {
        double resultado = parseFactor();
        while (pos < expr.length() && (expr.charAt(pos) == '*' || expr.charAt(pos) == '/')) {
            char op = expr.charAt(pos++);
            double derecha = parseFactor();
            if (op == '*') resultado *= derecha;
            else {
                if (derecha == 0) throw new RuntimeException("division por cero");
                resultado /= derecha;
            }
        }
        return resultado;
    }

    // Numero o subexpresion entre parentesis
    private static double parseFactor() {
        if (pos < expr.length() && expr.charAt(pos) == '(') {
            pos++; // consumir '('
            double resultado = parseExpresion();
            if (pos >= expr.length() || expr.charAt(pos) != ')') {
                throw new RuntimeException("parentesis sin cerrar");
            }
            pos++; // consumir ')'
            return resultado;
        }

        // Numero (con posible signo negativo unario)
        int inicio = pos;
        if (pos < expr.length() && expr.charAt(pos) == '-') pos++;
        while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
            pos++;
        }
        if (inicio == pos) throw new RuntimeException("numero esperado en posicion " + pos);
        return Double.parseDouble(expr.substring(inicio, pos));
    }
}
