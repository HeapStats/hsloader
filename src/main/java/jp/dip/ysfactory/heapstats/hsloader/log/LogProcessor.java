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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import jp.co.ntt.oss.heapstats.container.log.DiffData;
import jp.co.ntt.oss.heapstats.container.log.LogData;
import jp.co.ntt.oss.heapstats.task.ParseLogFile;
import jp.dip.ysfactory.heapstats.hsloader.Option;
import jp.dip.ysfactory.heapstats.hsloader.Processor;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private List<LocalDateTime> archivePointList;
    
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

    private void writeTag(JsonGenerator jsonGen, LocalDateTime dateTime) throws IOException {
        boolean isArchive = archivePointList.contains(dateTime);
        boolean isReboot = rebootSuspectList.contains(dateTime);

        if(isArchive || isReboot){
            jsonGen.writeArrayFieldStart("tag");

            if(isArchive){
                jsonGen.writeString("archive");
            }
            if(isReboot){
                jsonGen.writeString("reboot");
            }

            jsonGen.writeEndArray();
        }

    }

    private void storeLogData(LogData logData){
        StringWriter writer = new StringWriter();

        try(JsonGenerator jsonGen = (new JsonFactory()).createGenerator(writer)){
            jsonGen.writeStartObject();
            jsonGen.writeStringField("@timestamp", logData.getDateTime().atZone(opt.getZoneId()).toInstant().toString());
            jsonGen.writeStringField("logCause", logData.getLogCause().toString());
            jsonGen.writeNumberField("javaVSSize", logData.getJavaVSSize());
            jsonGen.writeNumberField("javaRSSize", logData.getJavaRSSize());
            jsonGen.writeNumberField("jvmLiveThreads", logData.getJvmLiveThreads());

            writeTag(jsonGen, logData.getDateTime());

            jsonGen.writeEndObject();
        }
        catch(IOException e){
            throw new UncheckedIOException(e);
        }

        this.pushData("heapstats-resource-log-" + logData.getDateTime().format(indexSuffixFormatter), "heapstats-resource-log", writer.toString());
    }
    
    private void storeDiffData(DiffData diffData){
        StringWriter writer = new StringWriter();

        try(JsonGenerator jsonGen = (new JsonFactory()).createGenerator(writer)){
            jsonGen.writeStartObject();
            jsonGen.writeStringField("@timestamp", diffData.getDateTime().atZone(opt.getZoneId()).toInstant().toString());
            jsonGen.writeNumberField("javaUserUsage", diffData.getJavaUserUsage());
            jsonGen.writeNumberField("javaSysUsage", diffData.getJavaSysUsage());
            jsonGen.writeNumberField("cpuUserUsage", diffData.getCpuUserUsage());
            jsonGen.writeNumberField("cpuNiceUsage", diffData.getCpuNiceUsage());
            jsonGen.writeNumberField("cpuSysUsage", diffData.getCpuSysUsage());
            jsonGen.writeNumberField("cpuIdleUsage", diffData.getCpuIdleUsage());
            jsonGen.writeNumberField("cpuIOWaitUsage", diffData.getCpuIOWaitUsage());
            jsonGen.writeNumberField("cpuIRQUsage", diffData.getCpuIRQUsage());
            jsonGen.writeNumberField("cpuSoftIRQUsage", diffData.getCpuSoftIRQUsage());
            jsonGen.writeNumberField("cpuStealUsage", diffData.getCpuStealUsage());
            jsonGen.writeNumberField("cpuGuestUsage", diffData.getCpuGuestUsage());
            jsonGen.writeNumberField("jvmSyncPark", diffData.getJvmSyncPark());
            jsonGen.writeNumberField("jvmSafepointTime", diffData.getJvmSafepointTime());
            jsonGen.writeNumberField("jvmSafepoints", diffData.getJvmSafepoints());

            writeTag(jsonGen, diffData.getDateTime());

            jsonGen.writeEndObject();
        }
        catch(IOException e){
            throw new UncheckedIOException(e);
        }

        this.pushData("heapstats-resource-diff-" + diffData.getDateTime().format(indexSuffixFormatter), "heapstats-resource-diff", writer.toString());
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
