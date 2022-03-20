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
package de.mhus.app.reactive.util.designer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MXml;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.util.DefaultSwimlane;

public abstract class XElement {

    public static final String DEFAULT_LANE = DefaultSwimlane.class.getCanonicalName();
    public static final String SEQUENCE_FLOW = "SequenceFlow_";

    private boolean used;
    protected String id;
    protected String name;
    protected LinkedList<String> outgoing = new LinkedList<>();
    protected LinkedList<String> incoming = new LinkedList<>();
    protected LinkedList<XBEvent> boundaries = new LinkedList<>();
    protected String laneId;
    private EElement element;

    void doUpdate(EElement element) {
        this.element = element;
        id = element.getCanonicalName();
        name = element.getName();
        if (element.getSwimlane() == null) {
            laneId = DEFAULT_LANE;
        } else laneId = element.getSwimlane().getCanonicalName();

        outgoing.clear();
        incoming.clear();
        ActivityDescription desc = element.getActivityDescription();
        if (desc != null) {
            for (Output out : desc.outputs()) outgoing.add(out.activity().getCanonicalName());

            boundaries.clear();
            int cnt = 0;
            for (Trigger trigger : desc.triggers()) {
                XBEvent boundary = XmlModel.createBoundary(this, trigger, cnt);
                cnt++;
                if (boundary != null) addBoundary(boundary);
            }
        }
    }

    void connectIncoming(TreeMap<String, XElement> elements) {
        incoming.clear();
        for (Entry<String, XElement> entry : elements.entrySet()) {
            if (entry.getValue().hasOutgoing(id)) incoming.add(entry.getKey());
        }
    }

    private boolean hasOutgoing(String otherId) {
        return outgoing.contains(otherId);
    }

    void doUpdate(Element xml) {
        id = xml.getAttribute("id");
        name = xml.getAttribute("name");
        incoming.clear();
        for (Element eIn : MXml.getLocalElementIterator(xml, "bpmn2:incoming")) {
            String ref = MXml.getValue(eIn, false);
            if (ref.startsWith(XElement.SEQUENCE_FLOW)) {
                ref = ref.substring(XElement.SEQUENCE_FLOW.length());
                ref = MString.beforeIndex(ref, '_');
            }
            incoming.add(ref);
        }
        outgoing.clear();
        for (Element eOut : MXml.getLocalElementIterator(xml, "bpmn2:outgoing")) {
            String ref = MXml.getValue(eOut, false);
            if (ref.startsWith(XElement.SEQUENCE_FLOW)) {
                ref = ref.substring(XElement.SEQUENCE_FLOW.length());
                ref = MString.afterIndex(ref, '_');
            }
            outgoing.add(ref);
        }
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getId() {
        return id;
    }

    public String getLaneId() {
        return laneId;
    }

    public void connectLane(HashMap<String, String> laneRefs) {
        laneId = laneRefs.get(id);
    }

    public Element createXml(Element xml, TreeMap<String, XElement> elements) {
        Document doc = xml.getOwnerDocument();
        Element eNode = doc.createElement(getXmlElementName());
        eNode.setAttribute("id", id);
        eNode.setAttribute("name", name);
        xml.appendChild(eNode);

        // documentation
        /*
        <bpmn2:documentation id="Documentation_12"><![CDATA[Test Documentation
        Second line]]></bpmn2:documentation>
        		 */
        StringWriter out = new StringWriter();
        PrintWriter documentation = new PrintWriter(out);
        createDocumentation(documentation);
        Element eDoc = doc.createElement("bpmn2:documentation");
        CDATASection eData = doc.createCDATASection(out.toString());
        eDoc.appendChild(eData);
        eNode.appendChild(eDoc);

        // incoming
        for (String ref : incoming) {
            Element eRef = doc.createElement("bpmn2:incoming");
            Text text = doc.createTextNode(SEQUENCE_FLOW + ref + "_" + getId());
            eRef.appendChild(text);
            eNode.appendChild(eRef);
        }
        // outgoing
        for (String ref : outgoing) {
            Element eRef = doc.createElement("bpmn2:outgoing");
            Text text = doc.createTextNode(SEQUENCE_FLOW + getId() + "_" + ref);
            eRef.appendChild(text);
            eNode.appendChild(eRef);
        }

        return eNode;
    }

    protected void createDocumentation(PrintWriter doc) {
        if (element != null) {
            doc.println("Id: " + id);
            doc.println("Name: " + element.getName());
            doc.println("Type: " + getType());
            ActivityDescription desc = element.getActivityDescription();
            if (desc != null) {
                doc.println("Display Name: " + desc.displayName());
                doc.println("Description: " + desc.description());
            }
        }
    }

    protected String getType() {
        String clazz = element.getCanonicalName();
        if (clazz != null) return MString.afterLastIndex(clazz, '.');
        return MString.afterLastIndex(element.getCanonicalName(), '.');
    }

    protected abstract String getXmlElementName();

    public String getName() {
        return name;
    }

    public String[] getIncoming() {
        return incoming.toArray(new String[incoming.size()]);
    }

    public String[] getOutgoing() {
        return outgoing.toArray(new String[outgoing.size()]);
    }

    public XBEvent[] getBoundaries() {
        return boundaries.toArray(new XBEvent[boundaries.size()]);
    }

    public void addBoundary(XBEvent boundary) {
        if (boundary == null) return;
        boundaries.add(boundary);
        if (boundary.getOutgoing() != null) outgoing.add(boundary.getOutgoing());
    }
}
