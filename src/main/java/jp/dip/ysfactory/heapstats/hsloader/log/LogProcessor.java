/*
 * Copyright (C) 2016-2017 Yasumasa Suenaga
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

import jp.co.ntt.oss.heapstats.container.log.DiffData;
import jp.co.ntt.oss.heapstats.container.log.LogData;
import jp.co.ntt.oss.heapstats.task.ParseLogFile;
import jp.dip.ysfactory.heapstats.hsloader.Option;
import jp.dip.ysfactory.heapstats.hsloader.Processor;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private Map<LocalDateTime, String> archivePoints;
    
    /**
     * List of reboot suspection.
     */
    private List<LocalDateTime> rebootSuspectList;
    
    /**
     * {@inheritDoc}
     */
    public LogProcessor(Option opt){
        super(opt);
    }

    private XContentBuilder writeTag(XContentBuilder builder, LocalDateTime dateTime) throws IOException {
        boolean isArchive = archivePoints.containsKey(dateTime);
        boolean isReboot = rebootSuspectList.contains(dateTime);

        if(isArchive || isReboot){
            builder.startArray("tag");
            if(isArchive){
                builder.value("archive");
            }
            if(isReboot){
                builder.value("reboot");
            }
            builder.endArray();
        }
        if(isArchive){
            builder.field("archivePath", archivePoints.get(dateTime));
        }

        return builder;
    }

    private void storeLogData(LogData logData){
        try{
            XContentBuilder builder = XContentFactory.jsonBuilder()
                                                     .startObject()
                                                     .field("@timestamp", logData.getDateTime().atZone(opt.getZoneId()).toInstant().toString())
                                                     .field("logCause", logData.getLogCause().toString())
                                                     .field("javaVSSize", logData.getJavaVSSize())
                                                     .field("javaRSSize", logData.getJavaRSSize())
                                                     .field("jvmLiveThreads", logData.getJvmLiveThreads());
            writeTag(builder, logData.getDateTime())
                .endObject();
            this.publish("heapstats-resource-log-" + logData.getDateTime().format(indexSuffixFormatter), "heapstats-resource-log", builder);
        }
        catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }
    
    private void storeDiffData(DiffData diffData){
        try{
            XContentBuilder builder = XContentFactory.jsonBuilder()
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
            writeTag(builder, diffData.getDateTime())
                .endObject();
            this.publish("heapstats-resource-diff-" + diffData.getDateTime().format(indexSuffixFormatter), "heapstats-resource-diff", builder);
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

        archivePoints = parser.getLogEntries()
                              .stream()
                              .filter(l -> l.getArchivePath() != null)
                              .collect(Collectors.toMap(LogData::getDateTime, LogData::getArchivePath));
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
