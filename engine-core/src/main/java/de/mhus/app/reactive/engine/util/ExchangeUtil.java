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
package de.mhus.app.reactive.engine.util;

import java.io.File;
import java.io.IOException;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.pojo.MPojo;
import org.summerclouds.common.core.tool.MFile;

import de.mhus.app.reactive.engine.Engine;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNodeInfo;

public class ExchangeUtil {

    public static INode caseToINode(PCase caze) throws Exception {
        INode nc = MPojo.pojoToNode(caze, MPojo.getAttributesModelFactory(), true, false);
        //        INode nc = new MNode();
        //        nc.setString("id", caze.getId().toString());
        //        nc.setString("state", caze.getState().name());
        //        nc.setString("name", caze.getName());
        //        nc.setString("canonicalName", caze.getCanonicalName());
        //        nc.setString("milestone", caze.getMilestone());
        //        nc.setString("uri", caze.getUri());
        //        nc.setLong("scheduled", caze.getScheduled());
        //        nc.setString("customerId", caze.getCustomerId());
        //        nc.setString("customId", caze.getCustomId());
        //        if (caze.getCloseActivity() != null)
        //            nc.setString("closeActivity", caze.getCloseActivity().toString() );
        //        nc.setInt("closedCode", caze.getClosedCode());
        //        nc.setString("createdBy", caze.getCreatedBy());
        //        nc.setLong("creationDate", caze.getCreationDate());
        //        String[] index = caze.getIndexValues();
        //        if (index != null) {
        //            NodeList nIndex = nc.createArray("index");
        //            for (String idx : index)
        //                nIndex.createObject().setString(INode.NAMELESS_VALUE, idx);
        //        }
        //        INode no = nc.createObject("options");
        //        for (Entry<String, Object> entry : caze.getOptions().entrySet()) {
        //            MPojo.nodeToPojo(nc, no);
        //            if (entry.getValue().getClass().isPrimitive()
        //                    || entry.getValue().getClass() == String.class)
        //                no.setString(entry.getKey(), String.valueOf(entry.getValue()));
        //            else if (entry.getValue() instanceof Date)
        //                no.setLong(entry.getKey(), ((Date)entry.getValue()).getTime());
        //            no.setString("_class_" + entry.getKey(),
        // entry.getValue().getClass().getCanonicalName());
        //        }
        //        INode np = nc.createObject("parameters");
        //        for (Entry<String, Object> entry : caze.getParameters().entrySet()) {
        //            if (entry.getValue().getClass().isPrimitive()
        //                    || entry.getValue().getClass() == String.class)
        //                np.setString(entry.getKey(), String.valueOf(entry.getValue()));
        //            else if (entry.getValue() instanceof Date)
        //                np.setLong(entry.getKey(), ((Date)entry.getValue()).getTime());
        //            np.setString("_class_" + entry.getKey(),
        // entry.getValue().getClass().getCanonicalName());
        //        }
        return nc;
    }

