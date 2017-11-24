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

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.threadpool.ThreadPool;

/**
 * Abstract class for file processor.
 * 
 * @author Yasumasa Suenaga
 */
public abstract class Processor implements AutoCloseable{

    /**
     * Commandline option.
     */
    protected final Option opt;
    
    /**
     * Elasticsearch REST client.
     */
    protected final RestHighLevelClient client;

    protected final BulkProcessor bulkProcessor;

    private boolean succeeded;

    private class BulkProcessorListener implements BulkProcessor.Listener{

        private boolean succeeded;

        public BulkProcessorListener(){
            succeeded = true;
        }

        @Override
        public void beforeBulk(long l, BulkRequest bulkRequest) {
            // Do nothing
        }

        @Override
        public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {
            // Do nothing
        }

        @Override
        public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {
            if(Boolean.getBoolean("debug")){
                throwable.printStackTrace();
            }
            succeeded = false;
        }

        public boolean isSucceeded() {
            return succeeded;
        }
    }

    private final BulkProcessorListener bulkProcessorListener;

    /**
     * Constructor of Processor.
     * 
     * @param opt Commandline option.
     */
    public Processor(Option opt){
        this.opt = opt;
        this.succeeded = true;

        int timeoutVal = opt.getTimeout() * 1000;
        this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(opt.getHost(), opt.getPort(), "http"))
                                                          .setRequestConfigCallback(b -> b.setConnectTimeout(timeoutVal).setSocketTimeout(timeoutVal))
                                                          .setMaxRetryTimeoutMillis(timeoutVal));
        this.bulkProcessorListener = new BulkProcessorListener();
        this.bulkProcessor = (new BulkProcessor.Builder(client::bulkAsync, bulkProcessorListener, new ThreadPool(Settings.builder()
                                                                                                                                 .put(Node.NODE_NAME_SETTING.getKey(), "high-level-client").build())))
                                    .setBulkActions(opt.getBulkRequests())
                                    .build();
    }

    public synchronized void publish(String index, String type, XContentBuilder contentBuilder){
        bulkProcessor.add(new IndexRequest(index, type).source(contentBuilder));
    }

    public boolean isSucceeded() {
        return bulkProcessorListener.isSucceeded();
    }

    /**
     * Process the file(s).
     */
    public abstract void process();

    @Override
    public void close() throws Exception {
        try{
            bulkProcessor.close();
        }
        catch(Exception e){
            // Do nothing
        }
        client.close();
    }
}
