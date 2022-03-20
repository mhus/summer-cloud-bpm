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
package de.mhus.app.reactive.model.engine;

import java.util.HashMap;
import java.util.Set;

import org.summerclouds.common.core.error.MException;

import de.mhus.app.reactive.model.activity.AActor;
import de.mhus.app.reactive.model.activity.AElement;
import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.SubDescription;
import de.mhus.app.reactive.model.annotations.Trigger;

public interface EElement {

    //	Class<? extends AElement<?>> getElementClass();

    String getCanonicalName();

    @SuppressWarnings("rawtypes")
    boolean is(Class<? extends AElement> ifc);

    Output[] getOutputs();

    Trigger[] getTriggers();

    Class<? extends ASwimlane<?>> getSwimlane();

    String getName();

    boolean isInterface(Class<?> ifc);

    ActivityDescription getActivityDescription();

    HashMap<String, Long> getSchedulerList();

    HashMap<String, String> getSignalList();

    HashMap<String, String> getMessageList();

    Class<? extends AActor> getAssignedActor(EPool pool);

    SubDescription getSubDescription();

    AElement<?> newInstance() throws MException;

    Set<EAttribute> getAttributes();
}
