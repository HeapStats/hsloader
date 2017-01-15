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
package jp.dip.ysfactory.heapstats.hsloader;

import org.elasticsearch.action.bulk.BulkProcessor;

/**
 * Abstract class for file processor.
 * 
 * @author Yasumasa Suenaga
 */
public abstract class Processor {
    
    /**
     * Commandline option.
     */
    protected final Option opt;
    
    /**
     * Bulk processor of Elasticsearch.
     */
    protected final BulkProcessor bulkProcessor;
    
    /**
     * Constructor of Processor.
     * 
     * @param opt Commandline option.
     * @param bulkProcessor Bulk processor of Elasticsearch.
     */
    public Processor(Option opt, BulkProcessor bulkProcessor){
        this.opt = opt;
        this.bulkProcessor = bulkProcessor;
    }
    
    /**
     * Process the file(s).
     */
    public abstract void process();
    
}