    public static PCase nodeToPCase(INode in) throws Exception {
        PCase out = new PCase();
        MPojo.nodeToPojo(in, out, MPojo.getAttributesModelFactory(), true, true);
        //        setValue(out, "id", UUID.fromString(in.getString("id")));
        //        out.setState(STATE_CASE.valueOf(in.getString("state")));
        //        setValue(out, "name", in.getString("name"));
        //        setValue(out, "canonicalName", in.getString("canonicalName"));
        //        out.setMilestone(in.getString("milestone"));
        //        setValue(out,"uri", in.getString("uri"));
        //        setValue(out, "scheduled", in.getLong("scheduled", 0));
        //        if (in.isProperty("closeActivity"))
        //            setValue(out, "closeActivity",
        // UUID.fromString(in.getString("closeActivity")));
        //        setValue(out, "closedCode", in.getInt("closedCode", 0));
        //        setValue(out, "createdBy", in.getString("createdBy"));
        //        setValue(out, "creationDate", in.getLong("creationDate", 0));
        //        NodeList nIndex = in.getArrayOrNull("index");
        //        if (nIndex != null) {
        //            String[] index = new String[nIndex.size()];
        //            for (int i = 0; i < index.length; i++)
        //                index[i] = nIndex.get(i).getString(INode.NAMELESS_VALUE);
        //            out.setIndexValues(index);
        //        }
        //        INode nOptions = in.getObjectOrNull("options");
        //        if (nOptions != null) {
        //            HashMap<String, Object> options = new HashMap<>();
        //            for( Entry<String, Object> entry : nOptions.entrySet()) {
        //                if (!entry.getKey().startsWith("_")) {
        //                    Object valStr = entry.getValue();
        //                    options.put(entry.getKey(), valStr);
        //                }
        //            }
        //            setValue(out, "options", options);
        //        }
        //
        //        INode nParams = in.getObjectOrNull("parameters");
        //        if (nParams != null) {
        //            Map<String, Object> params = out.getParameters();
        //            for( Entry<String, Object> entry : nParams.entrySet()) {
        //                if (!entry.getKey().startsWith("_")) {
        //                    Object valStr = entry.getValue();
        //                    String valType = (String) nParams.get("_class_" + entry.getKey());
        //                    Object val = valStr;
        //                    if (valType != null) {
        //                        switch (valType) {
        //                        case "int":
        //                        case "java.lang.Integer":
        //                            val = MCast.toint(valStr, 0);
        //                            break;
        //                        case "long":
        //                        case "java.lang.Long":
        //                            val = MCast.tolong(valStr, 0);
        //                            break;
        //                        case "java.util.Date":
        //                            val = new Date(MCast.tolong(valStr, 0));
        //                            break;
        //                        }
        //                    }
        //                    params.put(entry.getKey(), val);
        //                }
        //            }
        //        }
        return out;
    }

    public static INode nodeToINode(PNode node) throws Exception {
        INode nn = MPojo.pojoToNode(node, MPojo.getAttributesModelFactory(), true, false);
        //        INode nn = new MNode();
        //        nn.setString("id", node.getId().toString());
        //        nn.setString("caseId", node.getCaseId().toString());
        //        nn.setString("state", node.getState().name());
        //        nn.setString("type", node.getType().name());
        //        nn.setString("assignedUser", node.getAssignedUser());
        //        nn.setString("name", node.getName());
        //        nn.setString("canonicalName", node.getCanonicalName());
        //        nn.setInt("tryCount", node.getTryCount());
        //        nn.setString("actor", node.getActor());
        //        nn.setLong("activityTimeout", node.getActivityTimeout());
        //        nn.setLong("creationDate", node.getCreationDate());
        //        nn.setLong("due", node.getDue());
        //        nn.setString("exitMessage", node.getExitMessage());
        //        nn.setString("runtimeId", node.getRuntimeId().toString());
        //        nn.setLong("lastRunDate", node.getLastRunDate());
        //        nn.setString("suspendedState", String.valueOf(node.getSuspendedState()));
        //        nn.setString("startState",String.valueOf(node.getStartState()));
        //
        //        String[] index = node.getIndexValues();
        //        if (index != null) {
        //            NodeList nIndex = nn.createArray("index");
        //            for (String idx : index)
        //                nIndex.createObject().setString(INode.NAMELESS_VALUE, idx);
        //        }
        //
        //        INode np = nn.createObject("parameters");
        //        for (Entry<String, Object> entry : node.getParameters().entrySet()) {
        //            if (entry.getValue().getClass().isPrimitive()
        //                    || entry.getValue().getClass() == String.class)
        //                np.setString(entry.getKey(), String.valueOf(entry.getValue()));
        //            else if (entry.getValue() instanceof Date)
        //                np.setLong(entry.getKey(), ((Date)entry.getValue()).getTime());
        //            np.setString("_class_" + entry.getKey(),
        // entry.getValue().getClass().getCanonicalName());
        //        }
        return nn;
    }

