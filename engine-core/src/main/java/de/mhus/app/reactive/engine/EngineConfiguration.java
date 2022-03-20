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
package de.mhus.app.reactive.engine;

import java.util.LinkedList;
import java.util.Map;

import org.summerclouds.common.core.tool.MPeriod;

import de.mhus.app.reactive.model.engine.AaaProvider;
import de.mhus.app.reactive.model.engine.CaseLockProvider;
import de.mhus.app.reactive.model.engine.EngineListener;
import de.mhus.app.reactive.model.engine.PEngine;
import de.mhus.app.reactive.model.engine.ProcessProvider;
import de.mhus.app.reactive.model.engine.StorageProvider;

public class EngineConfiguration {

    public StorageProvider storage;
    public StorageProvider archive;
    public ProcessProvider processProvider;
    public AaaProvider aaa;
    public Map<String, String> parameters;
    public LinkedList<EngineListener> listener = new LinkedList<>();
    public PEngine persistent;
    public CaseLockProvider lockProvider;
    public boolean executeParallel = true;
    public int maxThreads = 10;
    public long sleepBetweenProgress = 100;
    public long progressTimeout = MPeriod.MINUTE_IN_MILLISECONDS * 5;
}
