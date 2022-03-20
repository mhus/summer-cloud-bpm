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
package de.mhus.app.reactive.examples.simple1.area;

import org.summerclouds.common.core.tool.MThread;

import de.mhus.app.reactive.examples.simple1.S1Pool;
import de.mhus.app.reactive.examples.simple1.S1TheEnd;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.util.activity.RTask;

@ActivityDescription(
        outputs = {
            @Output(activity = S3LeaveArea.class),
            @Output(name = "abord", activity = S1TheEnd.class),
        })
public class S2DoSomething extends RTask<S1Pool> {

    private static boolean inProgress = false;
    public static boolean failed = false;
    public static long sleepTime = 1000;

    @Override
    public String doExecute() throws Exception {
        if (inProgress) {
            failed = true;
            throw new Exception("That's not fair .... I'm not alone!");
        }
        inProgress = true;
        System.out.println("It's only me ... doing something");
        MThread.sleep(sleepTime);
        inProgress = false;
        return "abord".equals(getPool().getText2()) ? "abord" : null;
    }
}
