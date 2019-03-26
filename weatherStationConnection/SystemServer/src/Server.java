import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server extends Thread
{
    private final int serverPort;

    public ArrayList<ServerWorker> workerList = new ArrayList<>();
    private Server clientSocket;

    public Server(int serverPort)
    {
        this.serverPort = serverPort;
    }

    public List<ServerWorker> getWorkersList()
    {
        return workerList;
    }

    public void removeFromWorkersList(ServerWorker workerToRemove) // Remove a specified worker from WorkerList
    {
        workerList.remove(workerToRemove);
    }

    @Override
    public void run() {
        try
        {
            ServerSocket serverSocket = new ServerSocket(serverPort); // Creates socket on port
            while(true) // infinite loop to deal with incoming connections
            {
                System.out.println("Waiting for new connection...");
                Socket clientSocket = serverSocket.accept(); // Accepting connections on socket
                System.out.println("Connected to client "+ clientSocket);
                ServerWorker worker = new ServerWorker(this, clientSocket); // Creates a worker which handles communication with socket
                workerList.add(worker);
                worker.start();
            }
        }
        catch (IOException e) { e.printStackTrace(); }
    }
}
