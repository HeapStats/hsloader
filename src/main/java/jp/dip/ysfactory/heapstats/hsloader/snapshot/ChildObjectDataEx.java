/*
 * ChildObjectDataEx.java
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

import jp.co.ntt.oss.heapstats.container.snapshot.ChildObjectData;

/**
 * Reference data holder class.
 * This class add class name to ChildClassData for serialize through Jackson.
 * 
 * @author Yasumasa Suenaga
 */
public class ChildObjectDataEx extends ChildObjectData{
    
    /**
     * Parent class tag
     */
    private final long parentClassTag;

    /**
     * Constructor of ChildObjectDataEx.
     * 
     * @param parentClassTag Class tag of parent class.
     * @param tag {@inheritDoc}
     * @param instances {@inheritDoc}
     * @param totalSize {@inheritDoc}
     */
    public ChildObjectDataEx(long parentClassTag, long tag, long instances, long totalSize) {
        super(tag, instances, totalSize);
        this.parentClassTag = parentClassTag;
    }
    
    /**
     * Constructor of ChildObjectDataEx
     * 
     * @param parentClassTag Clsas tag of parent class.
     * @param child Instance of ChildObjectData.
     */
    public ChildObjectDataEx(long parentClassTag, ChildObjectData child){
        this(parentClassTag, child.getTag(), child.getInstances(), child.getTotalSize());
    }
    
    /**
     * Getter of parent class tag.
     * 
     * @return Parent class tag of this instance.
     */
    public long getParentClassTag(){
        return parentClassTag;
    }

}
