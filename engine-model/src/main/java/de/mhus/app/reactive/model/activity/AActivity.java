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
package de.mhus.app.reactive.model.activity;

import java.util.Map;

import de.mhus.app.reactive.model.engine.ProcessContext;

/**
 * The basic interface for all activities in a pool. Activity instances are stored in database. This
 * means between calling different methods the instance itself can change (stored and recreated).
 *
 * @author mikehummel
 * @param <P>
 */
public interface AActivity<P extends APool<?>> extends AElement<P> {

    String DEFAULT_OUTPUT = "";
    String NO = "no";
    String YES = "yes";

    /**
     * Return the current context for the running activity.
     *
     * @return runtime context
     */
    ProcessContext<P> getContext();

    /**
     * Helper to get the pool from context.
     *
     * @return my pool
     */
    default P getPool() {
        return getContext().getPool();
    }

    /**
     * Is called if the activity is created.
     *
     * @throws Exception
     */
    void initializeActivity() throws Exception;

    /**
     * Is called to execute the activity. Could be more then one time.
     *
     * @throws Exception
     */
    void doExecuteActivity() throws Exception;

    /**
     * Serialize the parameters to be stored in database.
     *
     * @return serialized (serializable values) map.
     */
    Map<String, Object> exportParamters();

    /**
     * Import parameters from database.
     *
     * @param parameters
     */
    void importParameters(Map<String, Object> parameters);
}
