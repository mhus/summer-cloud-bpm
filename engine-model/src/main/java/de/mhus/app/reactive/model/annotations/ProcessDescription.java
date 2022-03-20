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
package de.mhus.app.reactive.model.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.util.NoPool;

/**
 * A process must be defined with this annotation.
 *
 * @author mikehummel
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ProcessDescription {
    /**
     * Version of the process
     *
     * @return version string
     */
    String version();
    /**
     * Name of the process.
     *
     * @return name
     */
    String name() default "";
    /**
     * Description of the process.
     *
     * @return description
     */
    String description() default "";

    /**
     * If only the process is called to execute. This should define the default pool
     *
     * @return pool
     */
    Class<? extends APool<?>> defaultPool() default NoPool.class;

    /**
     * Set to true of this process can be deployed (if it is not already deployed) and activated.
     *
     * @return true
     */
    boolean autoDeploy() default false;
}
