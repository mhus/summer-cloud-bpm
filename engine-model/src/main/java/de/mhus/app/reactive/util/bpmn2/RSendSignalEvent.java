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

import de.mhus.app.reactive.model.activity.ASender;
import de.mhus.app.reactive.model.engine.InternalEngine;
import de.mhus.app.reactive.util.activity.RTask;

/*man bpmn
 * Send a signal and step to the next activity.
 */
public abstract class RSendSignalEvent<P extends RPool<?>> extends RTask<P> implements ASender<P> {

    @Override
    public String doExecute() throws Exception {
        MProperties parameters = new MProperties();
        String msg = prepareSignal(parameters);
        if (msg == null) return null; // ignore and go ahead if msg name is null

        // send
        MUri uri = MUri.toUri("bpms://" + msg);
        ((InternalEngine) getContext().getEEngine()).execute(uri, parameters);

        return null;
    }

    /**
     * Prepare the parameters and return the name of the signal to send.
     *
     * @param parameters
     * @return the name (not uri but the path of the uri without bpms://) or null will not send and
     *     go ahead
     */
    protected abstract String prepareSignal(MProperties parameters);
}
