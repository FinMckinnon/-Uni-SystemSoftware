public class serverMain
{
    public static void main(String[] args)
    {
        int port = 4445;
        Server server = new Server(port);
        server.start();
    }
}
