import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 *  Server is the class that receives the user's holding data
 *  and manages the entire user interface
 * 
 *  @author     Shawn Chen
 *  @version    01/19/2022
 */

public class Server {
    private final int PORT = 5001;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter output;
    private BufferedReader input;
    private int clientCounter = 0;
    private ArrayList < User > userList;
    private ArrayList < String > loginInfo;
    private ArrayList < Portfolio > portfolioList;
    //----------------------------------------------------------------------------
    public Server(ArrayList < User > userList, ArrayList < String > loginInfo, ArrayList < Portfolio > portfolioList) {
        this.userList = userList;
        this.loginInfo = loginInfo;
        this.portfolioList = portfolioList;
    }
    //----------------------------------------------------------------------------
    public static void main(String[] args) throws Exception {

        //  Taking input for login
        ArrayList < User > inputUsers = new ArrayList < User > ();
        ArrayList < String > inputLogin = new ArrayList < String > ();
        File userDatabase = new File("Server Database/USER_DATABASE.txt");
        Scanner readUsers = new Scanner(userDatabase);
        while (readUsers.hasNext()) {
            String lineAt = readUsers.next();
            inputLogin.add(lineAt);
            String[] loginComponents = lineAt.split("/");
            inputUsers.add(new User(loginComponents[0], loginComponents[1]));
        }
        readUsers.close();
        //  Taking input for portfolio
        ArrayList < Portfolio > inputPortfolio = new ArrayList < Portfolio > ();
        File portfolioDatabase = new File("Server Database/PORTFOLIO_DATABASE.txt");
        Scanner readPortfolios = new Scanner(portfolioDatabase);
        while (readPortfolios.hasNext()) {
            ArrayList < Holding > currentPortfolio = new ArrayList < Holding > ();
            String lineAt = readPortfolios.next();
            if (lineAt.equals("-")) {
                inputPortfolio.add(new Portfolio(currentPortfolio));

            } else {
                String[] portfolioComponents = lineAt.split(":");
                for (int i = 0; i < portfolioComponents.length; i++) {
                    String[] holdingComponents = portfolioComponents[i].split("/");
                    currentPortfolio.add(new Holding(holdingComponents[0], holdingComponents[1], holdingComponents[2], holdingComponents[3]));
                }
                inputPortfolio.add(new Portfolio(currentPortfolio));
            }
        }
        readPortfolios.close();
        Server server = new Server(inputUsers, inputLogin, inputPortfolio);
        server.go();
    }
    //----------------------------------------------------------------------------
    public void go() throws Exception {
        //  Create a socket with the local IP address and wait for connection request       
        System.out.println("Waiting for a connection request from a client ...");
        serverSocket = new ServerSocket(PORT);      //  create and bind a socket
        while (true) {
            clientSocket = serverSocket.accept();   //  wait for connection request
            clientCounter = clientCounter + 1;
            System.out.println("Client " + clientCounter + " connected");
            Thread connectionThread = new Thread(new ConnectionHandler(clientSocket));
            connectionThread.start();               //  start a new thread to handle the connection
        }
    }

    /**
     *  Obtains a list of user's that may be be logged in
     * 
     *  @return list of user's
     */
    public ArrayList < User > getUsers() {
        return this.userList;
    }
    //------------------------------------------------------------------------------
    class ConnectionHandler extends Thread {
        private Socket socket;
        private PrintWriter output;
        private BufferedReader input;
        private String login;
        private boolean isRunning;

        //  Print to file method, returns current state

        public int getUserIndex(String loginStr) {
            for (int i = 0; i < loginInfo.size(); i++) {
                if (loginInfo.get(i).equals(loginStr)) {
                    return i;
                }
            }
            return -1;
        }

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
            isRunning = true;
        }

        /**
         *  Checks if a user is authenticated and if so, allows them to access the application
         * 
         * @param   currentUser to authenticate
         * @return  boolean     if authenticated or not
         */
        public boolean authenticateUser(User currentUser) {
            //  Loops through entire user database to check if login is the same
            for (int userIndex = 0; userIndex < userList.size(); userIndex++) {
                if (userList.get(userIndex).compare(currentUser)) {
                    return true;
                }
            }
            return false;
        }

        /**
         *  Adds user's to the text file of user's logins
         *  When entering a new name, a new login will appear in the text file
         * 
         *  @param  currentUser to add to the login
         */
        public void addUser(User currentUser) {
            userList.add(currentUser);
            loginInfo.add(currentUser.getUsername() + "/" + currentUser.getPassword());
        }

        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream());
                //  LOGIN HANDLER
                login = input.readLine();
                String[] loginComponents = login.split("/");
                User loginUser = new User(loginComponents[0], loginComponents[1]);
                if (authenticateUser(loginUser)) {
                    output.println("Authenticated!");
                    output.flush();
                } else {
                    addUser(loginUser);
                    portfolioList.add(new Portfolio(new ArrayList < Holding > ()));
                    output.println("Authenticated!");
                    output.flush();
                }
                int portfolioIndex = getUserIndex(login);
                Portfolio userPortfolio = portfolioList.get(portfolioIndex);
                output.println(userPortfolio.deconstruct());
                output.flush();

                //  TRADE HANDLER
                while (isRunning) {
                    //  Checks if client is active or not
                    String order = input.readLine();
                    if (order == null) {
                        isRunning = false;
                        break;
                    } else {
                        String[] orderComponents = order.split("/");
                        Order submittedOrder = new Order(orderComponents[0], orderComponents[1], orderComponents[2], orderComponents[3]);
                        userPortfolio.updatePortfolio(submittedOrder);
                        output.println(userPortfolio.deconstruct());
                        output.flush();
                    }
                }
                input.close();
                output.close();
                System.out.println("DISCONNECTED!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //----------------------------------------------------------------------------
    }
}