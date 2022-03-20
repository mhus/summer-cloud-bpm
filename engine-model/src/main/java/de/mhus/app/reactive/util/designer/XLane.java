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

import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class XLane extends XElement {

    @Override
    public Element createXml(Element laneSet, TreeMap<String, XElement> elements) {

        // paranoia
        outgoing.clear();
        incoming.clear();

        // create node
        Element lane = super.createXml(laneSet, elements);

        // fill with refs
        Document doc = laneSet.getOwnerDocument();
        for (XElement element : elements.values()) {
            if (!(element instanceof XLane) && id.equals(element.getLaneId())) {
                Element ref = doc.createElement("bpmn2:flowNodeRef");
                lane.appendChild(ref);
                Text text = doc.createTextNode(element.getId());
                ref.appendChild(text);

                for (XBEvent event : element.getBoundaries()) {
                    ref = doc.createElement("bpmn2:flowNodeRef");
                    lane.appendChild(ref);
                    text = doc.createTextNode(event.getId());
                    ref.appendChild(text);
                }
            }
        }

        return lane;
    }

    @Override
    protected String getXmlElementName() {
        return "bpmn2:lane";
    }
}
