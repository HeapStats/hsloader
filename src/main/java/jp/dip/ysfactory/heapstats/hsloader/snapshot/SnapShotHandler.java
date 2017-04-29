/*
 * SnapShotHandler.java
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
package jp.dip.ysfactory.heapstats.hsloader.snapshot;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import jp.co.ntt.oss.heapstats.container.snapshot.ChildObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.ObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.SnapShotHeader;
import jp.co.ntt.oss.heapstats.parser.SnapShotParserEventHandler;
import jp.dip.ysfactory.heapstats.hsloader.Processor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HeapStats SnapShot parser event handler.
 * 
 * @author Yasumasa Suenaga
 */
public class SnapShotHandler implements SnapShotParserEventHandler{
    
    /**
     * Map for JVMTI tag and Class Name.
     * This field will be used for set class name to ChildObjectDataEx.
     */
    private Map<Long, String> tagClassNameMap;
    
    private List<ChildObjectDataEx> childrenList;
    
    private final Processor processor;
    
    /**
     * ZoneId of SnapShot.
     */
    private final ZoneId zoneId;
    
    /**
     * Formatter for index suffix.
     */
    private static final DateTimeFormatter indexSuffixFormatter = DateTimeFormatter.ofPattern("yyyyMM");
    
    /**
     * Index suffix
     */
    private String indexNameSuffix;

    private String currentTimestamp;
    
    /**
     * Constructor for SnapShotHandler.
     * 
     * @param processor Elasticsearch bulk operation processor.
     * @param zoneId ZoneId of SnapShot.
     */
    public SnapShotHandler(Processor processor, ZoneId zoneId){
        this.processor = processor;
        this.zoneId = zoneId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult onStart(long off) {
        // Do nothing
        return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult onNewSnapShot(SnapShotHeader header, String parent) {
        System.out.println("Parse snapshot at " + header.getSnapShotDate().toString());
        tagClassNameMap = new HashMap<>();
        childrenList = new ArrayList<>();
        indexNameSuffix = header.getSnapShotDate().format(indexSuffixFormatter);
        currentTimestamp = header.getSnapShotDate().atZone(zoneId).toInstant().toString();

        StringWriter writer = new StringWriter();

        try(JsonGenerator jsonGen = (new JsonFactory()).createGenerator(writer)){
            jsonGen.writeStartObject();
            jsonGen.writeStringField("@timestamp", currentTimestamp);
            jsonGen.writeNumberField("numEntries", header.getNumEntries());
            jsonGen.writeNumberField("numInstances", header.getNumInstances());
            jsonGen.writeStringField("cause", header.getCauseString());
            jsonGen.writeStringField("gcCause", header.getGcCause());
            jsonGen.writeNumberField("fullCount", header.getFullCount());
            jsonGen.writeNumberField("yngCount", header.getYngCount());
            jsonGen.writeNumberField("gcTime", header.getGcTime());
            jsonGen.writeNumberField("newHeap", header.getNewHeap());
            jsonGen.writeNumberField("oldHeap", header.getOldHeap());
            jsonGen.writeNumberField("totalCapacity", header.getTotalCapacity());
            jsonGen.writeNumberField("metaspaceUsage", header.getMetaspaceUsage());
            jsonGen.writeNumberField("metaspaceCapacity", header.getMetaspaceCapacity());
            jsonGen.writeNumberField("safepointTime", header.getSafepointTime());
            jsonGen.writeEndObject();
        }
        catch(IOException e){
            System.err.println(e.getLocalizedMessage());
            
            if(Boolean.getBoolean("debug")){
                e.printStackTrace();
            }
            
            return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_ABORT;
        }

        processor.pushData("heapstats-snapshot-summary-" + indexNameSuffix, "heapstats-snapshot-summary", writer.toString());
        
        return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult onEntry(ObjectData data) {
        tagClassNameMap.put(data.getTag(), data.getName());
        StringWriter writer = new StringWriter();

        try(JsonGenerator jsonGen = (new JsonFactory()).createGenerator(writer)){
            jsonGen.writeStartObject();
            jsonGen.writeStringField("@timestamp", currentTimestamp);
            jsonGen.writeNumberField("tag", data.getTag());
            jsonGen.writeStringField("name", data.getName());
            jsonGen.writeNumberField("classLoader", data.getClassLoader());
            jsonGen.writeNumberField("classLoaderTag", data.getClassLoaderTag());
            jsonGen.writeNumberField("count", data.getCount());
            jsonGen.writeNumberField("totalSize", data.getTotalSize());
            jsonGen.writeEndObject();
        }
        catch(IOException e){
            System.err.println(e.getLocalizedMessage());
            
            if(Boolean.getBoolean("debug")){
                e.printStackTrace();
            }
            
            return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_ABORT;
        }

        processor.pushData("heapstats-snapshot-objects-" + indexNameSuffix, "heapstats-snapshot-objects", writer.toString());

        return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult onChildEntry(long parentClassTag, ChildObjectData child) {
        childrenList.add(new ChildObjectDataEx(parentClassTag, child));
        return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }
    
    private void putChildData(ChildObjectDataEx child){
        StringWriter writer = new StringWriter();

        try(JsonGenerator jsonGen = (new JsonFactory()).createGenerator(writer)){
            jsonGen.writeStartObject();
            jsonGen.writeStringField("@timestamp", currentTimestamp);
            jsonGen.writeNumberField("parentTag", child.getParentClassTag());
            jsonGen.writeStringField("parentName", tagClassNameMap.get(child.getParentClassTag()));
            jsonGen.writeNumberField("tag", child.getTag());
            jsonGen.writeStringField("name", tagClassNameMap.get(child.getTag()));
            jsonGen.writeNumberField("instalces", child.getInstances());
            jsonGen.writeNumberField("totalSize", child.getTotalSize());
            jsonGen.writeEndObject();
        }
        catch(IOException e){
            throw new UncheckedIOException(e);
        }

        processor.pushData("heapstats-snapshot-refs-" + indexNameSuffix, "heapstats-snapshot-refs", writer.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult onFinish(long off) {
        childrenList.forEach(this::putChildData);
        return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }
    
}
