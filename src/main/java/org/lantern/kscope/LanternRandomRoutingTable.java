package org.lantern.kscope;

import org.kaleidoscope.BasicRandomRoutingTable;

import org.codehaus.jackson.annotate.JsonIgnore; 

import com.google.inject.Singleton;

@Singleton
public class LanternRandomRoutingTable extends BasicRandomRoutingTable {

    @JsonIgnore
    public Boolean empty;

    public LanternRandomRoutingTable() {
        super();
    }
}
