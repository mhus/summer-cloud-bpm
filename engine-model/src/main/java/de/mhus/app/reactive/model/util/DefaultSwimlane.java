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

import de.mhus.app.reactive.model.activity.AActor;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.engine.ContextRecipient;
import de.mhus.app.reactive.model.engine.ProcessContext;

public class DefaultSwimlane implements ASwimlane<APool<?>>, ContextRecipient {

    private Class<? extends AActor> actor;

    @Override
    public void setContext(ProcessContext<?> context) {
        actor = (Class<? extends AActor>) context.getEPool().getPoolDescription().actorDefault();
    }

    @Override
    public Class<? extends AActor> getActor() {
        return actor;
    }
}
