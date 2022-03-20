/**
 * Copyright (C) 2018 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.app.reactive.model.engine;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

import org.summerclouds.common.core.tool.MCast;

public class EngineMessage implements Externalizable {

    public enum TYPE {
        OTHER,
        FLOW,
        ERROR,
        DEBUG,
        CONNECT,
        START
    }

    public static final String FLOW_PREFIX = "flow:";
    public static final String CONNECT_PREFIX = "connect:";
    public static final String START_PREFIX = "start:";
    public static final String ERROR_PREFIX = "error:";
    public static final String DEBUG_PREFIX = "debug:";
    private TYPE type = TYPE.OTHER;
    private String msg;
    private UUID fromNode;
    private UUID toNode;
    private String originalMsg;
    private long ts;
    private String ident;

    public EngineMessage(String msg) {
        originalMsg = msg;
        int p = msg.indexOf('|');
        if (p >= 0) {
            String prefix = msg.substring(0, p);
            msg = msg.substring(p + 1);
            p = prefix.indexOf(',');
            if (p > 0) {
                ts = MCast.tolong(prefix.substring(0, p), 0);
                ident = prefix.substring(p + 1);
            } else ts = MCast.tolong(prefix, 0);
        }
        p = msg.indexOf(':');
        if (p >= 0) {
            String t = msg.substring(0, p + 1);
            switch (t) {
                case FLOW_PREFIX:
                    type = TYPE.FLOW;
                    break;
                case ERROR_PREFIX:
                    type = TYPE.ERROR;
                    break;
                case DEBUG_PREFIX:
                    type = TYPE.DEBUG;
                    break;
                case CONNECT_PREFIX:
                    type = TYPE.CONNECT;
                    break;
                case START_PREFIX:
                    type = TYPE.START;
                    break;
            }
            if (type != TYPE.OTHER) msg = msg.substring(p + 1);
            switch (type) {
                case CONNECT:
                    p = msg.indexOf(',');
                    if (p > 0) {
                        fromNode = UUID.fromString(msg.substring(0, p));
                        toNode = UUID.fromString(msg.substring(p + 1));
                    }
                    break;
                case FLOW:
                case START:
                    p = msg.indexOf(',');
                    if (p > 0) {
                        fromNode = UUID.fromString(msg.substring(0, p));
                        msg = msg.substring(p + 1);
                    }
                    break;
                case ERROR:
                case DEBUG:
                case OTHER:
                    break;
            }
        }

        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }

    public TYPE getType() {
        return type;
    }

    public UUID getFromNode() {
        return fromNode;
    }

    public UUID getToNode() {
        return toNode;
    }

    public long getTimestamp() {
        return ts;
    }

    public String getServerIdent() {
        return ident;
    }

    @Override
    public String toString() {
        return originalMsg;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(2);
        out.writeObject(type);
        out.writeObject(msg);
        out.writeObject(fromNode);
        out.writeObject(toNode);
        out.writeObject(originalMsg);
        out.writeLong(ts);
        out.writeObject(ident);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int v = in.readInt();
        if (v < 1 || v > 2) throw new IOException("Wrong object version");
        type = (TYPE) in.readObject();
        msg = (String) in.readObject();
        fromNode = (UUID) in.readObject();
        toNode = (UUID) in.readObject();
        originalMsg = (String) in.readObject();
        ts = in.readLong();
        if (v > 1) {
            ident = (String) in.readObject();
        }
    }
}
