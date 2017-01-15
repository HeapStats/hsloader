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
package jp.dip.ysfactory.heapstats.hsloader.snapshot;

import jp.co.ntt.oss.heapstats.lambda.ConsumerWrapper;
import jp.co.ntt.oss.heapstats.parser.SnapShotParser;
import jp.dip.ysfactory.heapstats.hsloader.Option;
import jp.dip.ysfactory.heapstats.hsloader.Processor;
import org.elasticsearch.action.bulk.BulkProcessor;

/**
 * Processor class for HeapStats SnapShot files.
 * 
 * @author Yasumasa Suenaga
 */
public class SnapShotProcessor extends Processor{
    
    /**
     * {@inheritDoc}
     */
    public SnapShotProcessor(Option option, BulkProcessor bulkProcessor){
        super(option, bulkProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process() {
        SnapShotParser parser = new SnapShotParser(true);
        SnapShotHandler handler = new SnapShotHandler(bulkProcessor, opt.getZoneId());
        ConsumerWrapper<String> parseConsumer = new ConsumerWrapper<>(f -> parser.parse(f, handler));

        opt.getFiles().forEach(parseConsumer);
    }
    
}
