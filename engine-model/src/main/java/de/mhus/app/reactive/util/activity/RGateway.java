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
package de.mhus.app.reactive.util.activity;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.AGateway;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.util.bpmn2.RPool;

public abstract class RGateway<P extends RPool<?>> extends RActivity<P> implements AGateway<P> {

    @Override
    public void doExecuteActivity() throws Exception {
        Output[] next = doExecute();
        if (next != null) {
            for (Output output : next) {
                Class<? extends AActivity<?>> act = output.activity();
                try {
                    getContext().createActivity(act);
                } catch (Throwable t) {
                    log().w("create activity {1} failed in {2}", act, getContext().getPNode(), t);
                }
            }
            getContext().getPNode().setState(STATE_NODE.CLOSED);
        }
    }

    public abstract Output[] doExecute() throws Exception;
}
