import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class StationSensors {

    private final StationClient stationClient; // Take in station[]
    public Random random = new Random();
    public Thread sensor;
    private double temperature;
    private double pressure;
    private double humidity;
    private double windSpeed;

    public StationSensors(StationClient stationClient){
        this.stationClient = stationClient;
    }

    public void startMessageReader(StationClient station) {
        this.sensor = new Thread(){
            @Override
            public void run()
            {
                try {
                    initialiseSensorValues();
                    startSession();
                    recordResults(station);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        sensor.start();
    }

    private void recordResults(StationClient station) throws InterruptedException, IOException {
        while(station.active) {

            generateResults();

            //Date to write put into readable format
            String fileName = stationClient.stationName + "_SensorData.txt";
            String data = "{ Temperature: "+round(temperature)+"C, " +
                    "Pressure: "+round(pressure)+"hPa, " +
                    "Humidity "+ round(humidity) +"% , " +
                    "Wind Speed: "+round(windSpeed)+"mps }\n";

            writeDataToFile(fileName, data);

            // Wait one minute before pulling more data
            Thread.sleep(1000 * 60);
        }
    }

    // Write given data to the database file with a date/time stamp
    private void writeDataToFile(String fileName, String data ) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String dateInfo = dateFormat.format(date);

        writer.append(dateInfo + ": " + data + System.getProperty("line.separator"));
        writer.close();
    }

    // Create a starting session message to separate sessions
    private void startSession() throws IOException {
        String fileName = stationClient.stationName + "_SensorData.txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));

        String linebreak = StringUtils.repeat("= ", 20);
        String msg =  "S E S S I O N    S T A R T  ";

        writer.append(linebreak + msg + linebreak + System.getProperty("line.separator"));
        writer.close();
    }

    // Update current data with random  values
    private void generateResults(){
        this.temperature += generateValue();
        this.humidity += generateValue();
        this.pressure += generateValue();
        this.windSpeed += generateValue();
    }

    // Set realistic base values for each value
    private void initialiseSensorValues(){
        this.temperature = random.nextInt(20) + 5;
        this.humidity = random.nextInt(10) + 68;
        this.pressure = random.nextInt(10) + 1017.5;
        this.windSpeed = random.nextInt(5) + 7;
    }

    // Generate a random value with a random sign
    private double generateValue(){
        return (Math.random() * randomSign());
    }

    // Returns a random sign value
    private int randomSign(){
        int choice = random.nextInt(2);
        if(choice == 1){
            return 1;
        }
        return -1;
    }

    //Rounds a value to two decimal places
    public double round(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
