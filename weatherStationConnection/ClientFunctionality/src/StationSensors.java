import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class StationSensors {

    private final StationClient stationClient; // Take in station[]
    private final int[] sensorIDs;
    public Random random = new Random();

    public StationSensors(StationClient stationClient){
        this.stationClient = stationClient;
        this.sensorIDs = getStationSensorIDs();
    }

    public void startMessageReader() {
        Thread t = new Thread(){
            @Override
            public void run()
            {
                try {
                    recordResults();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
       t.start();
    }

    private void recordResults() throws InterruptedException, IOException {
        while(true) {
            //For each sensor
            double[] results = generateResults(50.34, 20.56);
            //Write to file
            String fileName = stationClient.stationName + "_SensorData.txt";
            String data = "{ "+results[0]+", "+results[1]+", "+results[2]+" }\n";

            writeDataToFile(fileName, data);

            Thread.sleep(60000);
        }
    }

    private void writeDataToFile(String fileName, String data ) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String dateInfo = dateFormat.format(date);

        writer.append(dateInfo + ": " + data + System.getProperty("line.separator"));
        writer.close();
    }

    private double[] generateResults(double lat, double lon){
        double[] results = new double[3];

        double value1 = random.nextInt(100);
        double value2 = random.nextInt(100);
        double value3 = random.nextInt(100);

        results[0] = value1;
        results[1] = value2;
        results[2] =value3;

        return results;
    }

    public int[] getStationSensorIDs(){
        return stationClient.getStationSensors();
    }

   // private int[][] getSensorInfo(){ }


}
