package com.springml.device.service.api;

import com.google.cloud.WaitForOption;
import com.google.cloud.bigquery.*;
import com.springml.device.service.model.Device;
import com.springml.device.service.model.OilRig;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Created by kaarthikraaj on 15/6/17.
 */
public class OilRigsDevicesManager {
    private static Logger logger =
            Logger.getLogger(OilRigsDevicesManager.class.getName());
    private static long count = 0;
    private static Float[] firstDeviceRulList = {200f,90f,60f};
    private static Float[] secondDeviceRulList = {200f,110f,95f,60f};
    private static Float[] thirdDeviceRulList = {200f,140f,110f,90f,60f};
    private static Float[] fourthDeviceRulList = {200f,90f,60f};
    private static BigQuery bigQueryClient ;
    private static ArrayList<OilRig> oilRigs = new ArrayList<OilRig>();
    static{
        bigQueryClient = createAuthorizedClient();
    }

   /* private static HashMap<String,ArrayList<Device>> getOilRigsToDeviceMap() {
        long index = count;
         HashMap<String,ArrayList<Device>> oilRigToDeviceMap= new HashMap<String,ArrayList<Device>>();

        int oilRigCount = 1;
        for(OilRig oilRig:oilRigs) {

            ArrayList<Device> devices = new ArrayList<Device>();
            if(oilRigCount%2==1) {
                Device deviceone = new Device();
                deviceone.setName("Device1");
               // deviceone.setLatitude(oilRig.getLatitude()-.00001f);
               // deviceone.setLongtitude(oilRig.getLongtitude()-.00001f);
                deviceone.setRulVal(firstDeviceRulList[(int) index % 3]);
                devices.add(deviceone);
                Device devicetwo = new Device();
                devicetwo.setName("Device3");
               // devicetwo.setLatitude(oilRig.getLatitude()-.00003f);
               // devicetwo.setLongtitude(oilRig.getLongtitude()-.00003f);
                devicetwo.setRulVal(thirdDeviceRulList[(int) index % 5]);
                devices.add(devicetwo);
            }
            else {
                Device deviceone = new Device();
                deviceone.setName("Device2");
              //  deviceone.setLatitude(oilRig.getLatitude()-.00002f);
             //   deviceone.setLongtitude(oilRig.getLongtitude()-.00002f);
                deviceone.setRulVal(secondDeviceRulList[(int) index % 4]);
                devices.add(deviceone);
                Device devicetwo = new Device();
                devicetwo.setName("Device4");
              //  devicetwo.setLatitude(oilRig.getLatitude()-.00004f);
              //  devicetwo.setLongtitude(oilRig.getLongtitude()-.00004f);
                devicetwo.setRulVal(fourthDeviceRulList[(int) index % 3]);
                devices.add(devicetwo);

            }
            oilRigToDeviceMap.put(oilRig.getName(),devices);
            oilRigCount++;
        }
        count++;
        return oilRigToDeviceMap;
    }
*/
    /*private static void initOilRigs() {
        for(float i=0;i<10;i++) {
            OilRig oilRig = new OilRig();
            oilRig.setName("OilRig-"+i);
            float fracVal = (i/1000f);
            Float latitiude = new Float(28.736628)+new Float(fracVal);
            System.out.println(fracVal);

            oilRig.setLatitude(latitiude);
            Float longtitude = new Float(-88.365997)+new Float(fracVal);

            oilRig.setLongtitude(longtitude);

            oilRigs.add(oilRig);
        }

    }
    */

