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
package de.mhus.app.reactive.model.util;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.summerclouds.common.core.consts.Identifier;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.tool.MString;

import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.engine.CaseLock;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCaseLock;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.ProcessContext;

public class EngineUtil {

    public static void cancelAll(ProcessContext<?> context) throws IOException {
        PCase caze = context.getPCase();
        UUID cazeId = caze.getId();
        for (PNodeInfo info :
                context.getEEngine().storageGetFlowNodes(cazeId, STATE_NODE.WAITING)) {
            try {
                PNode node = context.getCaseLock().getFlowNode(info);
                if (node.getType() == TYPE_NODE.RUNTIME)
                    ((PCaseLock) context.getCaseLock()).closeRuntime(info.getId());
                else
                    ((PCaseLock) context.getCaseLock())
                            .closeFlowNode(null, node, STATE_NODE.CLOSED);
                context.getCaseLock().saveFlowNode(node);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        for (PNodeInfo info :
                context.getEEngine().storageGetFlowNodes(cazeId, STATE_NODE.SCHEDULED)) {
            try {
                PNode node = context.getCaseLock().getFlowNode(info);
                node.setState(STATE_NODE.CLOSED);
                context.getCaseLock().saveFlowNode(node);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public static void start(
            ProcessContext<?> context, Identifier ident, Map<String, ?> runtimeParam)
            throws Exception {
        String name = ident.getClazz().getAnnotation(ActivityDescription.class).name();
        if (MString.isEmpty(name)) name = ident.getClazz().getSimpleName();
        start(context, name, runtimeParam);
    }

    public static void start(ProcessContext<?> context, String name, Map<String, ?> runtimeParam)
            throws Exception {
        EElement point = null;
        for (EElement start : context.getEPool().getStartPoints(false)) {
            if (name.equals(start.getName())) {
                point = start;
                break;
            }
        }
        if (point == null) throw new NotFoundException("Start point not found", name);

        CaseLock lock = context.getCaseLock();
        ((PCaseLock) lock).createStartPoint(context, point, runtimeParam);
    }
}
