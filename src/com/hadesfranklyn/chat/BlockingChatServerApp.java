package com.hadesfranklyn.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Aplica��o servidora de chat utilizando a classe {@link ServerSocket}, 
 * que permite apenas requisi��es bloqueantes (blocking).
 *
 * @author Franklyn Roberto da Silva
 */
public class BlockingChatServerApp {
    /**
     * Porta na qual o servidor vai ficar escutando (aguardando conex�es dos clientes).
     * Em um determinado computador s� pode haver uma �nica aplica��o servidora
     * escutando em uma porta espec�fica.
     */
    public static final int PORT = 4000;

    /**
     * Objeto que permite ao servidor ficar escutando na porta especificada acima.
     */
    private ServerSocket serverSocket;

    /**
     * Lista de todos os clientes conectados ao servidor.
     */
    private final List<ClientSocket> clientSocketList;

    public BlockingChatServerApp() {
        clientSocketList = new LinkedList<>();
    }

    /**
     * Executa a aplica��o servidora que fica em loop infinito aguardando conex�es
     * dos clientes.
     * @param args par�metros de linha de comando (n�o usados para esta aplica��o)
     */
    public static void main(String[] args) {
        final BlockingChatServerApp server = new BlockingChatServerApp();
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }

    /**
     * Inicia a aplica��o, criando um socket para o servidor
     * ficar escutando na porta {@link #PORT}.
     * 
     * @throws IOException quando um erro de I/O (Input/Output, ou seja, Entrada/Sa�da) ocorrer,
     *                     como quando o servidor tentar iniciar mas a porta que ele deseja
     *                     escutar j� estiver em uso
     */
    private void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println(
                "Servidor de chat bloqueante iniciado no endere�o " + serverSocket.getInetAddress().getHostAddress() +
                " e porta " + PORT);
        clientConnectionLoop();
    }

    /**
     * Inicia o loop infinito de espera por conex�es dos clientes. Cada vez que um
     * cliente conecta, uma {@link Thread} � criada para executar o m�todo
     * {@link #clientMessageLoop(com.hadesfranklyn.chat.ClientSocket)} que ficar�
     * esperando mensagens do cliente.
     * 
     * @throws IOException quando um erro de I/O (Input/Output, ou seja,
     *                     Entrada/Sa�da) ocorrer, como quando o servidor tentar
     *                     aceitar a conex�o de um cliente, mas ele desconectar
     *                     antes disso (porque a conex�o dele ou do servidor cairam, por exemplo)
     */
    private void clientConnectionLoop() throws IOException {
        try {
            while (true) {
                System.out.println("Aguardando conex�o de novo cliente");
                
                final ClientSocket clientSocket;
                try {
                    clientSocket = new ClientSocket(serverSocket.accept());
                    System.out.println("Cliente " + clientSocket.getRemoteSocketAddress() + " conectado");
                }catch(SocketException e){
                    System.err.println("Erro ao aceitar conex�o do cliente. O servidor possivelmente est� sobrecarregado:");
                    System.err.println(e.getMessage());
                    continue;
                }

                /*
                Cria uma nova Thread para permitir que o servidor n�o fique bloqueado enquanto
                atende �s requisi��es de um �nico cliente.
                */
                try {
                    new Thread(() -> clientMessageLoop(clientSocket)).start();
                    clientSocketList.add(clientSocket);
                }catch(OutOfMemoryError ex){
                    System.err.println(
                            "N�o foi poss�vel criar thread para novo cliente. O servidor possivelmente est� sobrecarregdo. Conex�o ser� fechada: ");
                    System.err.println(ex.getMessage());
                    clientSocket.close();
                }
            }
        } finally{
            /*Se sair do la�o de repeti��o por algum erro, exibe uma mensagem
            indicando que o servidor finalizou e fecha o socket do servidor.*/
            stop();
        }
    }

    /**
     * M�todo executado sempre que um cliente conectar ao servidor.
     * O m�todo fica em loop aguardando mensagens do cliente,
     * at� que este desconecte.
     * A primeira mensagem que o servidor receber ap�s um cliente conectar � o login enviado pelo cliente.
     * 
     * @param clientSocket socket do cliente, por meio do qual o servidor
     *                     pode se comunicar com ele.
     */
    private void clientMessageLoop(final ClientSocket clientSocket){
        try {
            String msg;
            while((msg = clientSocket.getMessage()) != null){
                final SocketAddress clientIP = clientSocket.getRemoteSocketAddress();
                if("sair".equalsIgnoreCase(msg)){
                    return;
                }

                if(clientSocket.getLogin() == null){
                    clientSocket.setLogin(msg);
                    System.out.println("Cliente "+ clientIP + " logado como " + clientSocket.getLogin() +".");
                    msg = "Cliente " + clientSocket.getLogin() + " logado.";
                }
                else {
                    System.out.println("Mensagem recebida de "+ clientSocket.getLogin() +": " + msg);
                    msg = clientSocket.getLogin() + " diz: " + msg;
                };

                sendMsgToAll(clientSocket, msg);
            }
        } finally {
            clientSocket.close();
        }
    }
    
    /**
     * Encaminha uma mensagem recebida de um determinado cliente
     * para todos os outros clientes conectados.
     *
     * <p>
     * Usa um iterator para permitir percorrer a lista de clientes conectados.
     * Neste caso n�o � usado um for pois, como estamos removendo um cliente
     * da lista caso n�o consigamos enviar mensagem pra ele (pois ele j�
     * desconectou), se fizermos isso usando um foreach, ocorrer�
     * erro em tempo de execu��o. Um foreach n�o permite percorrer e modificar
     * uma lista ao mesmo tempo. Assim, a forma mais segura de fazer
     * isso � com um iterator.
     * </p>
     * 
     * @param sender cliente que enviou a mensagem
     * @param msg mensagem recebida. Exemplo de mensagem: "Ol� pessoal"
     */
    private void sendMsgToAll(final ClientSocket sender, final String msg) {
        final Iterator<ClientSocket> iterator = clientSocketList.iterator();
        int count = 0;
        
        /*Percorre a lista usando o iterator enquanto existir um pr�xima elemento (hasNext)
        para processar, ou seja, enquanto n�o percorrer a lista inteira.*/
        while (iterator.hasNext()) {
            //Obt�m o elemento atual da lista para ser processado.
            final ClientSocket client = iterator.next();
            /*Verifica se o elemento atual da lista (cliente) n�o � o cliente que enviou a mensagem.
            Se n�o for, encaminha a mensagem pra tal cliente.*/
            if (!client.equals(sender)) {
                if(client.sendMsg(msg))
                    count++;
                else iterator.remove();
            }
        }
        System.out.println("Mensagem encaminhada para " + count + " clientes");
    }

    /**
     * Fecha o socket do servidor quando a aplica��o estiver sendo finalizada.
     */
    private void stop()  {
        try {
            System.out.println("Finalizando servidor");
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar socket do servidor: " + e.getMessage());
        }
    }
}

