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

import org.json.me.JSONArray;

/**
 * Trieda vytvorená pre buffer, abz bolo mozne aj uložiť čas kedy boli vytvorené záznamy
 * @author Szilard Jager
 */
public class BufferElement {
    private long time;
    private JSONArray jsonArray;
    
    /**
     * Konštruktor nastaví premennú time na aktuálny čas 
     * @param jsonArray dáta získané od kolektora
     */
    public BufferElement(JSONArray jsonArray){
        this.time = System.currentTimeMillis();
        this.jsonArray = jsonArray;
    }

    /**
     * @return time čas vytvorenie záznamu
     */
    public long getTime() {
        return time;
    }

    /**
     * @return jsonArray dáta získané od kolektora
     */
    public JSONArray getJsonArray() {
        return jsonArray;
    }
    
    
}
