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
package de.mhus.app.reactive.util.bpmn2;

import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.util.MUri;

import de.mhus.app.reactive.model.activity.AEndPoint;
import de.mhus.app.reactive.model.engine.InternalEngine;
import de.mhus.app.reactive.util.activity.RActivity;

/*man bpmn
 * End point that sends a message to the system.
 */
public abstract class RMessageEnd<P extends RPool<?>> extends RActivity<P> implements AEndPoint<P> {

    @Override
    public void doExecuteActivity() throws Exception {
        MProperties parameters = new MProperties();
        String msg = prepareMessage(parameters);
        if (msg == null) return; // ignore and go ahead if msg name is null

        // send
        MUri uri = MUri.toUri("bpmm://" + msg);
        ((InternalEngine) getContext().getEEngine()).execute(uri, parameters);
    }

    /**
     * Prepare the parameters and return the name of the message to send.
     *
     * @param parameters
     * @return the name (not uri but the path of the uri without bpmm://) or null will not send and
     *     go ahead
     */
    protected abstract String prepareMessage(MProperties parameters);
}
