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

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.error.MRuntimeException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;

public class SearchCriterias implements Externalizable, Cloneable {

    public String name;
    public String custom;
    public String customer;

    public String process;
    public String version;
    public String pool;

    public boolean unassigned;
    public String assigned;
    public STATE_NODE nodeState;
    public String[] index;
    public STATE_CASE caseState;
    public String uri;
    public UUID caseId;
    public TYPE_NODE type;
    public int due = -1;

    public enum ORDER {
        CUSTOM,
        CUSTOMER,
        NAME,
        PROCESS,
        VERSION,
        POOL,
        STATE,
        TYPE,
        INDEX0,
        INDEX1,
        INDEX2,
        INDEX3,
        INDEX4,
        INDEX5,
        INDEX6,
        INDEX7,
        INDEX8,
        INDEX9,
        CREATED,
        MODIFIED,
        PRIORITY,
        SCORE,
        MILESTONE
    };

    public ORDER order;
    public boolean orderAscending = true;

    public int priority = Integer.MAX_VALUE;
    public int score = Integer.MIN_VALUE;

    public String milestone;

    public String[] actors;
    public int limit = 0;
    public boolean or = false;

    public SearchCriterias() {}

    public SearchCriterias(String[] parameters) {
        this(parameters == null ? new MProperties() : IProperties.explodeToMProperties(parameters));
    }

    public SearchCriterias(MProperties parameters) {
        parse(parameters);
    }

    public void parse(IProperties parameters) {
        if (parameters == null) return;
        or = false;
        for (String k : parameters.keySet()) {
            String v = parameters.getString(k, null);
            switch (k) {
                case "due":
                    due = M.to(v, -1);
                    break;
                case "state":
                    try {
                        caseState = STATE_CASE.valueOf(v.toUpperCase());
                    } catch (Throwable t) {
                    }
                    try {
                        nodeState = STATE_NODE.valueOf(v.toUpperCase());
                    } catch (Throwable t) {
                    }
                    break;
                case "type":
                    type = TYPE_NODE.valueOf(v.toUpperCase());
                    break;
                case "priority":
                    priority = M.to(v, priority);
                    break;
                case "score":
                    score = M.to(v, score);
                    break;
                case "order":
                    order = ORDER.valueOf(v.toUpperCase());
                    break;
                case "ascending":
                    orderAscending = M.to(v, true);
                    break;
                case "uri":
                    uri = v;
                    break;
                case "process":
                    process = v;
                    break;
                case "version":
                    version = v;
                    break;
                case "pool":
                    pool = v;
                    break;
                case "custom":
                    custom = v;
                    break;
                case "customer":
                    customer = v;
                    break;
                case "name":
                    name = v;
                    break;
                case "case":
                    caseId = UUID.fromString(v);
                    break;
                case "unassigned":
                    unassigned = M.to(v, false);
                    break;
                case "assigned":
                    assigned = v;
                    break;
                case "":
                case "search":
                    String x = "*" + v + "*";
                    index = new String[] {x, x, x, x, x, x, x, x, x, x};
                    name = x;
                    custom = x;
                    or = true;
                    break;
                case "index0":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index1":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index2":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index3":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index4":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index5":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index6":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index7":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index8":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "index9":
                    if (index == null)
                        index =
                                new String[] {
                                    null, null, null, null, null, null, null, null, null, null
                                };
                    index[0] = v;
                    break;
                case "milestone":
                    milestone = v;
                    break;
                case "limit":
                    limit = M.to(v, 0);
                default:
            }
        }
    }

    @Override
    public String toString() {
        return MSystem.toString(
                this,
                "name",
                name,
                "custom",
                custom,
                "customer",
                customer,
                "process",
                process,
                "version",
                version,
                "pool",
                pool,
                "unassigned",
                unassigned,
                "assigned",
                assigned,
                "nodeState",
                nodeState,
                "index",
                index,
                "caseState",
                caseState,
                "uri",
                uri,
                "caseId",
                caseId,
                "type",
                type,
                "order",
                order,
                "orderAscending",
                orderAscending,
                "priority",
                priority,
                "score",
                score,
                "milestone",
                milestone,
                "actors",
                actors,
                "due",
                due);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(2);
        out.writeObject(name);
        out.writeObject(custom);
        out.writeObject(customer);
        out.writeObject(process);
        out.writeObject(version);
        out.writeObject(pool);
        out.writeBoolean(unassigned);
        out.writeObject(assigned);
        out.writeObject(nodeState);
        out.writeObject(index);
        out.writeObject(caseState);
        out.writeObject(uri);
        out.writeObject(caseId);
        out.writeObject(type);
        out.writeObject(order);
        out.writeBoolean(orderAscending);
        out.writeInt(priority);
        out.writeInt(score);
        out.writeObject(milestone);
        out.writeObject(actors);
        out.writeInt(limit);
        out.writeInt(due);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int v = in.readInt();
        if (v < 1 || v > 2) throw new IOException("Wrong object version");

        name = (String) in.readObject();
        custom = (String) in.readObject();
        customer = (String) in.readObject();
        process = (String) in.readObject();
        version = (String) in.readObject();
        pool = (String) in.readObject();
        unassigned = in.readBoolean();
        assigned = (String) in.readObject();
        nodeState = (STATE_NODE) in.readObject();
        index = (String[]) in.readObject();
        caseState = (STATE_CASE) in.readObject();
        uri = (String) in.readObject();
        caseId = (UUID) in.readObject();
        type = (TYPE_NODE) in.readObject();
        order = (ORDER) in.readObject();
        orderAscending = in.readBoolean();
        priority = in.readInt();
        score = in.readInt();
        milestone = (String) in.readObject();
        actors = (String[]) in.readObject();
        limit = in.readInt();

        if (v >= 2) {
            due = in.readInt();
        } else {
            due = -1;
        }
    }

    public static String[] keys() {
        return new String[] {
            "state",
            "type",
            "priority",
            "score",
            "order",
            "ascending",
            "uri",
            "process",
            "version",
            "pool",
            "custom",
            "customer",
            "name",
            "case",
            "unassigned",
            "assigned",
            "search",
            "index0",
            "index1",
            "index2",
            "index3",
            "index4",
            "index5",
            "index6",
            "index7",
            "index8",
            "index9",
            "milestone",
            "limit",
            "due"
        };
    }

    @Override
    public SearchCriterias clone() {
        try {
            return (SearchCriterias) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new MRuntimeException(RC.STATUS.ERROR, e);
        }
    }
}
