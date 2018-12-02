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

//import javax.jws.WebMethod;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

//import javax.jws.WebParam;
//import javax.jws.WebService;
/**
 *
 * @author Szilard Jager
 */
//@WebService(serviceName = "ACP")
public class Acp implements Runnable {

    //druha vlakna co nadviaze spojenie s kolektorom
    //a posle vyhodnotene data na web
    private static Daemon daemon;
    //pomocou Redis aplikacie je zabezpeceny real-time komunikacia s webom
    private Jedis jedis = new Jedis("localhost");
    //aj tato trieda je samostatne vlakno
    private Thread thread;

    /**
     * Metóda volaná vláknom pracujúci v pozadí pre nastavenie premennej, aby
     * vlákno bolo dosiahnuteľné
     *
     * @param daemonToSet inštancia z triedy Daemon, vlákna pracujúci v pozadí
     */
    public static void setDaemon(Daemon daemonToSet) {
        daemon = daemonToSet;
    }

    public Acp() {
        thread = new Thread(this);
        thread.start();
    }
    /*   public void contextInitialized(ServletContextEvent sce) {
     thread = new Thread(this);
     thread.start();
     }

     public void contextDestroyed(ServletContextEvent sce) {
     //jedis.close();
     thread.interrupt();
     }
     */

    //ked je aplikacia deployed na server, vytvori vlakno co subscribe pre kanal ACP
    //a caka, na start alebo stop
    public void run() {
        try {
            jedis.subscribe(new JedisPubSubImpl(), "ACP");
        } catch (JedisConnectionException ex) {
            jedis.close();
        }
    }
//    /**
//     * Webová metóda volaným webovým rozhraním
//     * Metóda zavolá metódu connectToACP vlákna, na nadviazanie spojenia s kolektorom
//     * @param filter filtračné parametre vo forme JSON-a
//     */
//    @WebMethod(operationName = "connectToACP")
//    public void connectToACP(@WebParam(name = "filter") String filter) {
//     //   System.out.println("<ACP>: Recieving filter: " + filter);
//        
//        daemon.connectToACP(filter); 
//    }
//
//    /**
//     * Webová metóda volaným webovým rozhraním
//     * Metóda zavolá metódu getACPResults vlákna na vyhodnotenie dát
//     * @return vypočítané hodnoty vo forme JSON-a
//     */
//    @WebMethod(operationName = "getACPResults")
//    public String getACPResults() {
//        return daemon.getACPResults();
//    } 

    private class JedisPubSubImpl extends JedisPubSub {

        public JedisPubSubImpl() {
        }

        @Override
        public void onMessage(String arg0, String arg1) {
            //ked pride stop na kanal ukonci spojenie acp
            if (arg1.endsWith("stop")) {
                if (arg1.startsWith("throughputInB")) {
                    daemon.setSendThroughputInB(false);
                } else if (arg1.startsWith("throughputInP")) {
                    daemon.setSendThroughputInP(false);
                } else if (arg1.startsWith("packetlossInB")) {
                    daemon.setSendPacketlossInB(false);
                } else if (arg1.startsWith("packetlossInP")) {
                    daemon.setSendPacketlossInP(false);
                }

                boolean shutDown = daemon.isSendPacketlossInB() || daemon.isSendPacketlossInP() || daemon.isSendThroughputInB() || daemon.isSendThroughputInP();
                if (!shutDown) {
                    daemon.shutDown();
                }
                //ked pride start, nadviaze spojenie s acp a zacne posielat vyhodnotene data
            } else if (arg1.endsWith("start")) {

                boolean start = daemon.isSendPacketlossInB() || daemon.isSendPacketlossInP() || daemon.isSendThroughputInB() || daemon.isSendThroughputInP();
                if (!start) {
                    daemon.connectToACP(arg1);
                }
                System.out.println("get trigger to start, starting");
                if (arg1.startsWith("throughputInB")) {
                    daemon.setSendThroughputInB(true);
                } else if (arg1.startsWith("throughputInP")) {
                    daemon.setSendThroughputInP(true);
                } else if (arg1.startsWith("packetlossInB")) {
                    daemon.setSendPacketlossInB(true);
                } else if (arg1.startsWith("packetlossInP")) {
                    daemon.setSendPacketlossInP(true);
                }

            }
        }

        @Override
        public void onPMessage(String arg0, String arg1, String arg2) {

        }

        @Override
        public void onSubscribe(String arg0, int arg1) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onUnsubscribe(String arg0, int arg1) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        }

        @Override
        public void onPUnsubscribe(String arg0, int arg1) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onPSubscribe(String arg0, int arg1) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
