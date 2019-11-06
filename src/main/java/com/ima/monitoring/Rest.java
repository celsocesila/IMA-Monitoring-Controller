/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ima.monitoring;

import com.esotericsoftware.yamlbeans.YamlReader;
import static spark.Spark.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;
import static us.monoid.web.Resty.form;
import static us.monoid.web.Resty.content;
import static us.monoid.web.Resty.delete;
import static us.monoid.web.Resty.put;

/**
 *
 * @author celso
 */
public class Rest {
    
    static String slice_id;
    static String update;
    static String dataConsumer_id;
    static String dataSource_id;
    static String probe_id;
    static String tool;
    static String measurements_db_ip;
    static String measurements_db_port;
    static String measurements_db_type;
    static String granularity_secs;
    static String namespace = null;
    static String dc_slice_part_id;
    static Resty resty = new Resty();
    static int dc_dataplane_port = 22990;
    
    Logger LOGGER = LoggerFactory.getLogger(Rest.class);
 
    static public String startDataConsumer(String endPoint, String port, String userName, String args) throws JSONException {
        try {
            //Change the Data Consumer data plane port to garante the isolation between Slices 
            dc_dataplane_port = dc_dataplane_port + 1;
            
            String uri = "http://localhost:6666/dataconsumer/?endpoint=" + endPoint + "&port=" + port +  "&username=" + userName + "&args=" + dc_dataplane_port + args;
            
            JSONObject jsobj = resty.json(uri, form("")).toObject();
            String id = jsobj.getString("ID");

            return id;
        } catch (IOException ioe) {
            throw new JSONException("deployDC FAILED" + " IOException: " + ioe.getMessage());
        }
    }
      
