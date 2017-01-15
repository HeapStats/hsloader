/*
 * SnapShotHandler.java
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
package jp.dip.ysfactory.heapstats.hsloader.snapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.co.ntt.oss.heapstats.container.snapshot.ChildObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.ObjectData;
import jp.co.ntt.oss.heapstats.container.snapshot.SnapShotHeader;
import jp.co.ntt.oss.heapstats.parser.SnapShotParserEventHandler;
import jp.co.ntt.oss.heapstats.parser.SnapShotParserEventHandler.ParseResult;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

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
    
    private final BulkProcessor bulkProcessor;
    
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
     * @param bulkProcessor BulkProcessor to Elasticsearch.
     * @param zoneId ZoneId of SnapShot.
     */
    public SnapShotHandler(BulkProcessor bulkProcessor, ZoneId zoneId){
        this.bulkProcessor = bulkProcessor;
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
        
        try{
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
                                                         .startObject()
                                                         .field("@timestamp", currentTimestamp)
                                                         .field("numEntries", header.getNumEntries())
                                                         .field("numInstances", header.getNumInstances())
                                                         .field("cause", header.getCauseString())
                                                         .field("gcCause", header.getGcCause())
                                                         .field("fullCount", header.getFullCount())
                                                         .field("yngCount", header.getYngCount())
                                                         .field("gcTime", header.getGcTime())
                                                         .field("newHeap", header.getNewHeap())
                                                         .field("oldHeap", header.getOldHeap())
                                                         .field("totalCapacity", header.getTotalCapacity())
                                                         .field("metaspaceUsage", header.getMetaspaceUsage())
                                                         .field("metaspaceCapacity", header.getMetaspaceCapacity())
                                                         .field("safepointTime", header.getSafepointTime())
                                                         .endObject();
            bulkProcessor.add((new IndexRequest("heapstats-snapshot-summary-" + indexNameSuffix, "heapstats-snapshot-summary")).source(jsonBuilder));
        }
        catch(IOException e){
            System.err.println(e.getLocalizedMessage());
            
            if(Boolean.getBoolean("debug")){
                e.printStackTrace();
            }
            
            return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_ABORT;
        }
        
        return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult onEntry(ObjectData data) {
        tagClassNameMap.put(data.getTag(), data.getName());
        
        try{
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
                                                         .startObject()
                                                         .field("@timestamp", currentTimestamp)
                                                         .field("tag", data.getTag())
                                                         .field("name", data.getName())
                                                         .field("classLoader", data.getClassLoader())
                                                         .field("classLoaderTag", data.getClassLoaderTag())
                                                         .field("count", data.getCount())
                                                         .field("totalSize", data.getTotalSize())
                                                         .endObject();

            bulkProcessor.add((new IndexRequest("heapstats-snapshot-objects-" + indexNameSuffix, "heapstats-snapshot-objects")).source(jsonBuilder));
        }
        catch(IOException e){
            System.err.println(e.getLocalizedMessage());
            
            if(Boolean.getBoolean("debug")){
                e.printStackTrace();
            }
            
            return SnapShotParserEventHandler.ParseResult.HEAPSTATS_PARSE_ABORT;
        }
        
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
        try{
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder()
                                                         .startObject()
                                                         .field("@timestamp", currentTimestamp)
                                                         .field("parentTag", child.getParentClassTag())
                                                         .field("parentName", tagClassNameMap.get(child.getParentClassTag()))
                                                         .field("tag", child.getTag())
                                                         .field("name", tagClassNameMap.get(child.getTag()))
                                                         .field("instalces", child.getInstances())
                                                         .field("totalSize", child.getTotalSize())
                                                         .endObject();
            bulkProcessor.add((new IndexRequest("heapstats-snapshot-refs-" + indexNameSuffix, "heapstats-snapshot-refs")).source(jsonBuilder));
        }
        catch(IOException e){
            throw new UncheckedIOException(e);
        }
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
