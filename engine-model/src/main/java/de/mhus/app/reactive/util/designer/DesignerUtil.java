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

import java.io.File;
import java.util.UUID;

import org.summerclouds.common.core.tool.MXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DesignerUtil {

    public static void createDocument(XmlModel model, File file) throws Exception {
        Document doc = createXmlDocument(model);
        MXml.saveXml(doc.getDocumentElement(), file);
    }

    public static Document createXmlDocument(XmlModel model) throws Exception {
        Document doc = MXml.createDocument();
        // <bpmn2:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        // xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
        // xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
        // xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
        // xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
        // xmlns:xs="http://www.w3.org/2001/XMLSchema" id="_ZmpfgHPxEeiDG5oeS1KwtA"
        // exporter="org.eclipse.bpmn2.modeler.core"
        // exporterVersion="1.4.3.Final-v20180418-1358-B1">
        Element root = doc.createElement("bpmn2:definitions");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("xmlns:bpmn2", "http://www.omg.org/spec/BPMN/20100524/MODEL");
        root.setAttribute("xmlns:bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI");
        root.setAttribute("xmlns:dc", "http://www.omg.org/spec/DD/20100524/DC");
        root.setAttribute("xmlns:di", "http://www.omg.org/spec/DD/20100524/DI");
        root.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
        root.setAttribute("exporter", "org.eclipse.bpmn2.modeler.core");
        root.setAttribute("exporterVersion", "1.4.3.Final-v20180418-1358-B1");
        root.setAttribute("id", UUID.randomUUID().toString().replace('-', 'x'));
        doc.appendChild(root);

        //		Element cItemDef = doc.createElement("bpmn2:itemDefinition");
        //		cItemDef.setAttribute("id", "ItemDefinition_252");
        //		cItemDef.setAttribute("isCollection", "false");
        //		cItemDef.setAttribute("structureRef", "xs:boolean");
        //		root.appendChild(cItemDef);

        // <bpmn2:process id="Process_1" name="Process 1" isExecutable="false">
        Element eProcess = doc.createElement("bpmn2:process");
        root.appendChild(eProcess);

        // create rest of the model
        model.createXml(eProcess);

        return doc;
    }

    public static void saveInto(XmlModel model, File file) throws Exception {

        if (!file.exists()) {
            createDocument(model, file);
            return;
        }

        Document doc = MXml.loadXml(file);
        Element eProcess = MXml.getElementByPath(doc.getDocumentElement(), "bpmn2:process");
        for (Element child : MXml.getLocalElementIterator(eProcess)) eProcess.removeChild(child);

        model.createXml(eProcess);
        MXml.saveXml(doc.getDocumentElement(), file);
    }

    public static void load(XmlModel model, File file) throws Exception {
        Document doc = MXml.loadXml(file);
        Element eProcess = MXml.getElementByPath(doc.getDocumentElement(), "bpmn2:process");
        model.load(eProcess);
    }
}
