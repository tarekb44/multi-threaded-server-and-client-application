import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.Socket;
import java.nio.Buffer;
import java.util.Vector;
import java.util.concurrent.Callable;

/**
 * this Server accepts at least two options to be voted for,
 * server quits and throws an error when this condition is not met
 * for every client connected, it will creat a new
 * clienthandler
 *
 */
public class Server {
	public static void main(String[] args) {
		//stores endless options
		Vector<String> options = new Vector<String>();

		//store the options in a resizable vector
		for (String word : args) {
			options.add(word);
		}

		//put the arguments into one string
		String arguments = String.join(" ", args);

		//create a new class that stores the arguments
		VoteStorage optionsClass = new VoteStorage(arguments);

		//call method within VoteStorage class that stores the options into a map
		optionsClass.storeOptions(arguments);

		//check that it has more than at-least two elements
		if (options.size() < 2) {
			System.err.println("Error! You must input two or more options!");
			System.exit(1);
		}

		//Executor to manage a fixed thread pool of size 20
		ExecutorService service = Executors.newFixedThreadPool(20);

		//ip address of the client
		String ipAddress = null;
		try (ServerSocket server = new ServerSocket(7777)) {
			while (true) {
				try {
					//create a thread for each accepted client
					Socket connection = server.accept();

					//get Inet Address object of client
					InetAddress address = connection.getInetAddress();

					//get ip address of client
					ipAddress = address.getHostAddress();

					//call a thread for each request
					Callable<Void> task = new ClientHandler(connection, optionsClass, ipAddress);

					//submit in executor
					service.submit(task);

				} catch (IOException e) {
					//problem with client not connecting
					System.err.println(e.getMessage());
				}
			}

		} catch (IOException e) {
			System.out.println("Could not listen on PORT: 7777!");
		}
	}
}

/**
 * this class stores the options into a hashmap
 * and keeps track of the number of votes
 * this class is also responsible for incrementing the votes
 * and showing the votes
 *
 */
class VoteStorage {
	String options;

	//constructor
	public VoteStorage(String options) {
		this.options = options;
	}

	//store the array elements as keys in a HashMap
	Map<String, Integer> optionsMap = new HashMap<String, Integer>();

	//stores the options into a hashmap
	public void storeOptions(String options){
		//split the options and put them into an array
		String[] arrayOfOptions = options.split(" ");

		//place elements from the options array into HashMap
		for(String element : arrayOfOptions) {
			//initially, each option has a vote of 0
			optionsMap.put(element, 0);
		}
	}

	//increases the number of votes for a certain option by 1
	public int increaseVote(String key) {
		int val1 = 0;

		//get the current value of the option
		Integer value = optionsMap.get(key);

		//if there is no mapping for the option return -1
		if(value == null){
			val1 = -1;
		} else {
			//increment the value of the key
			optionsMap.put(key, value + 1);
			val1 = optionsMap.get(key);
		}

		return val1;
	}

	//shows the number of votes
	public int showVote(String key) {
		int val2 = 0;

		Integer value = optionsMap.get(key);

		if(value == null){
			val2 = -1;
		} else {
			val2 = optionsMap.get(key);
		}

		return val2;
	}

	//prints the current options and their votes
	public void testPrint() {
		System.out.println(optionsMap);
	}
}

/**
 * class that is called by the server
 * and handles each client
 * checks if the commands are valid and that option exist
 * responsible for logging every request in 'log.txt'
 * closes the socket after response
 *
 */
class ClientHandler implements Callable<Void> {
	Socket socket;
	VoteStorage votes;
	String ipAddress;

	//constructor
	public ClientHandler(Socket socket, VoteStorage votes, String ipAddress) {
		this.socket = socket;
		this.votes = votes;
		this.ipAddress = ipAddress;
	}

	public Void call() {
		//server output
		String output = null;
		//client input
		String userInput = null;

		//temporary array to store the user input
		//temp[0] = ("vote"||"show")
		//temp[1] = <option>
		String[] temp = null;

		//element in the options array becomes the Key in the HashMap
		String Key;
		int numVotes = 0;

		try {
			//read from the client
			BufferedReader in = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			//write to client
			PrintWriter out = new PrintWriter(socket.getOutputStream(),true);

			//while there is input read
			while ((userInput = in.readLine()) != null) {
				//log into log file for every client
				writeToLogFile(ipAddress, userInput);

				//put user components into an array
				temp = userInput.split("[ ]");

				//check for two arguments only
				if(temp.length > 2){
					//throw error to client
					out.println("Error! '" + userInput + "' has " + temp.length + " elements!." +
							" You can only input two arguments: ('show' or 'vote') and <option>");

					//close connection
					socket.close();
				}

				if (temp[0].equals("show")) {
					//store the option in key variable
					Key = temp[1];

					//call and store number of votes
					numVotes = votes.showVote(Key);

					//if the option does not exist, throw an error and close socket
					//else show the number of votes
					if(numVotes == -1){
						out.println("Error! The option '" + Key + "' does not exist!");

						//close socket after error encountered
						socket.close();
					}

					//print to the client
					output = "'" + temp[1] + "'" + " has " + numVotes + " vote(s)";
					out.println(output);

				} else if (temp[0].equals("vote")) {
					//store the option
					Key = temp[1];

					//call and store the number of votes after increment
					numVotes = votes.increaseVote(Key);

					//if the option does not exist, throw an error and close socket
					//else increment the number of votes
					if(numVotes == -1){
						out.println("Error! The option '" + Key + "' does not exist!");

						//close socket after error encountered
						socket.close();
					}

					//print to the client
					output = "'"+temp[1]+"'" + " now has " + numVotes + " vote(s)";
					out.println(output);

				} else if(!(temp[0].equals("vote") || temp[0].equals("show"))) {
					//if client inputs invalid command, throw an error
					out.println("Error! '" + temp[0] + "' is not a valid command. Only 'show' or 'vote' are valid commands");

					//close socket
					socket.close();
				}

				//close the connection
				out.close();
				in.close();
				socket.close();
			}

		} catch (IOException e) {
		}

		return null;
	}

	/**
	 * creates 'log.txt' file
	 * when requested, logs to the file
	 * for every client request
	 *
	 * @param ipAddress
	 * @param command
	 */
	public static void writeToLogFile(String ipAddress, String command) {

		//get current date
		DateTimeFormatter currentDate = DateTimeFormatter.ofPattern("dd/MM/yyyy:HH:mm:ss");

		//get the current time
		LocalDateTime currentTime = LocalDateTime.now();

		try{
			//create 'log.txt' file
			BufferedWriter write = new BufferedWriter( new FileWriter("log.txt",true));

			//for each request write to file in the format: date:time:client Ip address:request
			write.append(currentDate.format(currentTime) + ":"
					+ ipAddress + ":" + command);
			write.append('\n');

			//close file
			write.close();

		} catch (IOException ex){
			//throw error if you cannot open file
			ex.printStackTrace();
		}
	}
}
