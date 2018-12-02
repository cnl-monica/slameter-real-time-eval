/*
 * Copyright (C) 2013 MONICA Research Group / TUKE
 *
 * This file is part of SLAmeter.
 * <http://wiki.cnl.sk/Monica/SLAmeter>
 *
 * SLAMeter is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.

 * SLAMeter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with SLAMeter; If not, see <http://www.gnu.org/licenses/>.
 *
 */
package acp;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.AuthenticationException;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.log4j.BasicConfigurator;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import redis.clients.jedis.Jedis;
import sk.tuke.cnl.bm.ACPIPFIXTemplate;
import sk.tuke.cnl.bm.ACPapi.ACP;
import sk.tuke.cnl.bm.ACPapi.ACPException;
import sk.tuke.cnl.bm.ACPapi.IACP;
import sk.tuke.cnl.bm.Filter;
import sk.tuke.cnl.bm.InvalidFilterRuleException;
import sk.tuke.cnl.bm.SimpleFilter;

/**
 *
 * @author Szilard Jager
 */
public class Daemon implements Runnable {

    //Vlákno pracujúce v pozadí
    private Thread thread;
    //Premenná pre nekonečný cyklus vlákna
    private boolean finish = true;
    //Inštancia ACPapi
    private ACP acp = new ACP();
    //Preddefinovaná šablóna                                                                                                                                                              //Informacne elementy pre overenie
    private int[] fieldsToRead = {ACPIPFIXTemplate.octetDeltaCount, ACPIPFIXTemplate.packetDeltaCount, ACPIPFIXTemplate.droppedOctetDeltaCount, ACPIPFIXTemplate.droppedPacketDeltaCount/*,ACPIPFIXTemplate.sourceIPv4Address, ACPIPFIXTemplate.destinationIPv4Address,ACPIPFIXTemplate.sourceTransportPort,ACPIPFIXTemplate.destinationTransportPort,ACPIPFIXTemplate.flowStartMilliseconds,ACPIPFIXTemplate.flowEndMilliseconds*/};
    //Premenné potrebné pre komponenty
    private long octetDeltaCount;               //throughput in B/s
    private long packetDeltaCount;              //throughput in p/s
    private long droppedOctetDeltaCount;       //packetLoss in B/s
    private long droppedPacketDeltaCount;      //packetLoss in p/s
    //Buffer pre dáta získané od kolektora
    private Buffer fifoBuffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(1000));
    //Premenné pre časovač
    private int timeCounter = 0;
    private Timer timer;
    //Premenná na zistenie či ACP beží
    private boolean acpRunning = false;
    //Premenné pre nastavenia filtra
    private String monitorPointIP = ""; //IP adresa meracieho bodu
    private String sourceIP = "";       //zdrojová IP adresa
    private String sourcePort = "";     //cieľová IP adresa
    private String destinationIP = "";  //zdrojový port
    private String destinationPort = "";//cieĺový zdroj
    private String protocol = "";       //protokol

    //pomocou Redis sa posielaju vyhodnotene data na web rozhranie
    private Jedis jedis = new Jedis("localhost");
    private TimeCounter counter;// = new TimeCounter();

    private boolean sendThroughputInB = false;
    private boolean sendThroughputInP = false;
    private boolean sendPacketlossInB = false;
    private boolean sendPacketlossInP = false;
    /*
     /**
     * metóda volaná hneď po nasadení aplikácie na servera nastaví statickú
     * premennú triedy Acp ,aby táto trieda bola dosiahnuteľná súčasne vytvorí
     * inštanciu vlákna, pre prácu v pozadí
     *
     * @param sce
     */
    /*  public void contextInitialized(ServletContextEvent sce) {
     Acp.setDaemon(this);
     finish = true;
     thread = new Thread(this);
     // thread.setDaemon(true);
     thread.start();
     }
    
     */

    public Daemon() {
        Acp.setDaemon(this);
        finish = true;
        thread = new Thread(this);
        // thread.setDaemon(true);
        thread.start();
    }
    /*

     /**
     * metóda volaná pred zastavením servera ukončí vlákna
     *
     * @param sce
     */
    /* public void contextDestroyed(ServletContextEvent sce) {
     finish = false;
     thread.interrupt();
     if (timer != null) {
     timer.cancel();
     counter.cancel();
     }
     }
     */

    /**
     * metóda vlákna pracujúce v pozadí štartuje časovača na ukončenie práce
     * následne získava dáta od kolektora a uloží do buffra
     */
    public void run() {

        while (finish) {
            //časovač treba spustiť len keď ACP beží
            if (acpRunning) {
//              TimeCounter 
                counter = new TimeCounter();
                timer = new Timer();
//              timer.schedule(counter, 0, 1000);
                timer.schedule(counter, 2000, 500);

                //ziskanie dát a následne uloženie do buffera
                while (acpRunning) {
                    String list = recieveData();

                    if (list != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(list);
                            JSONArray jsonArray = jsonObject.getJSONArray("IPFIXData");
                            fifoBuffer.add(new BufferElement(jsonArray));
                            //vypisi pre overenie
                            //     for (int i = 0; i < jsonArray.length(); i++) {
                            //         System.out.print(jsonArray.getString(i) + ",");
                            //     }
                            //     System.out.println(System.currentTimeMillis());
                        } catch (JSONException ex) {
                            Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }
            }
            try {
                thread.sleep(5L);
            } catch (InterruptedException ex) {
                Logger.getLogger(Daemon.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * @param sendThroughputInB the sendThroughputInB to set
     */
    public void setSendThroughputInB(boolean sendThroughputInB) {
        this.sendThroughputInB = sendThroughputInB;
    }

    /**
     * @param sendThroughputInP the sendThroughputInP to set
     */
    public void setSendThroughputInP(boolean sendThroughputInP) {
        this.sendThroughputInP = sendThroughputInP;
    }

    /**
     * @param sendPacketlossInB the sendPacketlossInB to set
     */
    public void setSendPacketlossInB(boolean sendPacketlossInB) {
        this.sendPacketlossInB = sendPacketlossInB;
    }

    /**
     * @param sendPacketlossInP the sendPacketlossInP to set
     */
    public void setSendPacketlossInP(boolean sendPacketlossInP) {
        this.sendPacketlossInP = sendPacketlossInP;
    }

    /**
     * @return the sendThroughputInB
     */
    public boolean isSendThroughputInB() {
        return sendThroughputInB;
    }

    /**
     * @return the sendThroughputInP
     */
    public boolean isSendThroughputInP() {
        return sendThroughputInP;
    }

    /**
     * @return the sendPacketlossInB
     */
    public boolean isSendPacketlossInB() {
        return sendPacketlossInB;
    }

    /**
     * @return the sendPacketlossInP
     */
    public boolean isSendPacketlossInP() {
        return sendPacketlossInP;
    }

//    /**
//     * Trieda vytvorená pre časovač Keď po 20tich sekúndách nepríde požiadavka
//     * na vyhodnotenie dát, tak zastaví získanie údaje od kolektora a ukončí
//     * spojenie
//     */
    //casovac uz posiela vyhodnotene data na web rozhranie
    private class TimeCounter extends TimerTask {

        @Override
        public void run() {
//            timeCounter++;
//            //ukoncenie zikanie dát po 20 sekundach
//            if (timeCounter >= 20) {
//                acpRunning = false;
//                //jedno sekundové oneskorenie pre zabezpečenie, že už acp neprijima dáta
//                //a ukončenie spojenia časovača
//                if (timeCounter > 20) {
//                    acp.quit();
//                    this.cancel(); //mozno toto netreba
//                    timer.cancel();
//                }
//
//            }

            if (!acpRunning) {
                this.cancel();
                timer.cancel();
            }
            //     jedis.publish("ACPResponse", getACPResults());
            getACPResults();

        }
    }

    //nahradi casovaca, ukonci spojenie s acp
    //je spusteni Redis triggerom na kanaly ACP prikazom stop
    public void shutDown() {
        acpRunning = false;
        acp.quit();
        if (timer != null) {
            timer.cancel();
        }
        if (counter != null) {
            counter.cancel();
        }
        timer = null;
        counter = null;
        //finish = false;
    }

    /**
     * Metóda volaná cez triedy Acp z webového rozhrania zavolá metódu na
     * nastavenie premenných pre filter potom sa pripojí na kolektora, následne
     * nastaví šablónu a filter nakoniec spustí vlákna pre získanie údaje
     *
     * @param filter reťazec v tvare JSON , filtračné parametre
     */
    public void connectToACP(String filter) {
        BasicConfigurator.configure();

        if (acp.isConnected()) {
            timeCounter = 19;
            try {
                thread.sleep(2000L);
            } catch (InterruptedException ex) {
                Logger.getLogger(Daemon.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        parseJSON(filter);

        try {
            //nadviazanie spojenia
            acp.connectToCollector(AcpSettings.getHOST(), AcpSettings.getPORT(), AcpSettings.getLOGIN(), AcpSettings.getPASSWORD());

            try {
                //nastavenie šablóny
                acp.sendTemplate(fieldsToRead);

                //nastavenie filtra
                acp.sendFilter(setFilter(monitorPointIP, sourceIP, sourcePort, destinationIP, destinationPort, protocol));
            } catch (InterruptedException ex) {
                Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
            }

            //vynulovanie premenných pre nové spojenie
            timeCounter = 0;
            acpRunning = true;
            fifoBuffer.clear();

        } catch (AuthenticationException ex) {
            Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Metóda volaná cez triedy Acp z webového rozhrania vyprázdni buffer a
     * získava hodnoty z dočasného buffra pre vyhodnotenie vyhodnotí údaje na
     * základe implementovaných komponentov
     *
     * @return JSON reťazec skladaný z vypočítaných hodnôt
     */
    //  public String getACPResults() {
    public void getACPResults() {
        timeCounter = 0;
        //String resultJson = "";//{\"name\": \"ACP\", \"response\": {";

        //keď je buffer prázdny tak , odpoved je chybová hláška
        if (!fifoBuffer.isEmpty()) {

            //zistenie dát mimo cykla pre získanie startTime
            Buffer tempBuffer = new CircularFifoBuffer(fifoBuffer);
            fifoBuffer.removeAll(tempBuffer);

            BufferElement element = (BufferElement) tempBuffer.remove();

            long startTime = element.getTime();

            JSONArray jsonArray = element.getJsonArray();
            try {
                if (jsonArray.getString(0) != null) {
                    octetDeltaCount = Long.parseLong(jsonArray.getString(0));
                }

                if (jsonArray.getString(1) != null) {
                    packetDeltaCount = Long.parseLong(jsonArray.getString(1));
                }
                if (jsonArray.getString(2) != null) {
                    droppedOctetDeltaCount = Long.parseLong(jsonArray.getString(2));
                }
                if (jsonArray.getString(3) != null) {
                    droppedPacketDeltaCount = Long.parseLong(jsonArray.getString(3));
                }
            } catch (JSONException ex) {
                Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
            }

            while (!tempBuffer.isEmpty()) {
                element = (BufferElement) tempBuffer.remove();

                jsonArray = element.getJsonArray();
                try {
                    if (jsonArray.getString(0) != null) {
                        octetDeltaCount += Integer.parseInt(jsonArray.getString(0));
                    }

                    if (jsonArray.getString(1) != null) {
                        packetDeltaCount += Long.parseLong(jsonArray.getString(1));
                    }
                    if (jsonArray.getString(2) != null) {
                        droppedOctetDeltaCount += Long.parseLong(jsonArray.getString(2));
                    }
                    if (jsonArray.getString(3) != null) {
                        droppedPacketDeltaCount += Long.parseLong(jsonArray.getString(3));
                    }
                } catch (JSONException ex) {
                    Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            // po poslednom cykle možno získať endTime
            long endTime = element.getTime();

            //vypisi pre overenie  
            //  System.out.println("startTime: " + startTime + ", endTime: " + endTime);
            //vypocet casu
            long time = (endTime + startTime) / 2;

            //vypocet rozdiel casu
            double timeDifference = ((double) (endTime - startTime)) / 1000;

            //keby sme delili mensim cislom ako 1, tak by sme dostali vacsie cislo 
            if (timeDifference < 1) {
                timeDifference = 1;
            }
            if (isSendThroughputInB()) {
                getThroughputInBytes(time, timeDifference);
            }
            if (isSendThroughputInP()) {
                getThroughputInPackets(time, timeDifference);
            }
            if (isSendPacketlossInB()) {
                getPacketLossInBytes(time, timeDifference);
            }
            if (isSendPacketlossInP()) {
                getPacketLossInPackets(time, timeDifference);
            }

            //poskladanie JSON-a v každom riadku iný komponent
            //resultJson += "\"time\": " + time;
//            resultJson += "{\"time\": " + time;
//            resultJson += getThroughputInBytes(timeDifference);
//            resultJson += getThroughputInPackets(timeDifference);
//            resultJson += getPacketLossInBytes(timeDifference);
//            resultJson += getPacketLossInPackets(timeDifference);
//            //    resultJson += "}}";
//            resultJson += "}";
        } else {
            //     resultJson += "\"error\": \"Buffer isEmpty\"}}";
//            resultJson += "{\"time\": " + System.currentTimeMillis();
//            resultJson += ",\"throughputInB\": 0";
//            resultJson += ",\"throughputInP\": 0";
//            resultJson += ",\"packetlossInP\": 0";
//            resultJson += ",\"packetlossInB\": 0";
//            resultJson += "}";
            if (isSendThroughputInB()) {
                jedis.publish("throughputInB", "{\"time\": " + System.currentTimeMillis() + ",\"throughputInB\": 0}");
                //  System.out.println("throughputInB {\"time\": " + System.currentTimeMillis() + ",\"throughputInB\": 0}");
            }
            if (isSendThroughputInP()) {
                jedis.publish("throughputInP", "{\"time\": " + System.currentTimeMillis() + ",\"throughputInP\": 0}");
                // System.out.println("throughputInP {\"time\": " + System.currentTimeMillis() + ",\"throughputInP\": 0}");
            }
            if (isSendPacketlossInP()) {
                jedis.publish("packetlossInP", "{\"time\": " + System.currentTimeMillis() + ",\"packetlossInP\": 0}");
                // System.out.println("packetlossInP {\"time\": " + System.currentTimeMillis() + ",\"packetlossInP\": 0}");
            }
            if (isSendPacketlossInB()) {
                jedis.publish("packetlossInB", "{\"time\": " + System.currentTimeMillis() + ",\"packetlossInB\": 0}");
                // System.out.println("packetlossInB {\"time\": " + System.currentTimeMillis() + ",\"packetlossInB\": 0}");
            }
        }
//        System.out.println(resultJson);
        //return resultJson;
    }

    /**
     * metoda na vypocet priepustnosti v bajtoch
     *
     * @param time rozdiel intervalu casov pre ktore ma vypocitat priepustnost
     * @return priepustnost v bajtoch
     */
    //   private String getThroughputInBytes(double time) {
    private void getThroughputInBytes(double time, double timeDifference) {
        long throughputB = Math.round(((double) octetDeltaCount) / timeDifference);
        jedis.publish("throughputInB", "{\"time\": " + time + ",\"throughputInB\": " + throughputB + "}");
        // System.out.println("throughputInB {\"time\": " + time + ",\"throughputInB\": " + throughputB + "}");
        //   return ",\"throughputInB\": " + throughputB;
    }

    /**
     * metoda na vypocet priepustnosti v paketoch
     *
     * @param time rozdiel intervalu casov pre ktore ma vypocitat priepustnost
     * @return priepustnost v paketoch
     */
    //   private String getThroughputInPackets(double time) {
    private void getThroughputInPackets(double time, double timeDifference) {
        long throughputP = (long) Math.ceil(((double) packetDeltaCount) / timeDifference);
        jedis.publish("throughputInP", "{\"time\": " + time + ",\"throughputInP\": " + throughputP + "}");
        //System.out.println("throughputInP {\"time\": " + time + ",\"throughputInP\": " + throughputP + "}");
        //return ",\"throughputInP\": " + throughputP;
    }

    /**
     * metoda na vypocet stratovosti v paketoch
     *
     * @param time rozdiel intervalu casov pre ktore ma vypocitat stratovost
     * @return stratovost v paketoch
     */
    //  private String getPacketLossInPackets(double time) {
    private void getPacketLossInPackets(double time, double timeDifference) {
        long packetLossP = (long) Math.ceil(((double) droppedPacketDeltaCount) / timeDifference);
        jedis.publish("packetlossInP", "{\"time\": " + time + ",\"packetlossInP\": " + packetLossP + "}");
        //System.out.println("packetlossInP {\"time\": " + time + ",\"packetlossInP\": " + packetLossP + "}");
        // return ",\"packetlossInP\": " + packetLossP;
    }

    /**
     * metoda na vypocet stratovosti v bajtoch
     *
     * @param time rozdiel intervalu casov pre ktore ma vypocitat stratovost
     * @return stratovost v bajtoch
     */
    //   private String getPacketLossInBytes(double time) {
    private void getPacketLossInBytes(double time, double timeDifference) {
        long packetLossB = Math.round(((double) droppedOctetDeltaCount) / timeDifference);
        jedis.publish("packetlossInB", "{\"time\": " + time + ",\"packetlossInB\": " + packetLossB + "}");
        //System.out.println("packetlossInB {\"time\": " + time + ",\"packetlossInB\": " + packetLossB + "}");
//        return ",\"packetlossInB\": " + packetLossB;
    }

    /**
     * Metóda na parsovanie JSON-a a nastavenie parametre pre filtra
     *
     * @param filter retazec vo forme JSON-a
     */
    private void parseJSON(String filter) {
        try {
            JSONObject jsonObject = new JSONObject(filter);
            JSONObject json = jsonObject.getJSONObject("filter");

            if (!json.getString("monitorpointip").equals("n/a")) {
                monitorPointIP = json.getString("monitorpointip");
            }

            if (json.getString("sourceiptag").equals("one")) {
                sourceIP = json.getString("sourceip");
            } else if (json.getString("sourceiptag").equals("range")) {
                JSONArray sourceIPs = json.getJSONArray("sourceip");
                sourceIP = sourceIPs.getString(0) + "-" + sourceIPs.getString(1);
            } else if (json.getString("sourceiptag").equals("array")) {
                JSONArray sourceIPs = json.getJSONArray("sourceip");
                sourceIP = sourceIPs.getString(0);
                for (int i = 1; i < sourceIPs.length(); i++) {
                    sourceIP += "," + sourceIPs.getString(i);
                }
            }

            if (json.getString("sourceporttag").equals("one")) {
                sourcePort = json.getString("sourceport");
            } else if (json.getString("sourceporttag").equals("range")) {
                JSONArray sourcePorts = json.getJSONArray("sourceport");
                sourcePort = sourcePorts.getString(0) + "-" + sourcePorts.getString(1);
            } else if (json.getString("sourceporttag").equals("array")) {
                JSONArray sourcePorts = json.getJSONArray("sourceport");
                sourcePort = sourcePorts.getString(0);
                for (int i = 1; i < sourcePorts.length(); i++) {
                    sourcePort += "," + sourcePorts.getString(i);
                }
            }

            if (json.getString("destinationiptag").equals("one")) {
                destinationIP = json.getString("destinationip");
            } else if (json.getString("destinationiptag").equals("range")) {
                JSONArray destinationIPs = json.getJSONArray("destinationip");
                destinationIP = destinationIPs.getString(0) + "-" + destinationIPs.getString(1);
            } else if (json.getString("destinationiptag").equals("array")) {
                JSONArray destinationIPs = json.getJSONArray("destinationip");
                destinationIP = destinationIPs.getString(0);
                for (int i = 1; i < destinationIPs.length(); i++) {
                    destinationIP += "," + destinationIPs.getString(i);
                }
            }

            if (json.getString("destinationporttag").equals("one")) {
                destinationPort = json.getString("destinationport");
            } else if (json.getString("destinationporttag").equals("range")) {
                JSONArray destinationPorts = json.getJSONArray("destinationport");
                destinationPort = destinationPorts.getString(0) + "-" + destinationPorts.getString(1);
            } else if (json.getString("destinationporttag").equals("array")) {
                JSONArray destinationPorts = json.getJSONArray("destinationport");
                destinationPort = destinationPorts.getString(0);
                for (int i = 1; i < destinationPorts.length(); i++) {
                    destinationPort += "," + destinationPorts.getString(i);
                }
            }

            if (json.getString("protocoltag").equals("one")) {
                protocol = json.getString("protocol");
            } else if (json.getString("protocoltag").equals("range")) {
                JSONArray protocols = json.getJSONArray("protocol");
                protocol = protocols.getString(0) + "-" + protocols.getString(1);
            } else if (json.getString("protocoltag").equals("array")) {
                JSONArray protocols = json.getJSONArray("protocol");
                protocol = protocols.getString(0);
                for (int i = 1; i < protocols.length(); i++) {
                    protocol += "," + protocols.getString(i);
                }
            }

        } catch (JSONException ex) {
            //Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("filter not set");
        }
    }

    /**
     * Metóda na vytvorenie filtra podla filtračných podmienok
     *
     * @param monitorPointIP //IP adresa meracieho bodu
     * @param sourceIP //zdrojová IP adresa
     * @param sourcePort //cieľová IP adresa
     * @param destinationIP //zdrojový port
     * @param destinationPort//cieľový port
     * @param protocol // protokol
     * @return Filter
     */
    private SimpleFilter setFilter(String monitorPointIP,
            String sourceIP,
            String sourcePort,
            String destinationIP,
            String destinationPort,
            String protocol) {
        Filter filter = new Filter();
        filter.ACPCreateFilter(monitorPointIP, sourceIP, sourcePort, destinationIP, destinationPort, protocol);
        SimpleFilter simpleFilter = null;
        try {
            simpleFilter = filter.createSimpleFilter();
        } catch (InvalidFilterRuleException ex) {
            System.out.println("Createsimplefilter error");
            // Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
        }

        return simpleFilter;
    }

    /**
     * Metóda pre získanie dát od kolektora
     *
     * @return získané dáta vo forme JSON
     */
    private String recieveData() {
        String jsonString = null;

        if (!acp.isPaused() || acp.getIsReceiving() == true) {
            int respondMessageType;
            try {
                respondMessageType = acp.getDatInStr().readInt();

                switch (respondMessageType) {
                    case IACP.COL_DATA_MSG:
                        acp.isReceiving(true);
                        try {
                            jsonString = (String) acp.getObjectInput().readObject();
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        acp.getDatOutStr().writeInt(5);
                        acp.isReceiving(false);
                        break;
                    case IACP.COL_ANSWER_MSG:
                        try {
                            acp.readCollectorAnswers();
                        } catch (ACPException ex) {
                            Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    default:
                        try {
                            throw new ACPException("Unknown message type received (" + respondMessageType + ").");
                        } catch (ACPException ex) {
                            Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
            } catch (IOException ex) {
                //System.out.println("IOException Error");
                // Logger.getLogger(Acp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return jsonString;
    }
}
