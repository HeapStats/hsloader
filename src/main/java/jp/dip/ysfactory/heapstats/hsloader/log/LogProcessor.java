/*
 * Copyright (C) 2016 Yasumasa Suenaga
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
package jp.dip.ysfactory.heapstats.hsloader.log;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import jp.co.ntt.oss.heapstats.container.log.DiffData;
import jp.co.ntt.oss.heapstats.container.log.LogData;
import jp.co.ntt.oss.heapstats.task.ParseLogFile;
import jp.dip.ysfactory.heapstats.hsloader.Option;
import jp.dip.ysfactory.heapstats.hsloader.Processor;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 * Processor class for HeapStats Resource Log (CSV) files.
 * 
 * @author Yasumasa Suenaga
 */
public class LogProcessor extends Processor{
    
    /**
     * Formatter for index suffix.
     */
    private static final DateTimeFormatter indexSuffixFormatter = DateTimeFormatter.ofPattern("yyyyMM");
    
    /**
     * List of archive point.
     */
    private List<LocalDateTime> archivePointList;
    
    /**
     * List of reboot suspection.
     */
    private List<LocalDateTime> rebootSuspectList;
    
    /**
     * {@inheritDoc}
     */
    public LogProcessor(Option opt, BulkProcessor bulkProcessor){
        super(opt, bulkProcessor);
    }
    
    private void storeLogData(LogData logData){
        try{
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
                                                         .startObject()
                                                         .field("@timestamp", logData.getDateTime().atZone(opt.getZoneId()).toInstant().toString())
                                                         .field("logCause", logData.getLogCause().toString())
                                                         .field("javaVSSize", logData.getJavaVSSize())
                                                         .field("javaRSSize", logData.getJavaRSSize())
                                                         .field("jvmLiveThreads", logData.getJvmLiveThreads());

            if(archivePointList.contains(logData.getDateTime())){
                jsonBuilder.field("archivePoint", 0);
            }
            if(rebootSuspectList.contains(logData.getDateTime())){
                jsonBuilder.field("rebootPoint", 0);
            }

            jsonBuilder.endObject();
            bulkProcessor.add((new IndexRequest("heapstats-resource-log-" + logData.getDateTime().format(indexSuffixFormatter), "heapstats-resource-log")).source(jsonBuilder));
        }
        catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }
    
    private void storeDiffData(DiffData diffData){
        try{
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
                                                         .startObject()
                                                         .field("@timestamp", diffData.getDateTime().atZone(opt.getZoneId()).toInstant().toString())
                                                         .field("javaUserUsage", diffData.getJavaUserUsage())
                                                         .field("javaSysUsage", diffData.getJavaSysUsage())
                                                         .field("cpuUserUsage", diffData.getCpuUserUsage())
                                                         .field("cpuNiceUsage", diffData.getCpuNiceUsage())
                                                         .field("cpuSysUsage", diffData.getCpuSysUsage())
                                                         .field("cpuIdleUsage", diffData.getCpuIdleUsage())
                                                         .field("cpuIOWaitUsage", diffData.getCpuIOWaitUsage())
                                                         .field("cpuIRQUsage", diffData.getCpuIRQUsage())
                                                         .field("cpuSoftIRQUsage", diffData.getCpuSoftIRQUsage())
                                                         .field("cpuStealUsage", diffData.getCpuStealUsage())
                                                         .field("cpuGuestUsage", diffData.getCpuGuestUsage())
                                                         .field("jvmSyncPark", diffData.getJvmSyncPark())
                                                         .field("jvmSafepointTime", diffData.getJvmSafepointTime())
                                                         .field("jvmSafepoints", diffData.getJvmSafepoints());

            if(archivePointList.contains(diffData.getDateTime())){
                jsonBuilder.field("archivePoint", 0);
            }
            if(rebootSuspectList.contains(diffData.getDateTime())){
                jsonBuilder.field("rebootPoint", 0);
            }

            jsonBuilder.endObject();
            bulkProcessor.add((new IndexRequest("heapstats-resource-diff-" + diffData.getDateTime().format(indexSuffixFormatter), "heapstats-resource-diff")).source(jsonBuilder));
        }
        catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process() {
        ParseLogFile parser = new ParseLogFile(opt.getFiles().stream().map(f -> new File(f)).collect(Collectors.toList()), true);
        
        System.out.println("Parsing...");
        parser.run();
        
        archivePointList = parser.getLogEntries()
                                 .stream()
                                 .filter(l -> l.getArchivePath() != null)
                                 .map(l -> l.getDateTime())
                                 .collect(Collectors.toList());
        rebootSuspectList = parser.getDiffEntries()
                                  .stream()
                                  .filter(d -> d.hasMinusData())
                                  .map(d -> d.getDateTime())
                                  .collect(Collectors.toList());
        
        System.out.println("Putting log data...");
        parser.getLogEntries().forEach(this::storeLogData);
        
        System.out.println("Putting diff data...");
        parser.getDiffEntries().forEach(this::storeDiffData);
        
        System.out.println("Done.");
    }
    
}
