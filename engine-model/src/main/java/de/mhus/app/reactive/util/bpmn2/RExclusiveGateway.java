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

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.RC;

import de.mhus.app.reactive.model.activity.ACondition;
import de.mhus.app.reactive.model.activity.AExclusiveGateway;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.model.util.NoCondition;
import de.mhus.app.reactive.util.activity.RGateway;

/*man bpmn
 * Decide between outputs using the Output Condition Define one Output without Condition to set it
 * as default Output Define ACondition(s) to the Output(s) for the condition. RExclusiveGateway will
 * follow the Output with the highest result. Or the first with the same result For binary decisions
 * use the constants TRUE and FALSE
 */
public class RExclusiveGateway<P extends RPool<?>> extends RGateway<P>
        implements AExclusiveGateway<P> {

    @SuppressWarnings("unchecked")
    @Override
    public Output[] doExecute() throws Exception {
        Output current = null;
        Output defaultOutput = null;
        int currentRes = ACondition.FALSE;
        for (Output output : ActivityUtil.getOutputs(this)) {
            if (output.condition() == NoCondition.class) defaultOutput = output;
            else {
                Class<? extends ACondition<P>> condition =
                        (Class<? extends ACondition<P>>) output.condition();
                int res = condition.getDeclaredConstructor().newInstance().check(getContext());
                if (res >= 0 && res > currentRes) {
                    currentRes = res;
                    current = output;
                }
            }
        }
        if (current == null) current = defaultOutput;
        if (current == null)
            throw new MException(RC.ERROR, "condition not found in {1}", getClass().getCanonicalName());
        return new Output[] {current};
    }
}
