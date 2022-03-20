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
package de.mhus.app.reactive.examples.demo.v1_0_0;

import java.util.Map;

import de.mhus.app.reactive.examples.simple1.S1ActorManager;
import de.mhus.app.reactive.examples.simple1.S1ActorWorker;
import de.mhus.app.reactive.model.annotations.PoolDescription;
import de.mhus.app.reactive.util.bpmn2.RPool;

@PoolDescription(
        displayName = "Demo Pool",
        description = "This pool is a demo",
        actorRead = S1ActorWorker.class,
        actorInitiator = S1ActorManager.class)
public class DemoPool extends RPool<DemoPool> {

    @Override
    public String[] createIndexValues(boolean init) {
        return null;
    }

    @Override
    protected void checkInputParameters(Map<String, Object> parameters) throws Exception {}
}
