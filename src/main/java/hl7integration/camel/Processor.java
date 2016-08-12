package hl7integration.camel;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.GenericMessage;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.*;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
//import com.leidos.def.core.hl7.cda.CDAGenerator;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.stereotype.Component;
import sun.net.www.content.text.Generic;

import javax.jms.*;
import java.io.*;
import java.util.Date;
import java.util.Properties;

//import ca.uhn.hl7v2.model.v22.datatype.PN;

@Component
public class Processor {

    public void process(Message in) throws Exception {

        // convert incoming message to a string
        String msg = in.toString();

        // convert
//        convertHL7ToXML(in);

        // immediately store message in cassandra

        // uncomment it
        //recordMessage(msg, "RAW");

        // check if BSI rev 9 message is an ER message
//        boolean isER = isERMessage(msg);

//        if (isER) {
        // push message to Active MQ

//        System.out.println(convertHL7ToXML(in));

//        publishToQueue(msg);
        System.out.println("---MESSAGE TO CONSOLE---");
        System.out.println(msg);
//        }
    }

    public void recordMessage(String msg, String status) throws Exception {
        java.util.Date date = new java.util.Date();
        String mongoIp = (System.getenv("MONGODB_SERVICE_HOST")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_SERVICE_HOST");

        // create mongo client
        MongoClient mongo = new MongoClient( mongoIp , 27017 );

//        if (System.getenv("MONGODB_AUTH").equals("true")) {
//            String username = (System.getenv("MONGODB_USERNAME")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_USERNAME");
//            String password = (System.getenv("MONGODB_PASSWORD")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_PASSWORD");
//            String database = (System.getenv("MONGODB_DATABASE")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_DATABASE");
//            com.mongodb.MongoCredential credentials = MongoCredential.createCredential(username, database, password);
//            mongo = new MongoClient( mongoIp , credentials );
//        }

        // get mongo db
        DB db = mongo.getDB("ldef_hl7_demo");

        // get mongo collection (aka table)
        DBCollection table = db.getCollection("message");

        // create document(data)
        BasicDBObject document = new BasicDBObject();

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String assign_auth = String.valueOf(terser.get("/MSH-4-2"));
        String eoc_acc = String.valueOf(terser.get("/PID-3-1"));
        String episode = assign_auth + eoc_acc;

        String dt = "dateof( now() )";
        String type = String.valueOf(terser.get("/MSH-9-1"));

        String query = ("INSERT INTO message (id, episode, message, status, statusdate, type) VALUES (now(),'" + episode + "','" + msg + "','" + status + "'," + dt + ",'" + type + "')");

        // add data to a mongo document
        document.put("episode", episode);
        document.put("message", msg);
        document.put("status", status);
        document.put("statusdate", date);
        document.put("type", type);
        table.insert(document);
    }

    public void publishToQueue(String msg) throws IOException {

        String activemqHost = (System.getenv("ACTIVEMQ_SERVICE_HOST")== null) ? getPropValues("activemq-host") : System.getenv("ACTIVEMQ_SERVICE_HOST");
        String activemqPort = (System.getenv("ACTIVEMQ_SERVICE_PORT")== null) ? getPropValues("activemq-port") : System.getenv("ACTIVEMQ_SERVICE_PORT");

        String activemqUri = "tcp://" + activemqHost + ":" + activemqPort;

        // create sender
        ActiveMQProducer producer = new ActiveMQProducer(activemqUri, "admin", "admin");
        try {
            // attempt to add the message to an ActiveMQ Queue
            printMultilineMessageToScreen(msg);
            producer.sendMessage("demo.queue", msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String convertHL7ToXML(Message in) throws Exception {
        HapiContext context = new DefaultHapiContext();
//        Parser hapiParser = context.getGenericParser();
        String xml = "";
//        Message hapiMsg;
        XMLParser xmlParser = new DefaultXMLParser();
//        context.setModelClassFactory(new GenericModelClassFactory());
//        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(String.valueOf(in));

//        hapiMsg = hapiParser.parse(String.valueOf(in));

//        Terser terser = new Terser(hapiMsg);

//        Terser terser = new Terser(genMessage);
//        String asAuth = String.valueOf(terser.get("/MSH-4"));
//        System.out.println("Assigning Authority: " + asAuth);
//        System.out.println("Assigning Authority: " + String.valueOf(terser.get("/MSH-1")));
//        System.out.println("Assigning Authority: " + String.valueOf(terser.get("/MSH-2")));
//        System.out.println("Assigning Authority: " + String.valueOf(terser.get("/MSH-3")));
//        System.out.println("Assigning Authority: " + String.valueOf(terser.get("/MSH-4-2")));


        try {
            xml = xmlParser.encode(in);
        } catch (EncodingNotSupportedException e) {
            e.printStackTrace();
        } catch (HL7Exception e) {
            e.printStackTrace();
        }

        return xml;
    }

    public String convertXMLtoCDA(String xml) {
        String cda = "";

        //TODO: implement XML to CDA parser

        return cda;
    }

    public void printMultilineMessageToScreen(String msg) throws IOException {
        File file = new File(msg);
        BufferedReader reader = new BufferedReader(new StringReader(msg));

        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    public String getPropValues(String property) throws IOException {
        String result = "";
        InputStream inputStream = null;

        try {
            Properties prop = new Properties();
            String propFileName = "endpoint.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            // get the property value and print it out
            result = prop.getProperty(property);

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
        return result;
    }


    public void processMessage(String msg) throws Exception {
        recordMessage(msg, "RAW");
        System.out.println("--- processing raw msg ---");
        printMultilineMessageToScreen(msg);
        System.out.println("--- raw message recorded ---");

        if (!isA03Message(msg)) {
            printMultilineMessageToScreen(msg);
        }


        // validate that the HL7 message is a proper 2.5.1 message
        Boolean isValidHL7 = validateHL7(msg);

        if (isValidHL7) {
            recordMessage("HL7", msg, "VALID");

            Boolean isA03 = isA03Message(msg);
            if (isA03) {

                // check if this message is an ED (Emergency) message
                Boolean isED = isERMessage(msg);
                if (isED) {

                    //TODO: convert to XML
                    recordMessage("XML", msg, "XML");
                    //publish xml to queue
                    System.out.println("----publishing to queue----");
                    publishToQueue(msg);
                    System.out.println("----published to queue-----");

                    //TODO: validate cda portion???
                } else {
                    recordMessage("HL7", msg, "HL7-NOT-ED");
                }


            } else {
                recordMessage("HL7", msg, "HL7-NOT-A03");
            }

        } else {
            recordMessage("HL7", msg, "HL7-NOT-VALID");
        }
    }

    public String convertHL7ToXML(String in) throws Exception {
        String xml = "";
        HapiContext context = new DefaultHapiContext();
        Parser pipeParser = context.getPipeParser();
        XMLParser xmlParser = new DefaultXMLParser();

        ca.uhn.hl7v2.model.Message message = pipeParser.parse(in);

        try {
            xml = xmlParser.encode(message);
        } catch (EncodingNotSupportedException e) {
            e.printStackTrace();
        } catch (HL7Exception e) {
            e.printStackTrace();
        }

        return xml;
    }

    public void recordMessage(String msgType, String msg, String status) throws Exception {

        java.util.Date date = new java.util.Date();
        String mongoIp = (System.getenv("MONGODB_SERVICE_HOST")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_SERVICE_HOST");
//
        // create mongo client
        MongoClient mongo = new MongoClient( mongoIp , 27017 );

//        if (System.getenv("MONGODB_AUTH").equals("true")) {
//            String username = (System.getenv("MONGODB_USERNAME")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_USERNAME");
//            String password = (System.getenv("MONGODB_PASSWORD")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_PASSWORD");
//            String database = (System.getenv("MONGODB_DATABASE")== null) ? getPropValues("mongo-ip") : System.getenv("MONGODB_DATABASE");
//            com.mongodb.MongoCredential credentials = MongoCredential.createCredential(username, database, password);
//            mongo = new MongoClient( mongoIp , credentials );
//        }

        // get mongo db
        DB db = mongo.getDB("ldef_hl7_demo");

        // get mongo collection (aka table)
        DBCollection table = db.getCollection("message");

        // create document(data)
        BasicDBObject document = new BasicDBObject();

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String assign_auth = String.valueOf(terser.get("/MSH-4-2"));
        String eoc_acc = String.valueOf(terser.get("/PID-3-1"));
        String episode = assign_auth + eoc_acc;

        String dt = "dateof( now() )";
        String type = String.valueOf(terser.get("/MSH-9-1"));

        if (msgType == "XML") {
            String xml = convertHL7ToXML(msg);
            msg = xml;
        }

        String query = ("INSERT INTO message (id, episode, message, status, statusdate, type) VALUES (now(),'" + episode + "','" + msg + "','" + status + "'," + dt + ",'" + type + "')");


        // add data to a mongo document
        document.put("episode", episode);
        document.put("message", msg);
        document.put("status", status);
        document.put("statusdate", date);
        document.put("type", type);
        table.insert(document);
    }

    public boolean isERMessage(String msg) throws HL7Exception {
        boolean response = false;

        //TODO: if the message is not an ER message, return false, else return true

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String hl7Type = String.valueOf(terser.get("/MSH-9-1"));

        if (hl7Type == "ADT") {
            String type = String.valueOf(terser.get("/PV1-2-1"));
            if (type == "E") {
                response = true;
            } else {
                response = false;
            }
        } else {
            // return true for ORU (HACK!!!)
            response = true;
        }

        return response;
    }

    public boolean isA03Message(String msg) throws HL7Exception {
        boolean response = false;

        //TODO: if the message is not an ER message, return false, else return true

        // create hapi context
        HapiContext context = new DefaultHapiContext();

        if (msg.contains("ADT")) {
//            System.out.println(msg.indexOf("A03"));

        }

        // set up
        context.setModelClassFactory(new GenericModelClassFactory());
        context.setValidationContext(ValidationContextFactory.noValidation());
        GenericMessage genMessage = (GenericMessage) context.getPipeParser().parse(msg);
        Terser terser = new Terser(genMessage);

        // parse message for content
        String hl7Type = String.valueOf(terser.get("/MSH-9-1"));
//        System.out.println("HL7 TYPE: " + hl7Type);

        if (msg.contains("ADT")) {
            if (msg.contains("A03")) {
                response = true;
            }
        } else {
            // return true for ORU (HACK!!!)
            response = true;
        }

        return response;
    }

    public boolean validateHL7(String msg) {
        boolean response = false;

        //TODO: use hapi to validate the hl7 message
        HapiContext context = new DefaultHapiContext();
        context.setValidationContext(ValidationContextFactory.defaultValidation());
        PipeParser pipeParser = context.getPipeParser();

        try {
            pipeParser.parse(msg);
            response = true;
        } catch (HL7Exception e) {
            response = false;
            System.out.println("Invalid Message: " + e.getMessage());
        }

        return response;
    }
}

