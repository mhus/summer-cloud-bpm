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
import java.util.TreeMap;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.mhus.app.reactive.model.annotations.Trigger;

public abstract class XBEvent {

    private String outgoing;
    private String id;
    private String name;
    private Trigger trigger;

    public void update(Element eType, String outgoingRef, Element elem) {
        this.outgoing = outgoingRef;
        this.id = elem.getAttribute("id");
        this.name = elem.getAttribute("name");
    }

    public String getOutgoing() {
        return outgoing;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void createXml(Element xml, XElement elem, TreeMap<String, XElement> elements, int cnt) {
        //	    <bpmn2:boundaryEvent id="BoundaryEvent_2" name="" attachedToRef="Task_3">
        //	      <bpmn2:outgoing>SequenceFlow_19</bpmn2:outgoing>
        //	      <bpmn2:timerEventDefinition id="TimerEventDefinition_1"/>
        //	    </bpmn2:boundaryEvent>
        Document doc = xml.getOwnerDocument();
        Element eEvent = doc.createElement("bpmn2:boundaryEvent");
        id = elem.getId() + "-" + cnt;
        eEvent.setAttribute("id", id);
        eEvent.setAttribute("name", name);
        eEvent.setAttribute("attachedToRef", elem.getId());
        xml.appendChild(eEvent);

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
        eEvent.appendChild(eDoc);

        if (outgoing != null) {
            Element eOut = doc.createElement("bpmn2:outgoing");
            Text text = doc.createTextNode(XElement.SEQUENCE_FLOW + getId() + "_" + outgoing);
            eOut.appendChild(text);
            eEvent.appendChild(eOut);
        }

        String xmlName = getXmlElementName();
        if (xmlName != null) {
            Element eType = doc.createElement(xmlName);
            eType.setAttribute("id", id + "-Definition");
            eEvent.appendChild(eType);
        }
    }

    protected void createDocumentation(PrintWriter doc) {
        doc.println("Id: " + id);
        if (trigger != null) {
            doc.println("Type: " + trigger.type());
            doc.println("Event: " + trigger.event());
            doc.println("Abord: " + trigger.abort());
        }
        doc.println("Activity: " + outgoing);
    }

    protected abstract String getXmlElementName();

    public void update(XElement xElement, Trigger trigger, int cnt) {
        id = xElement.getId() + "-" + cnt;
        name = trigger.name();
        outgoing = trigger.activity().getCanonicalName();
        this.trigger = trigger;
    }
}
