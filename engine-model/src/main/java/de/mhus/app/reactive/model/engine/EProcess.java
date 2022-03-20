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
import de.mhus.app.reactive.model.activity.AProcess;
import de.mhus.app.reactive.model.annotations.ProcessDescription;

public interface EProcess {

    /**
     * Return the simple name of the process.
     *
     * @return Simple name
     */
    String getName();

    String getVersion();

    List<Class<? extends AElement<?>>> getElements();

    EPool getPool(String name);

    EElement getElement(String name);

    Set<String> getPoolNames();

    Set<String> getElementNames();

    ProcessDescription getProcessDescription();

    /**
     * Return processName : processVersion
     *
     * @return The unique name processName : processVersion
     */
    String getProcessName();

    Class<? extends AProcess> getProcessClass();

    /**
     * Return the canonical name of the process class only.
     *
     * @return canonical class name
     */
    String getCanonicalName();

    AProcess newInstance() throws MException;
}