    static public String startDataSource(String endPoint, String port, String userName, int dataplane_port, String args) throws JSONException {
        try {
            String uri = "http://localhost:6666/datasource/?endpoint=" + endPoint + "&port=" + port +  "&username=" + userName + "&args=localhost+" + dataplane_port + args;
            JSONObject jsobj = resty.json(uri, form("")).toObject();
            String id = jsobj.getString("ID");

            return id;
        } catch (IOException ioe) {
            throw new JSONException("deployDS FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    static public String startProbe(String dataSourceId, String tool, String ip, String port, String slicePartId, String sliceId, String granularitySecs, ArrayList <String> metrics) throws JSONException {
        try {
            
            StringBuilder args = new StringBuilder();
           
            args.append(ip)
                .append("+")
                .append(port)
                .append("+")
                .append(tool)                    
                .append("+")                    
                .append(slicePartId)
                .append("+")
                .append(sliceId)
                .append("+")
                .append(granularitySecs)
                .append("+\\[");
     
            for (int i=0; i < metrics.size(); ++i) {
                args.append(metrics.get(i))
                          .append(",");
            }
            
            args.append("\\]");
            System.out.println("Args: " + args); 
            String uri = "http://localhost:6666/datasource/" + dataSourceId + "/probe/?className=mon.lattice.appl.probes." + tool + "&args=" + java.net.URLEncoder.encode(args.toString(), "UTF-8");
            System.out.println("URI: " + uri);
            
            try{
                Thread.sleep(5000);
            }catch(InterruptedException e){
            }
            
            JSONObject jsobj = resty.json(uri, form("")).toObject();
            
            String id = jsobj.getString("createdProbeID");
            System.out.println("Probe ID: "+ id);
             turnOnProbe(id);
            
            return id;
        } catch (IOException ioe) {
            throw new JSONException("deployProbe FAILED" + " IOException: " + ioe.getMessage());
        }
    }
        
    static public String startProbeContainer(String dataSourceId, String tool, String ip, String port, String slicePartId, String sliceId, String granularitySecs, String namespace, ArrayList <String> metrics) throws JSONException {
        try {
            
            StringBuilder args = new StringBuilder();
           
            args.append(ip)
                .append("+")
                .append(port)
                .append("+")
                .append(tool)                    
                .append("+")                    
                .append(slicePartId)
                .append("+")
                .append(sliceId)
                .append("+")
                .append(granularitySecs)
                .append("+\\[");
     
            for (int i=0; i < metrics.size(); ++i) {
                args.append(metrics.get(i))
                          .append(",");
            }
            
            args.append("\\]+")
                .append(namespace);
          
            String uri = "http://localhost:6666/datasource/" + dataSourceId + "/probe/?className=mon.lattice.appl.probes." + tool + "&args=" + java.net.URLEncoder.encode(args.toString(), "UTF-8");
            System.out.println("URI: " + uri);
            JSONObject jsobj =resty.json(uri, form("")).toObject();
            System.out.println("Saida do resty: " + jsobj.toString());
            
            String id = jsobj.getString("createdProbeID");
            
            turnOnProbe(id);
          
            return id;
        } catch (IOException ioe) {
            throw new JSONException("deployProbe FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    static public JSONObject turnOnProbe(String  probeId) throws JSONException {
        try {
            String uri = "http://localhost:6666/probe/" + probeId + "/?status=on";
            JSONObject jsobj = resty.json(uri, put(content(""))).toObject();
              
            return jsobj;
        } catch (IOException ioe) {
            throw new JSONException("turnOnProbe FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    static public JSONObject turnOffProbe(String  probeId) throws JSONException {
        try {
            String uri = "http://localhost:6666/probe/" + probeId + "/?status=off";
            JSONObject jsobj = resty.json(uri, put(content(""))).toObject();
            
            return jsobj;
        } catch (IOException ioe) {
            throw new JSONException("turnOffProbe FAILED" + " IOException: " + ioe.getMessage());
        }
    }    
    
    static public JSONObject setSliceId(String probeId, String  sliceId) throws JSONException {
        try {
            String uri = "http://localhost:6666/probe/" + probeId + "/?sliceid=" + sliceId;
            JSONObject jsobj = resty.json(uri,put(content(""))).toObject();

            return jsobj;
        } catch (IOException ioe) {
            throw new JSONException("setSliceId FAILED" + " IOException: " + ioe.getMessage());
        }
    }    
 
    static public String startInfluxdbReporter(String dataConsumerId, String args) throws JSONException {
        try {
            String uri = "http://localhost:6666/dataconsumer/" + dataConsumerId + "/reporter/?className=mon.lattice.appl.reporters.InfluxDBReporter&args=" + args;
            
            JSONObject jsobj = resty.json(uri, form("")).toObject();
            String id = jsobj.getString("createdReporterID");
          
            return id;
        } catch (IOException ioe) {
            throw new JSONException("startInfluxdbReporter FAILED" + " IOException: " + ioe.getMessage());
        }
    }
       
    static public JSONObject getProbesCatalogue() throws JSONException {
        try {
            String uri = "http://localhost:6666/probe/catalogue/";
            JSONObject jsobj = resty.json(uri).toObject();

            return jsobj;
        } catch (IOException ioe) {
            throw new JSONException("getProbesCatalogue FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    static public JSONObject stopDataSource(String dataSourceId) throws JSONException {
        try {
            
            System.out.println("Entrou no data source!!!");
            
            String uri = "http://localhost:6666/datasource/" + dataSourceId;
            System.out.println("URI data source: " + uri);
            
            JSONObject jsobj = resty.json(uri, delete()).toObject();
            System.out.println("Saiu do comando resty data source!!!");
            return jsobj;
        } catch (IOException ioe) {
            throw new JSONException("stopDS FAILED" + " IOException: " + ioe.getMessage());
        }
    }

    static public JSONObject stopDataConsumer(String dataConsumerId) throws JSONException {
        try {
            
            System.out.println("Entrou no data consumer!!!");
            
            String uri = "http://localhost:6666/dataconsumer/" + dataConsumerId;
            System.out.println("URI data consumer: " + uri);
            
            JSONObject jsobj = resty.json(uri, delete()).toObject();
            System.out.println("Saiu do comando resty data source!!!  " + jsobj.toString());

            return jsobj;
        } catch (IOException ioe) {
            throw new JSONException("stopDC FAILED" + " IOException: " + ioe.getMessage());
        }
    }
        
    static public JSONObject stopProbe(String probeId) throws JSONException {
        try {
            String uri = "http://localhost:6666/probe/" + probeId;
            JSONObject jsobj = resty.json(uri, delete()).toObject();

            return jsobj;
        } catch (IOException ioe) {
            throw new JSONException("stopProbe FAILED" + " IOException: " + ioe.getMessage());
        }
    }
    
    public static void main(String[] args) throws IOException, JSONException {
        
        //Control Table
        DefaultTableModel dtm = new DefaultTableModel();
        JTable controllTable = new JTable();
        dtm.setColumnIdentifiers(new Object[]{"SliceID", "DataConsumer", "SlicePartID", "DataSource", "ProbeID", "DataConsumerPort"});
        controllTable.setModel(dtm);
         
        
        //////////////// START MONITORING ////////////////
        post("/necos/ima/start_monitoring", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                
                System.out.println("Valor da porta do data plane no começo do metodo: " + dc_dataplane_port);
                
                System.out.println(request.body());
                // Receive the YAML
                YamlReader reader = new YamlReader(request.body());
                Object object = reader.read();
                Map mapObject = (Map)object;

                // Convert to JSON
                JSONObject jsobj = new JSONObject(mapObject);
                JSONObject slice = jsobj.getJSONObject("slice");
                
                // Read SliceID from the JSON File
                slice_id = slice.getString("id");
                
                // Check if exist in Controll Table
                for (int i = 0; i < dtm.getRowCount(); i++) {
                    
                    if (dtm.getValueAt(i, 0).equals(slice_id)){
                        response.status(500);  //Internal Server Error
                        String message;
                        JSONObject json = new JSONObject();
                        json.put("Error", "SliceID already exist! Try Update Monitoring");
                        message = json.toString();
                        return message;
                    }
                }
                
                dataConsumer_id = startDataConsumer("localhost","22",System.getProperty("user.name"),"+localhost+6699+5555");
                
                String reporter_id = startInfluxdbReporter(dataConsumer_id, "localhost+8086+E2E_SLICE");
                
                // Get how many Slice Parts have
                JSONArray  slicePartsList = slice.getJSONArray("slice-parts");
                JSONObject slicePart;
                JSONObject slicePartId;
                JSONObject monitoringParameters;
       
                for (int i = 0; i < slicePartsList.length(); i++){
                    
                    // Get the parameters
                    slicePart = slicePartsList.getJSONObject(i).getJSONObject("dc-slice-part");
                    
                    slicePartId = slicePart.getJSONObject("dc-slice-part-id");
                    String controllerId = slicePartId.getString("slice-controller-id");
                    String slicePartUuid = slicePartId.getString("slice-part-uuid");
                    dc_slice_part_id = controllerId + "-" + slicePartUuid;
                    
                    monitoringParameters = slicePart.getJSONObject("monitoring-parameters");
                    
                    //Tranform the name of the tool in the Lattice class name 
                    tool = monitoringParameters.getString("tool");
                    String className = tool+"."+tool.substring(0,1).toUpperCase().concat(tool.substring(1))+"Probe"; 
  
                    measurements_db_ip = monitoringParameters.getString("measurements-db-ip");
                    measurements_db_port = monitoringParameters.getString("measurements-db-port");
                    measurements_db_type = monitoringParameters.getString("type");
                    granularity_secs = monitoringParameters.getString("granularity-secs");
                    //namespace = monitoringParameters.getString("namespace");
                    
                    // Get the list of metrics
                    JSONArray  jsMetrics = monitoringParameters.getJSONArray("metrics");
                    JSONObject  metric;
                    ArrayList<String> metrics = new ArrayList<>();
                    
                    for (int j = 0; j < jsMetrics.length(); ++j) {
                        metric = jsMetrics.getJSONObject(j).getJSONObject("metric");
                        metrics.add(j,metric.getString("name"));
                    }

                    dataSource_id = startDataSource("localhost","22",System.getProperty("user.name"),dc_dataplane_port,"+localhost+6699+5555");
                    
//                    System.out.println("Vai entrar o IF do container");
//                    // Select if probe should get metrics just for the Containers, just for the Host or both.
//                    if (("container".equals(measurements_db_type) || "all".equals(measurements_db_type)) && namespace != null){
//                         System.out.println("nao deve entarr aqui 1");
//                        probe_id = startProbeContainer(dataSource_id, className, measurements_db_ip, measurements_db_port, dc_slice_part_id, slice_id, granularity_secs, namespace, metrics);
//                        Object[] data1 ={slice_id,dataConsumer_id, dc_slice_part_id, dataSource_id, probe_id};
//                        dtm.addRow(data1);
//                        
//                    }else if (("container".equals(measurements_db_type) || "all".equals(measurements_db_type)) && namespace == null){
//                        System.out.println("nao deve entarr aqui 2");
//                        probe_id = startProbeContainer(dataSource_id, className, measurements_db_ip, measurements_db_port, dc_slice_part_id, slice_id, granularity_secs, "monitoring", metrics);
//                        Object[] data1 ={slice_id,dataConsumer_id, dc_slice_part_id, dataSource_id, probe_id};
//                        dtm.addRow(data1);
//                    }    
//                    System.out.println("Saiu do IF do container");
//                    
//                    
//                    System.out.println("Vai entrar o IF do host");
//                    if ("host".equals(measurements_db_type) || "all".equals(measurements_db_type)){
                        System.out.println("vai fazer a chamada para criar o probe");
                        probe_id = startProbe(dataSource_id, className, measurements_db_ip, measurements_db_port, dc_slice_part_id, slice_id, granularity_secs, metrics);
                        Object[] data2 ={slice_id,dataConsumer_id, dc_slice_part_id, dataSource_id, probe_id, dc_dataplane_port};
                        dtm.addRow(data2);
//                    }
                    
                }
                
                for (int i = 0; i < dtm.getColumnCount(); i++) { 
            
                    for (int j = 0; j < dtm.getRowCount(); j++){
                    
                        System.out.println(dtm.getColumnName(i));
                        System.out.println("Linha " + j + " :" +dtm.getValueAt(j, i));
                    
                    }
                }
                
                System.out.println("Valor da porta do data plane no final do metodo: " + dc_dataplane_port);
                
                response.status(201); // 201 Created
                String message;
                JSONObject json = new JSONObject();
                json.put("Created", "Monitoring Successfully Created");
                message = json.toString();
                return message;
                
            }
        });

        //////////////// UPDATE MONITORING ////////////////
        post("/necos/ima/update_monitoring", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                
                System.out.println("Valor da porta do data plane no começo do metodo: " + dc_dataplane_port);
                
                System.out.println(request.body());
                // Receive the YAML
                YamlReader reader = new YamlReader(request.body());
                Object object = reader.read();
                Map mapObject = (Map)object;

                // Convert to JSON
                JSONObject jsobj = new JSONObject(mapObject);
                JSONObject slice = jsobj.getJSONObject("slice");
                
                // Read SliceID from the JSON File
                slice_id = slice.getString("id");
                
                // Get how many Slice Parts have
                JSONArray  slicePartsList = slice.getJSONArray("slice-parts");
                JSONObject slicePart;
                JSONObject slicePartId;
                JSONObject monitoringParameters;
                
                for (int i = 0; i < slicePartsList.length(); i++){
                    
                    // Get the parameters
                    slicePart = slicePartsList.getJSONObject(i).getJSONObject("dc-slice-part");
                    
                    slicePartId = slicePart.getJSONObject("dc-slice-part-id");
                    String controllerId = slicePartId.getString("slice-controller-id");
                    String slicePartUuid = slicePartId.getString("slice-part-uuid");
                    dc_slice_part_id = controllerId + "-" + slicePartUuid;
                    
                    monitoringParameters = slicePart.getJSONObject("monitoring-parameters");
                    
                    //Tranform the name of the tool in the Lattice class name 
                    tool = monitoringParameters.getString("tool");
                    String className = tool+"."+tool.substring(0,1).toUpperCase().concat(tool.substring(1))+"Probe"; 
  
                    measurements_db_ip = monitoringParameters.getString("measurements-db-ip");
                    measurements_db_port = monitoringParameters.getString("measurements-db-port");
                    measurements_db_type = monitoringParameters.getString("type");
                    granularity_secs = monitoringParameters.getString("granularity-secs");
                    //namespace = monitoringParameters.getString("namespace");
                    
                    // Get the list of metrics
                    JSONArray  jsMetrics = monitoringParameters.getJSONArray("metrics");
                    JSONObject  metric;
                    ArrayList<String> metrics = new ArrayList<>();
                    
                    for (int j = 0; j < jsMetrics.length(); ++j) {
                        metric = jsMetrics.getJSONObject(j).getJSONObject("metric");
                        metrics.add(j,metric.getString("name"));
                    }
                    
                    int checkSliceIdExist = 0;
                    
                        for (int j = 0; j < dtm.getRowCount(); j++) {
                                                
                            if (slice_id == null ? dtm.getValueAt(j,0).toString() == null : slice_id.equals(dtm.getValueAt(j,0).toString())){
                               
                                checkSliceIdExist = checkSliceIdExist +1;
                                
                                dataConsumer_id = dtm.getValueAt(j,1).toString();                                  
                                System.out.println(dataConsumer_id);
                                
                                dc_dataplane_port = Integer.parseInt(dtm.getValueAt(j,5).toString());                                  
                                System.out.println(dc_dataplane_port);
                                
                                dataSource_id = startDataSource("localhost","22",System.getProperty("user.name"),dc_dataplane_port,"+localhost+6699+5555");
                                
                                probe_id = startProbe(dataSource_id, className, measurements_db_ip, measurements_db_port, dc_slice_part_id, slice_id, granularity_secs, metrics);
                                
                                Object[] data2 ={slice_id,dataConsumer_id, dc_slice_part_id, dataSource_id, probe_id, dc_dataplane_port};
                                
                                dtm.addRow(data2);
                                
                                j = dtm.getRowCount();
                                
                            }
                    
                        }
                        
                        if (checkSliceIdExist == 0){
                            System.out.println("Entrou no IF que compara de check é 0: " + checkSliceIdExist );
                            response.status(500);  //Internal Server Error
                            String message;
                            JSONObject json = new JSONObject();
                            json.put("Error", "SliceID not exist! Try start Monitoring");
                            message = json.toString();
                            return message;
                        }
                    
                }
                
                for (int i = 0; i < dtm.getColumnCount(); i++) { 
            
                    for (int j = 0; j < dtm.getRowCount(); j++){
                    
                        System.out.println(dtm.getColumnName(i));
                        System.out.println("Linha " + j + " :" +dtm.getValueAt(j, i));
                    
                    }
                }
                
                System.out.println("Valor da porta do data plane no final do metodo: " + dc_dataplane_port);
                
                response.status(200); 
                String message;
                JSONObject json = new JSONObject();
                json.put("Updated", "Monitoring Successfully Updated");
                message = json.toString();
                return message;
                
            }
        });        
        
        /////////////// START SERVICE CONTAINER MONITORING ////////////////
        post("/necos/ima/start_container_monitoring", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                System.out.println(request.body());
                // Receive the YAML
                YamlReader reader = new YamlReader(request.body());
                Object object = reader.read();
                
                Map mapObject = (Map)object;

                // Convert to JSON
                JSONObject jsobj = new JSONObject(mapObject);
                JSONObject slice = jsobj.getJSONObject("slice");
                
                // Read SliceID from the JSON File
                slice_id = slice.getString("id");
                System.out.println("Slice ID: " + slice_id);
                
                // Get how many Slice Parts have
                JSONArray  slicePartsList = slice.getJSONArray("slice-parts");
                JSONObject slicePart;
                JSONObject slicePartId;
                JSONObject monitoringParameters;
                
                for (int i = 0; i < slicePartsList.length(); i++){
                    
                    // Get the parameters
                    slicePart = slicePartsList.getJSONObject(i).getJSONObject("dc-slice-part");
                    
                    slicePartId = slicePart.getJSONObject("dc-slice-part-id");
                    String controllerId = slicePartId.getString("slice-controller-id");
                    String slicePartUuid = slicePartId.getString("slice-part-uuid");
                    dc_slice_part_id = controllerId + "-" + slicePartUuid;
                    System.out.println("SlicePart ID: " + dc_slice_part_id);
                    
                    monitoringParameters = slicePart.getJSONObject("monitoring-parameters");
                    tool = monitoringParameters.getString("tool");
                    
                    //Check if its a netdata tool don't start the container monitoring
                    if (!"netdata".equals(tool)){
                        
                        int aux = 0;
                        
                        // Get the Data Source ID and Data Consumer ID from the Control Table
                        System.out.println("Check data source e data consumer ID");
                        
                        for (int n = 0; n < dtm.getRowCount(); n++){
                            
                            System.out.println("Linha: " + n);
                            
                            if(dtm.getValueAt(n,0).toString().equals(slice_id)){
                                
                                if (dtm.getValueAt(n,2).toString().equals(dc_slice_part_id)){
                                    dataConsumer_id = dtm.getValueAt(n,1).toString();
                                    dataSource_id = dtm.getValueAt(n,3).toString();
                                    dc_dataplane_port = Integer.parseInt(dtm.getValueAt(n,5).toString());

                                    aux++;
                                }
                            }    
                        }
                        
                        if (aux == 0){
                            dataConsumer_id = startDataConsumer("localhost","22",System.getProperty("user.name"),"+localhost+6699+5555");
                            String reporter_id = startInfluxdbReporter(dataConsumer_id, "localhost+8086+E2E_SLICE");
                            dataSource_id = startDataSource("localhost","22",System.getProperty("user.name"),dc_dataplane_port,"+localhost+6699+5555");
                        }
                        
                        // Tranform the name of the tool in the Lattice class name 
                        String className = tool+"."+tool.substring(0,1).toUpperCase().concat(tool.substring(1))+"Probe"; 
                    
                        measurements_db_ip = monitoringParameters.getString("measurements-db-ip");
                        measurements_db_port = monitoringParameters.getString("measurements-db-port");
                        measurements_db_type = monitoringParameters.getString("type");
                        granularity_secs = monitoringParameters.getString("granularity-secs");
                        namespace = monitoringParameters.getString("namespace");

                        System.out.println("pegando as metricas");

                        // Get the list of metrics
                        JSONArray  jsMetrics = monitoringParameters.getJSONArray("metrics");
                        JSONObject  metric;
                        ArrayList<String> metrics = new ArrayList<>();

                        for (int j = 0; j < jsMetrics.length(); ++j) {
                            metric = jsMetrics.getJSONObject(j).getJSONObject("metric");
                            metrics.add(j,metric.getString("name"));
                        }

                        System.out.println("pegou as metricas e vai inciar o probe");
                        // Start Probe
                        probe_id = startProbeContainer(dataSource_id, className, measurements_db_ip, measurements_db_port, dc_slice_part_id, slice_id, granularity_secs, namespace, metrics);

                        Object[] data1 ={slice_id,dataConsumer_id, dc_slice_part_id, dataSource_id, probe_id, dc_dataplane_port};
                        dtm.addRow(data1);

                    }
                        
                }
                
                
                for (int i = 0; i < dtm.getColumnCount(); i++) { 
            
                    for (int j = 0; j < dtm.getRowCount(); j++){
                    
                        System.out.println(dtm.getColumnName(i));
                        System.out.println("Linha " + j + " :" +dtm.getValueAt(j, i));
                    
                    }
                }
                
                response.status(201); // 201 Created
                String message;
                JSONObject json = new JSONObject();
                json.put("Created", "Monitoring Successfully Created");
                message = json.toString();
                return message;     
            }
        });

        //////////////// DELETE MONITORING ////////////////
        post("/necos/ima/delete_monitoring", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                System.out.println(request.body());
                // Receive the YAML
                YamlReader reader = new YamlReader(request.body());
                Object object = reader.read();
                
                System.out.println(object);
                Map mapObject = (Map)object;

                // Convert to JSON
                JSONObject jsobj = new JSONObject(mapObject);
                JSONObject slice = jsobj.getJSONObject("slice");
                
                // Read SliceID from the JSON File
                slice_id = slice.getString("id");
                
                // Get how many Slice Parts have
                JSONArray  slicePartsList = slice.getJSONArray("slice-parts");
                JSONObject slicePart;
                JSONObject slicePartId;
             
                System.out.println(slicePartsList.length());
                ArrayList<Integer> del_list = new ArrayList();
                
                for (int i = 0; i < slicePartsList.length(); i++){
                    
                    System.out.println("numero de I: " + i);
                    
                    // Get the parameters               
                    slicePart = slicePartsList.getJSONObject(i).getJSONObject("dc-slice-part");
                    
                    slicePartId = slicePart.getJSONObject("dc-slice-part-id");
                    String controllerId = slicePartId.getString("slice-controller-id");
                    String slicePartUuid = slicePartId.getString("slice-part-uuid");
                    dc_slice_part_id = controllerId + "-" + slicePartUuid;
                    
                    int checkSliceIdExist = 0;
                    int countTable = 0;
                    
                        for (int j = 0; j < dtm.getRowCount(); j++) {
                            
                            countTable = countTable + 1;
                                                
                            if (slice_id == null ? dtm.getValueAt(j,0).toString() == null : slice_id.equals(dtm.getValueAt(j,0).toString())){
                               
                                checkSliceIdExist = checkSliceIdExist +1;
                                
                                //Get the Data Consumer ID to be deleted in the end of SlicePart loop
                                dataConsumer_id = dtm.getValueAt(j,1).toString();                                  
                                System.out.println(dataConsumer_id);
                                
                                if (dc_slice_part_id == null ? dtm.getValueAt(j, 2).toString() == null : dc_slice_part_id.equals(dtm.getValueAt(j, 2).toString())){
                                
                                    probe_id = dtm.getValueAt(j,4).toString();
                                    System.out.println(probe_id);
                                    System.out.println(stopProbe (probe_id).toString());
                                    del_list.add(j);
 //                                   dtm.removeRow(j);
                                }
                                
                                if (countTable == dtm.getRowCount()){
                                    
                                    System.out.println("Entrou no IF do Data Source");
                                
                                    dataSource_id = dtm.getValueAt(j,3).toString();
                                    System.out.println(dataSource_id);
                                    System.out.println(stopDataSource (dataSource_id).toString());
                            
                                }
                                
                            }
                            
                            System.out.println("Não entrou no IF  e o Valor de check slideID é: " + checkSliceIdExist );
                     
                        }  
                        
                        if (checkSliceIdExist == 0){
                            System.out.println("Entrou no IF que compara de check é 0: " + checkSliceIdExist );
                            response.status(500);  //Internal Server Error
                            String message;
                            JSONObject json = new JSONObject();
                            json.put("Error", "SliceID not exist! Impossible Delete Monitoring");
                            message = json.toString();
                            return message;
                        }
            
                }
                
                System.out.println(stopDataConsumer (dataConsumer_id).toString());
                
                for (int k = 0; k < del_list.size(); k++){
                    dtm.removeRow(k);
                }
                
                if (dtm.getRowCount() != 0){
                    
                    for (int i = 0; i < dtm.getColumnCount(); i++) { 
                        
                        for (int j = 0; j < dtm.getRowCount(); j++){
                            
                            System.out.println(dtm.getColumnName(i));
                            System.out.println("Linha " + j + " :" +dtm.getValueAt(j, i));
                        }
                    }
                } else {
                    
                    System.out.println("Table is empty!!!");
                } 
                    
                response.status(200); // 201 Created
                String message;
                JSONObject json = new JSONObject();
                json.put("Deleted", "Monitoring Successfully Deleted");
                message = json.toString();
                return message;
                
            }
        });                
    }    
}
