package acp;

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


/**
 *
 * @author Szilard Jager
 */
public class AcpSettings {

    private static String HOST = "127.0.0.1"; //nazov hostitela, resp. IP adresa kolektora
    private static int PORT = 2138;           //port, na ktorom bezi kolektor
    private static String LOGIN = "bm";       //prihlasovacie meno pre nadviazanie spojenie s kollektorom
    private static String PASSWORD = "bm";    //heslo pre nadviazanie spojenie s kollektorom

    /**
     * @return the HOST
     */
    public static String getHOST() {
        return HOST;
    }

    /**
     * @param aHOST the HOST to set
     */
    public static void setHOST(String aHOST) {
        HOST = aHOST;
    }

    /**
     * @return the PORT
     */
    public static int getPORT() {
        return PORT;
    }

    /**
     * @param aPORT the PORT to set
     */
    public static void setPORT(int aPORT) {
        PORT = aPORT;
    }

    /**
     * @return the LOGIN
     */
    public static String getLOGIN() {
        return LOGIN;
    }

    /**
     * @param aLOGIN the LOGIN to set
     */
    public static void setLOGIN(String aLOGIN) {
        LOGIN = aLOGIN;
    }

    /**
     * @return the PASSWORD
     */
    public static String getPASSWORD() {
        return PASSWORD;
    }

    /**
     * @param aPASSWORD the PASSWORD to set
     */
    public static void setPASSWORD(String aPASSWORD) {
        PASSWORD = aPASSWORD;
    }

    private AcpSettings() {
    }

}