    public static PNode nodeToPNode(INode in) throws MException, IOException {
        PNode out = new PNode();
        MPojo.nodeToPojo(in, out, MPojo.getAttributesModelFactory(), true, true);
        //        setValue(out, "id", UUID.fromString(in.getString("id")));
        //        setValue(out, "caseId", UUID.fromString(in.getString("caseId")));
        //        out.setState(STATE_NODE.valueOf(in.getString("state")));
        //        out.setType(TYPE_NODE.valueOf(in.getString("type")));
        //        out.setAssignedUser(in.getString("assignedUser", null));
        //        setValue(out, "name", in.getString("name"));
        //        setValue(out, "canonicalName", in.getString("canonicalName"));
        //        out.setTryCount(in.getInt("tryCount", 0));
        //        setValue(out,"actor", in.getString("actor", null));
        //        out.setActivityTimeout(in.getLong("activityTimeout", 0));
        //        setValue(out, "creationDate", in.getLong("creationDate", 0));
        //        out.setDue(in.getLong("due", 0));
        //        out.setExitMessage(in.getString("exitMessage", null));
        //        if (in.isProperty("runtimeId"))
        //            out.setRuntimeNode(UUID.fromString(in.getString("runtimeId")));
        //        out.setLastRunDate(in.getLong("lastRunDate", 0));
        //        if (MString.isSet(in.getString("suspendedState", "")) &&
        // !"null".equals(in.getString("suspendedState", "")))
        //            out.setSuspendedState(STATE_NODE.valueOf(in.getString("suspendedState")));
        //        if (MString.isSet(in.getString("startState", "")) &&
        // !"null".equals(in.getString("startState", "")))
        //            setValue(out,"startState",STATE_NODE.valueOf(in.getString("suspendedState")));
        //
        //        NodeList nIndex = in.getArrayOrNull("index");
        //        if (nIndex != null) {
        //            String[] index = new String[nIndex.size()];
        //            for (int i = 0; i < index.length; i++)
        //                index[i] = nIndex.get(i).getString(INode.NAMELESS_VALUE);
        //            out.setIndexValues(index);
        //        }
        //
        //        INode nParams = in.getObjectOrNull("parameters");
        //        if (nParams != null) {
        //            Map<String, Object> params = out.getParameters();
        //            for( Entry<String, Object> entry : nParams.entrySet()) {
        //                if (!entry.getKey().startsWith("_")) {
        //                    Object valStr = entry.getValue();
        //                    String valType = (String) nParams.get("_class_" + entry.getKey());
        //                    Object val = valStr;
        //                    if (valType != null) {
        //                        switch (valType) {
        //                        case "int":
        //                        case "java.lang.Integer":
        //                            val = MCast.toint(valStr, 0);
        //                            break;
        //                        case "long":
        //                        case "java.lang.Long":
        //                            val = MCast.tolong(valStr, 0);
        //                            break;
        //                        case "java.util.Date":
        //                            val = new Date(MCast.tolong(valStr, 0));
        //                            break;
        //                        }
        //                    }
        //                    params.put(entry.getKey(), val);
        //                }
        //            }
        //        }
        return out;
    }

    public static void exportData(Engine engine, File dir) throws IOException {
        for (PCaseInfo info : engine.storageGetCases(null)) {
            try {
                PCase caze = engine.getCaseWithoutLock(info.getId());
                INode out = ExchangeUtil.caseToINode(caze);
                File f = new File(dir, "case_" + caze.getId() + ".data");
                System.out.println(f.getName());
                String content = INode.toPrettyJsonString(out);
                MFile.writeFile(f, content);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        for (PNodeInfo info : engine.storageGetFlowNodes(null, null)) {
            try {
                PNode node = engine.getNodeWithoutLock(info.getId());
                INode out = ExchangeUtil.nodeToINode(node);
                File f = new File(dir, "node_" + node.getId() + ".data");
                System.out.println(f.getName());
                String content = INode.toPrettyJsonString(out);
                MFile.writeFile(f, content);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void importData(Engine engine, File dir) {
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".data")) {
                try {
                    if (f.getName().startsWith("case_")) {
                        System.out.println(f.getName());
                        String content = MFile.readFile(f);
                        INode in = INode.readFromJsonString(content);
                        PCase out = ExchangeUtil.nodeToPCase(in);
                        engine.getStorage().saveCase(out);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".data")) {
                try {
                    if (f.getName().startsWith("node_")) {
                        System.out.println(f.getName());
                        String content = MFile.readFile(f);
                        INode in = INode.readFromJsonString(content);
                        PNode out = ExchangeUtil.nodeToPNode(in);
                        engine.getStorage().saveFlowNode(out);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    //    private static void setValue(Object entry, String fieldName, Object value) {
    //        if (entry == null) return;
    //        try {
    //            Field field = entry.getClass().getDeclaredField(fieldName);
    //            if (!field.canAccess(entry)) field.setAccessible(true);
    //            field.set(entry, value);
    //        } catch (Exception e) {
    //            throw new RuntimeException("Entry " + entry.getClass(), e);
    //        }
    //    }

}
