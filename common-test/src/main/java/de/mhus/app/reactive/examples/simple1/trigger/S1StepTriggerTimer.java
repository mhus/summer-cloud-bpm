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
package de.mhus.app.reactive.examples.simple1.trigger;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.form.DefRoot;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;

import de.mhus.app.reactive.examples.simple1.S1ActorSpecialist;
import de.mhus.app.reactive.examples.simple1.S1Pool;
import de.mhus.app.reactive.examples.simple1.S1Terminate2;
import de.mhus.app.reactive.examples.simple1.S1TheEnd;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.ActorAssign;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.annotations.Trigger.TYPE;
import de.mhus.app.reactive.util.bpmn2.RUserTask;

@ActivityDescription(
        outputs = @Output(activity = S1TheEnd.class),
        triggers = {
            @Trigger(type = TYPE.TIMER, event = "1s", activity = S1Terminate2.class),
        })
@ActorAssign(S1ActorSpecialist.class)
public class S1StepTriggerTimer extends RUserTask<S1Pool> {

    @Override
    public DefRoot getForm() {
        return null;
    }

    @Override
    public String[] createIndexValues(boolean init) {
        return null;
    }

    @Override
    protected void doSubmit() throws MException {
        // TODO Auto-generated method stub

    }

    @Override
    public MProperties doAction(String action, IProperties values) {
        // TODO Auto-generated method stub
        return null;
    }
}