    public static int getRandomNumber(int min,int max){

        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public static ArrayList<OilRig> getOilRigs() {
        ArrayList<OilRig> oilRigs = new ArrayList<OilRig>();
        //callbigquery to get list of IndustrialPlants and form arraylist
        String industrialPlantsQuery = generateIndustrialPlantsQuery();
        Job industrialPlantsJob = createJob(industrialPlantsQuery, bigQueryClient);
        waitForJobCompletion(industrialPlantsJob);
        QueryResponse response = bigQueryClient.getQueryResults(industrialPlantsJob.getJobId());
        QueryResult result = processResponseAndGetResult(response);
        Iterator<List<FieldValue>> rowItr = result.iterateAll().iterator();

        while (rowItr.hasNext()) {
            List<FieldValue> columns = rowItr.next();
            OilRig oilRig = new OilRig();

            for (int loopCount = 0; loopCount < columns.size(); loopCount++) {
                if(loopCount == 0) {
                    oilRig.setName(columns.get(loopCount).getStringValue());
                }
                else{
                    if(loopCount == 1) {
                        oilRig.setLatitude(columns.get(loopCount).getDoubleValue());
                    }
                    else{
                        if(loopCount == 2) {
                            oilRig.setLongtitude(columns.get(loopCount).getDoubleValue());
                        }
                    }
                }

            }
            oilRigs.add(oilRig);
        }
        return oilRigs;

    }

    private static void waitForJobCompletion(Job job) {
        try {
            job.waitFor(WaitForOption.checkEvery(1, TimeUnit.SECONDS));

        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }


    private static Job createJob(String query, BigQuery bigQueryClient) {
        QueryJobConfiguration.Builder queryJobBuilder = QueryJobConfiguration.newBuilder(query);
        QueryJobConfiguration jobConfig = queryJobBuilder.build();

        JobInfo.Builder builder = Job.newBuilder(jobConfig);
        JobInfo jobInfo = builder.build();
        Job job = bigQueryClient.create(jobInfo);
        return job;
    }

    private static QueryResult processResponseAndGetResult(QueryResponse response) {
        QueryResult result= null;
        List<BigQueryError> executionErrors = response.getExecutionErrors();
        // look for errors in executionErrors
        if (executionErrors.isEmpty()) {
             result = response.getResult();
            logger.info("Query Execution Successful");
        } else {
            StringBuilder errorMsg = new StringBuilder();
            for (BigQueryError err : executionErrors) {
                errorMsg.append(err.getMessage()).append('\n');
            }
            logger.severe("Error while executing bigquery job" + errorMsg.toString());
        }
        return result;
    }


    private static String generateIndustrialPlantsQuery() {
        String fetchIndustrialPlantsQuery = "SELECT IndustrialPlantName,Latitude,Longtitude FROM [mlpdm-168115:FleetMaintainance.DeviceSensorReadings] where IndustrialPlantName is not null group by  \n" +
                "IndustrialPlantName,Latitude,Longtitude\n";
        return fetchIndustrialPlantsQuery;
    }

    private static String fetchDeviceAndLatestPreditionsQuery(String industrialPlantName) {
        String fetchIndustrialPlantsQuery = "SELECT UnitNumber,RemainingOperationCycles from (SELECT PredictedDate,RemainingOperationCycles,IndustrialPlantName\n" +
                " from [mlpdm-168115:FleetMaintainance.DeviceSensorReadings] where IndustrialPlantName like '"+industrialPlantName+"') FirstTable JOIN \n" +
                "(SELECT Max(PredictedDate) AS latest,UnitNumber\n" +
                "FROM \n" +
                "[mlpdm-168115:FleetMaintainance.DeviceSensorReadings]\n" +
                "WHERE PredictedDate is NOT NULL AND IndustrialPlantName like '"+industrialPlantName+"'\n" +
                "group by UnitNumber) \n" +
                "SecondTable\n" +
                "on SecondTable.latest = FirstTable.PredictedDate ";
        return fetchIndustrialPlantsQuery;
    }


    public static HashMap<String, HashMap<String,Double>> getOilRigDeviceSensorReadings(String industrialPlantId,String deviceId) {
        //query sensor readings for all sensors for the given device from big query table and fill the map
        HashMap<String,HashMap<String,Double>> sensorsHistoryMap = new HashMap<String,HashMap<String,Double>>();
        String sensorReadingsQuery = fetchSensorReadingsForDeviceQuery(industrialPlantId,deviceId);
        Job sensorReadingsJob = createJob(sensorReadingsQuery, bigQueryClient);
        waitForJobCompletion(sensorReadingsJob);
        QueryResponse response = bigQueryClient.getQueryResults(sensorReadingsJob.getJobId());
        QueryResult result = processResponseAndGetResult(response);
        Iterator<List<FieldValue>> rowItr = result.iterateAll().iterator();
        HashMap<String, Double> sensorReadingsOverTimeMapForSensorOne = new HashMap<String, Double>();
        HashMap<String, Double> sensorReadingsOverTimeMapForSensorTwo = new HashMap<String, Double>();
        HashMap<String, Double> sensorReadingsOverTimeMapForSensorThree = new HashMap<String, Double>();
        HashMap<String, Double> sensorReadingsOverTimeMapForSensorFour = new HashMap<String, Double>();


        while (rowItr.hasNext()) {
            List<FieldValue> columns = rowItr.next();
            String time = "";
            for (int loopCount = 0; loopCount < columns.size(); loopCount++) {
                if(loopCount == 0) {
                    time = ""+columns.get(loopCount).getStringValue();
                }
                else{
                    if(loopCount == 1) {
                        sensorReadingsOverTimeMapForSensorOne.put(time,columns.get(loopCount).getDoubleValue());
                    }
                    else{
                        if(loopCount == 2) {
                            sensorReadingsOverTimeMapForSensorTwo.put(time,columns.get(loopCount).getDoubleValue());
                        }
                        else{
                            if(loopCount == 3) {
                                sensorReadingsOverTimeMapForSensorThree.put(time,columns.get(loopCount).getDoubleValue());
                            }
                            else{
                                if(loopCount == 4) {
                                    sensorReadingsOverTimeMapForSensorFour.put(time,columns.get(loopCount).getDoubleValue());
                                }
                            }
                        }
                    }
                }

            }

        }

        sensorsHistoryMap.put("SensorMeasure2",sensorReadingsOverTimeMapForSensorOne);
        sensorsHistoryMap.put("SensorMeasure3",sensorReadingsOverTimeMapForSensorTwo);
        sensorsHistoryMap.put("SensorMeasure4",sensorReadingsOverTimeMapForSensorThree);
        sensorsHistoryMap.put("SensorMeasure7",sensorReadingsOverTimeMapForSensorFour);


        return sensorsHistoryMap;
    }

    private static String fetchSensorReadingsForDeviceQuery(String industrialPlantId, String deviceId) {
        String sensorReadingsForDeviceQuery = "SELECT PredictedDate,SensorMeasure2,SensorMeasure3,SensorMeasure4,SensorMeasure7 FROM [mlpdm-168115:FleetMaintainance.DeviceSensorReadings] where IndustrialPlantName like '"+industrialPlantId+"' and\n" +
                "UnitNumber like '"+deviceId+"'";
        return sensorReadingsForDeviceQuery;
    }

    public static ArrayList<Device> getDevicesLifeCyclePredictions(String industrialPlantId) {
        //query bigquery table for the given oilrigId and get list of devices and its latest predictions
       ArrayList<Device> deviceList= new ArrayList<Device>();
        String deviceAndPredictionsQuery = fetchDeviceAndLatestPreditionsQuery(industrialPlantId);
        Job deviceAndPredictionsJob = createJob(deviceAndPredictionsQuery, bigQueryClient);
        waitForJobCompletion(deviceAndPredictionsJob);
        QueryResponse response = bigQueryClient.getQueryResults(deviceAndPredictionsJob.getJobId());
        QueryResult result = processResponseAndGetResult(response);
        Iterator<List<FieldValue>> rowItr = result.iterateAll().iterator();

        while (rowItr.hasNext()) {
            List<FieldValue> columns = rowItr.next();
            Device device = new Device();

            for (int loopCount = 0; loopCount < columns.size(); loopCount++) {
                if(loopCount == 0) {
                    device.setName(columns.get(loopCount).getStringValue());
                }
                else{
                    if(loopCount == 1) {
                        device.setRulVal(columns.get(loopCount).getDoubleValue());
                    }
                }

            }
            deviceList.add(device);
        }

        return deviceList;
    }
    /*
        Prepares for authorization to build BigQuery Client
    */
    private static BigQuery createAuthorizedClient() {
        BigQuery bigQueryClient = null;

        try {
            bigQueryClient = BigQueryOptions.getDefaultInstance().getService();
        } catch (Exception exception) {
            logger.severe("Exception while preparing for authorization :" + exception.getMessage());
        }

        return bigQueryClient;
    }

}
