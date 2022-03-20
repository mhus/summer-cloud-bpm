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

import java.util.List;
import java.util.Set;

import org.summerclouds.common.core.error.MException;

import de.mhus.app.reactive.model.activity.AElement;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.annotations.PoolDescription;

public interface EPool {

    List<EElement> getStartPoints(boolean activeOnly);

    //	Class<? extends APool<?>> getPoolClass();

    EElement getElement(String name);

    Set<String> getElementNames();

    List<EElement> getElements(Class<? extends AElement<?>> ifc);

    String getCanonicalName();

    List<EElement> getOutputElements(EElement element);

    String getName();

    boolean isElementOfPool(EElement element);

    PoolDescription getPoolDescription();

    APool<?> newInstance() throws MException;

    Set<EAttribute> getAttributes();
}
