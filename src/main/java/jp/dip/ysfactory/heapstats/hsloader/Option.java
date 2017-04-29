/*
 * Option.java
 *
 * Copyright (C) 2015-2017 Yasumasa Suenaga
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package jp.dip.ysfactory.heapstats.hsloader;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


public class Option{
    
    /**
     * Parser mode.
     */
    public static enum ParserMode{
        snapshot,
        log
    }
    
    /**
     * Default value of parser mode.
     */
    public static final ParserMode DEFAULT_PARSER_MODE = ParserMode.snapshot;
    
    private ParserMode parserMode;
    
    /**
     * Default value of target hostname.
     */
    public static final String DEFAULT_HOST = "localhost";

    /**
     * Hostname of Elasticsearch.
     */
    private String host;
    
    /**
     * Default value of target port number.
     */
    public static final int DEFAULT_PORT = 9200;

    /**
     * Port number of Elasticsearch.
     */
    private int port;
    
    /**
     * Default bulk level.
     */
    public static final int DEFAULT_BULK_LEVEL = 1000;
    
    /**
     * Num of bulk requests.
     */
    private int bulkLevel;
    
    /**
     * Default timezone.
     */
    public static final ZoneId DEFAULT_TIMEZONE = ZoneId.systemDefault();
    
    /**
     * ZoneId of SnapShot.
     */
    private ZoneId zoneId;

    private List<String> files;
    
    /**
     * Default timeout in second.
     */
    public static final int DEFAULT_TIMEOUT = 60;
    
    /**
     * Request timeout
     */
    private int timeout;

    /**
     * Print help strings.
     */
    public static void printOptions(){
      System.out.println("HSLoader 0.2.0");
      System.out.println("Copyright (C) 2015-2017 Yasumasa Suenaga");
      System.out.println();
      System.out.println("Usage:");
      System.out.println("  java -jar hsloader.jar [options] snapshot1 snapshot2 ...");
      System.out.println();
      System.out.println("Options:");
      System.out.println("  --help: This help.");
      System.out.println("  --mode <snapshot>: Parser mode. (default: snapshot)");
      System.out.println("  --host <hostname>: Hostname of Elasticsearch. (default: localhost)");
      System.out.println("  --port <num>: HTTP port of Elasticsearch. (default: 9200)");
      System.out.println("  --bulk <num>: Number of bulk requests to Elasticsearch. (default: 10)");
      System.out.println("  --timezone <zone id>: Timezone of SnapShot. (default: System Default)");
      System.out.println("  --timeout <num>: Timeout in seconds (default: 60)");
    }

    /**
     * Constructor of Option.
     * 
     * @param args Array of commandline options.
     * @throws IllegalArgumentException 
     */
    public Option(String[] args) throws IllegalArgumentException{
        parserMode = DEFAULT_PARSER_MODE;
        host = DEFAULT_HOST;
        port = DEFAULT_PORT;
        bulkLevel = DEFAULT_BULK_LEVEL;
        zoneId = DEFAULT_TIMEZONE;
        files = new ArrayList<>();
        timeout = DEFAULT_TIMEOUT;

        Iterator<String> itr = Arrays.asList(args).iterator();

        if(!itr.hasNext()){
            throw new IllegalArgumentException("SnapShot files are not selected.");
        }

        while(itr.hasNext()){
            String str = itr.next();

            switch(str){

                case "--help":
                    Option.printOptions();
                    System.exit(1);
                    
                case "--mode":
                    parserMode = ParserMode.valueOf(itr.next());
                    break;

                case "--host":
                    host = itr.next();
                    break;

                case "--port":
                    port = Integer.parseInt(itr.next());
                    break;

                case "--bulk":
                    bulkLevel = Integer.parseInt(itr.next());
                    break;
                    
                case "--timezone":
                    zoneId = ZoneId.of(itr.next());
                    break;
                    
                case "--timeout":
                    timeout = Integer.parseInt(itr.next());
                    break;
                    
                default:
                    files.add(str);

            }

        }

    }
    
    /**
     * Get parser mode.
     * 
     * @return Parser mode.
     */
    public ParserMode getParserMode(){
        return parserMode;
    }

    /**
     * Get hostname.
     * 
     * @return Hostname.
     */
    public String getHost() {
        return host;
    }

    /**
     * Get port number.
     * 
     * @return port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get bulk level.
     * @return Bulk level.
     */
    public int getBulkLevel() {
        return bulkLevel;
    }

    /**
     * Get timezone.
     * 
     * @return Timezone.
     */
    public ZoneId getZoneId() {
        return zoneId;
    }
    
    /**
     * Get timeout.
     * 
     * @return Timeout.
     */
    public int getTimeout(){
        return timeout;
    }

    /**
     * Get list of files to process.
     * 
     * @return Target file list.
     */
    public List<String> getFiles() {
        return files;
    }

}

