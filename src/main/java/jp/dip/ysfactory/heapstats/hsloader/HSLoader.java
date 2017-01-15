/*
 * HSLoader.java
 *
 * Copyright (C) 2015-2016 Yasumasa Suenaga
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

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import jp.dip.ysfactory.heapstats.hsloader.log.LogProcessor;
import jp.dip.ysfactory.heapstats.hsloader.snapshot.SnapShotProcessor;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;


/**
 * Main class of HSLoader.
 * 
 * @author Yasumasa Suenaga
 */
public class HSLoader {
    
    private static class BulkProcessListener implements BulkProcessor.Listener{
        
        private final int timeout;
        
        public BulkProcessListener(int timeout){
            this.timeout = timeout;
        }

        @Override
        public void beforeBulk(long l, BulkRequest br) {
            br.timeout(TimeValue.timeValueSeconds(timeout));
        }

        @Override
        public void afterBulk(long l, BulkRequest req, BulkResponse res) {
            if(res.hasFailures()){
                throw new RuntimeException(res.buildFailureMessage());
            }
        }

        @Override
        public void afterBulk(long l, BulkRequest br, Throwable thrwbl) {
            throw new RuntimeException(thrwbl);
        }
        
    }
    
    private static class ExceptionHandler implements Thread.UncaughtExceptionHandler{

        @Override
        public void uncaughtException(Thread thread, Throwable thrwbl) {
            Throwable cause = thrwbl;
            while(cause.getCause() != null){
                cause = cause.getCause();
            }
            
            System.err.println(cause.getLocalizedMessage());
            
            if(Boolean.getBoolean("debug")){
                thrwbl.printStackTrace();
            }
            
        }
        
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws Exception{
        Option opt;
        try{
            opt = new Option(args);
        }
        catch(Exception e){
            Option.printOptions();
            return;
        }
        
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        
        InetSocketTransportAddress addr = new InetSocketTransportAddress(InetAddress.getByName(opt.getHost()), opt.getPort());
        
        try(TransportClient client = (new PreBuiltTransportClient(Settings.EMPTY)).addTransportAddress(addr);
            BulkProcessor bulkProcessor = BulkProcessor.builder(client, new BulkProcessListener(opt.getTimeout()))
                                                       .setName("HSLoader bulk processor")
                                                       .setBulkActions(opt.getBulkLevel())
                                                       .setConcurrentRequests(Runtime.getRuntime().availableProcessors())
                                                       .build()){
            
            Processor processor = null;
            
            if(opt.getParserMode() == Option.ParserMode.snapshot){
                processor = new SnapShotProcessor(opt, bulkProcessor);
            }
            else if(opt.getParserMode() == Option.ParserMode.log){
                processor = new LogProcessor(opt, bulkProcessor);
            }
            
            Objects.requireNonNull(processor, "Processor must not be null");
            processor.process();
            
            bulkProcessor.awaitClose(opt.getTimeout(), TimeUnit.SECONDS);
        }

    }
    
}
