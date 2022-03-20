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
package de.mhus.app.reactive.examples.simple1;

import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.ActorAssign;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.util.bpmn2.RServiceTask;

@ActivityDescription(
        outputs = @Output(name = S1Step3.END, activity = S1TheEnd.class),
        lane = S1Lane1.class)
@ActorAssign(S1ActorSpecialist.class)
public class S1Step3 extends RServiceTask<S1Pool> {

    public static final String END = "end";

    @Override
    public String doExecute() {
        return END;
    }
}
