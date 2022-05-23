package com.hadesfranklyn.chat;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Aplica��o cliente de chat utilizando a classe {@link Socket},
 * que permite apenas requisi��es bloqueantes (blocking).
 * 
 * <p>Observe que a classe implementa a interface {@link Runnable}.
 * Com isto, o m�todo {@link #run()} foi inclu�do (pressionando-se
 * ALT-ENTER ap�s incluir o "implements Runnable")
 * para que ele seja executado por uma nova thread que criamos
 * dentro do {@link #messageLoop()}.
 * O m�todo {@link #run()} fica em loop aguardando
 * mensagens do servidor.</p>
 *
 * @author Franklyn Roberto da Silva
 */
public class BlockingChatClientApp implements Runnable {
    /**
     * Endere�o IP ou nome DNS para conectar no servidor.
     * O n�mero da porta � obtido diretamente da constante {@link BlockingChatServerApp#PORT}
     * na classe do servidor.
     */
    public static final String SERVER_ADDRESS = "127.0.0.1";

    /**
     * Objeto para capturar dados do teclado e assim
     * permitir que o usu�rio digite mensagens a enviar.
     */
    private final Scanner scanner;
    
    /**
     * Objeto que armazena alguns dados do cliente (como o login)
     * e o {@link Socket} que representa a conex�o do cliente com o servidor.
     */
    private ClientSocket clientSocket;
    
    /**
     * Executa a aplica��o cliente.
     * Pode-se executar quantas inst�ncias desta classe desejar.
     * Isto permite ter v�rios clientes conectados e interagindo
     * por meio do servidor.
     * 
     * @param args par�metros de linha de comando (n�o usados para esta aplica��o)
     */
    public static void main(String[] args) {
        try {
            BlockingChatClientApp client = new BlockingChatClientApp();
            client.start();
        } catch (IOException e) {
            System.out.println("Erro ao conectar ao servidor: " + e.getMessage());
        }
    }
    
    /**
     * Instancia um cliente, realizando o m�nimo de opera��es necess�rias.
     */
    public BlockingChatClientApp(){
        scanner = new Scanner(System.in);
    }

    /**
     * Inicia o cliente, conectando ao servidor e
     * entrando no loop de envio e recebimento de mensagens.
     * @throws IOException quando um erro de I/O (Input/Output, ou seja,
     *                     Entrada/Sa�da) ocorrer, como quando o cliente tentar
     *                     conectar no servidor, mas o servidor n�o est� aberto
     *                     ou o cliente n�o tem acesso � rede.
     */
    private void start() throws IOException {
        final Socket socket = new Socket(SERVER_ADDRESS, BlockingChatServerApp.PORT);
        clientSocket = new ClientSocket(socket);
        System.out.println(
            "Cliente conectado ao servidor no endere�o " + SERVER_ADDRESS +
            " e porta " + BlockingChatServerApp.PORT);

        login();

        new Thread(this).start();
        messageLoop();
    }

    /**
     * Executa o login no sistema, enviando o login digitado para o servidor.
     * A primeira mensagem que o servidor receber ap�s um cliente conectar � ent�o o login daquele cliente.
     */
    private void login() {
        System.out.print("Digite seu login: ");
        final String login = scanner.nextLine();
        clientSocket.setLogin(login);
        clientSocket.sendMsg(login);
    }

    /**
     * Inicia o loop de envio e recebimento de mensagens.
     * O loop � interrompido quando o usu�rio digitar "sair".
     */
    private void messageLoop() {
        String msg;
        do {
            System.out.print("Digite uma msg (ou 'sair' para encerrar): ");
            msg = scanner.nextLine();
            clientSocket.sendMsg(msg);
        } while(!"sair".equalsIgnoreCase(msg));
        clientSocket.close();
    }

    /**
     * Aguarda mensagens do servidor enquanto o socket n�o for fechado
     * e o cliente n�o receber uma mensagem null.
     * Se uma mensagem null for recebida, � porque ocorreu erro na conex�o com o servidor.
     * Neste caso, podemos encerrar a espera por novas mensagens.
     * 
     * <p>
     * O m�todo tem esse nome pois estamos implementando a interface {@link Runnable}
     * na declara��o da classe, o que nos obriga a incluir um m�todo com tal nome
     * na nossa classe. Com isto, permitimos que tal m�todo possa ser executado
     * por uma nova thread que criamos no m�todo {@link #messageLoop()},
     * o que facilita a cria��o da thread.
     * </p>
     */
    @Override
    public void run() {
        String msg;
        while((msg = clientSocket.getMessage())!=null) {
            System.out.println(msg);
        }
    }
}
