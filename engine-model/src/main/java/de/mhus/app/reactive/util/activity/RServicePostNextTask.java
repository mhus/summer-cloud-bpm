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
import de.mhus.app.reactive.model.activity.AServiceTask;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.errors.EngineException;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.util.bpmn2.RPool;
/**
 * This task is used if you need to know the id of the next created activity in execution time. If
 * you use this, the next activity will already be created and maybe executed. The main scenario to
 * use this is to execute a ExternalEvent as next task and grab the id before.
 *
 * <p>You can not deny execution of the next node e.g. be error.
 *
 * @author mikehummel
 * @param <P>
 */
public abstract class RServicePostNextTask<P extends RPool<?>> extends RTask<P>
        implements AServiceTask<P> {

    @Override
    public void doExecuteActivity() throws Exception {
        String nextName = doExecute();
        if (nextName == null) nextName = DEFAULT_OUTPUT;
        if (!nextName.equals(RETRY)) {
            Class<? extends AActivity<?>> next = ActivityUtil.getOutputByName(this, nextName);
            if (next == null)
                throw new EngineException(
                        "Output Activity not found: "
                                + nextName
                                + " in "
                                + getClass().getCanonicalName());
            PNode nextPNode = getContext().createActivity(next);

            doExecute(nextPNode);

            getContext().getPNode().setState(STATE_NODE.CLOSED);
        }
    }

    @Override
    public String doExecute() throws Exception {
        return null;
    }

    /**
     * Executed after the next activity is created.
     *
     * @param nextPNode
     * @throws Exception
     */
    public abstract void doExecute(PNode nextPNode) throws Exception;
}
