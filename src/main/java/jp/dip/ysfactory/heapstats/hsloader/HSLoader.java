/*
 * HSLoader.java
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
package jp.dip.ysfactory.heapstats.hsloader;

import jp.dip.ysfactory.heapstats.hsloader.log.LogProcessor;
import jp.dip.ysfactory.heapstats.hsloader.snapshot.SnapShotProcessor;


/**
 * Main class of HSLoader.
 * 
 * @author Yasumasa Suenaga
 */
public class HSLoader {
    
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

        try(Processor processor = (opt.getParserMode() == Option.ParserMode.log) ? new LogProcessor(opt) : new SnapShotProcessor(opt)) {
            processor.process();

            System.out.println();
            System.out.println(processor.isSucceeded() ? "Succeeded" : "Failed");
        }

    }
    
}
