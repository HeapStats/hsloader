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
package jp.dip.ysfactory.heapstats.hsloader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class for file processor.
 * 
 * @author Yasumasa Suenaga
 */
public abstract class Processor implements AutoCloseable{

    private static class IndexData{

        private final String indexName;

        private final String indexType;

        private final String jsonData;

        public IndexData(String indexName, String indexType, String jsonData) {
            this.indexName = indexName;
            this.indexType = indexType;
            this.jsonData = jsonData;
        }

        @Override
        public String toString() {
            StringWriter writer = new StringWriter();

            try(JsonGenerator jsonGen = (new JsonFactory()).createGenerator(writer)){
                jsonGen.writeStartObject();
                jsonGen.writeObjectFieldStart("index");
                jsonGen.writeStringField("_index", indexName);
                jsonGen.writeStringField("_type", indexType);
                jsonGen.writeEndObject();
                jsonGen.writeEndObject();
            }
            catch(IOException e){
                throw new UncheckedIOException(e);
            }

            writer.append('\n');
            writer.append(jsonData);

            return writer.toString();
        }

    }
    
    /**
     * Commandline option.
     */
    protected final Option opt;
    
    /**
     * Elasticsearch REST client.
     */
    protected final RestClient client;

    private final List<IndexData> indexDataList;

    private boolean succeeded;
    
    /**
     * Constructor of Processor.
     * 
     * @param opt Commandline option.
     */
    public Processor(Option opt){
        this.opt = opt;
        this.indexDataList = new ArrayList<>(opt.getBulkLevel());
        this.succeeded = true;

        int timeoutVal = opt.getTimeout() * 1000;
        this.client = RestClient.builder(new HttpHost(opt.getHost(), opt.getPort(), "http"))
                                  .setRequestConfigCallback(b -> b.setConnectTimeout(timeoutVal).setSocketTimeout(timeoutVal))
                                  .setMaxRetryTimeoutMillis(timeoutVal)
                                  .build();
    }

    public synchronized void publish(){

        if(indexDataList.isEmpty()){
            return;
        }

        String putMessage = indexDataList.stream()
                                           .map(IndexData::toString)
                                           .collect(Collectors.joining("\n"));
        indexDataList.clear();

        try {
            Response response = client.performRequest("PUT", "/_bulk", Collections.emptyMap(), new NStringEntity(putMessage, ContentType.create("application/x-ndjson")));

            JsonFactory factory = new JsonFactory();
            try (InputStream content = response.getEntity().getContent();
                 JsonParser responseParser = factory.createParser(content);) {
                while(responseParser.nextToken() != JsonToken.END_OBJECT){
                    String objName = responseParser.getCurrentName();
                    if((objName != null) && objName.equals("errors")){

                        if(responseParser.getValueAsBoolean()){
                            succeeded = false;
                        }

                        break;
                    }
                }
            }

        }
        catch (IOException e){
            throw new UncheckedIOException(e);
        }

    }

    public synchronized void pushData(String indexName, String indexType, String jsonData){
        indexDataList.add(new IndexData(indexName, indexType, jsonData));
        if(indexDataList.size() == opt.getBulkLevel()){
            publish();
        }
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    /**
     * Process the file(s).
     */
    public abstract void process();

    @Override
    public void close() throws Exception {

        if(!indexDataList.isEmpty()){
            publish();
        }

        client.close();
    }
}
