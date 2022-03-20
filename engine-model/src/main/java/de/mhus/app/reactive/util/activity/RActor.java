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

import de.mhus.app.reactive.model.activity.AActor;
import de.mhus.app.reactive.model.annotations.ActorDescription;
import de.mhus.app.reactive.model.engine.AaaProvider;
import de.mhus.app.reactive.model.engine.ContextRecipient;
import de.mhus.app.reactive.model.engine.ProcessContext;

public class RActor implements AActor, ContextRecipient {

    private ProcessContext<?> context;

    protected ProcessContext<?> getContext() {
        return context;
    }

    @Override
    public void setContext(ProcessContext<?> context) {
        this.context = (ProcessContext<?>) context;
    }

    @Override
    public boolean hasAccess(String user) {
        AaaProvider aaa = context.getAaaProvider();
        if (user == null || !aaa.isUserActive(user)) return false;
        if (aaa.hasAdminAccess(user)) return true;
        if (aaa.hasUserGeneralActorAccess(context.getUri(), getClass().getCanonicalName(), user))
            return true;
        ActorDescription desc = this.getClass().getAnnotation(ActorDescription.class);
        if (desc != null) {
            for (String name : desc.users()) {
                if (user.equals(name)) return true;
            }
            for (String name : desc.groups()) {
                if (aaa.hasGroupAccess(user, name)) return true;
            }
        }
        return false;
    }
}
