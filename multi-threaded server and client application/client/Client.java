import java.io.*;
import java.net.*;
import java.util.*;


/**
 * accepts two commands: 'show' or 'vote' and <option>
 * for show it requests that server shows the
 * number of votes of the option, for vote it requests that
 * the server increments the number of votes of the option by 1
 *
 */
public class Client {
    /**
     * this method opens a socket
     * and connects to the server,
     * it sends the user input to the server
     * and prints server response
     *
     * @param userCommand
     */
    public static void userCommand(String userCommand){
        int TIMEOUT = 10000;

        //store the user output
        String serverResponse = null;

        try{
            //set up a socket to the server
            Socket ClientSocket = new Socket("localhost", 7777);

            //client output
            //chaining a writing stream with a buffer to the server
            PrintWriter socketResponse = new PrintWriter(ClientSocket.getOutputStream());

            //write and flush to the server the user command arguments
            socketResponse.println(userCommand);
            socketResponse.flush();

            //chaining a reading stream with a buffer
            BufferedReader clientInput = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));

            //print the server output to the client
            while((serverResponse = clientInput.readLine())!= null){
                System.out.println(serverResponse);
            }

            //closing the connection
            clientInput.close();
            socketResponse.close();
            ClientSocket.close();

            //quit after server response
            System.exit(1);

        } catch (IOException e) {
            //IOException thrown when socket cannot be open
            System.err.println(e);
        }
    }

    /**
     * get the user input
     * checks that right commands have been entered
     * and that there are only two commands entered
     *
     * @param args
     */
    public static void main(String[] args) {

        //join the arguments into one string
        String input = String.join(" ", args);

        //method for calling the server
        userCommand(input);
    }
}
