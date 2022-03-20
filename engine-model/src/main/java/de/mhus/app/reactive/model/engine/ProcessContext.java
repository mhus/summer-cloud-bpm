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

import java.io.IOException;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.activity.ASwimlane;

public interface ProcessContext<P extends APool<?>> {

    P getPool();

    ASwimlane<P> getSwimlane();

    PNode createActivity(Class<? extends AActivity<?>> next) throws Exception;

    PCase getPCase();

    EPool getEPool();

    PNode getPNode();

    EElement getENode();

    AActivity<?> getANode();

    String getUri();

    EProcess getEProcess();

    PNode getPRuntime();

    RuntimeNode getARuntime();

    void saveRuntime() throws IOException;

    AaaProvider getAaaProvider();

    EEngine getEEngine();

    CaseLock getCaseLock();

    void debug(Object... objects);

    void error(Object... objects);
}
