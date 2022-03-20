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

import org.summerclouds.common.core.form.IFormInformation;

import de.mhus.app.reactive.model.activity.AActor;
import de.mhus.app.reactive.model.util.EverybodyActor;
import de.mhus.app.reactive.model.util.NoForm;

/**
 * You must descripe a pool with this annotation.
 *
 * @author mikehummel
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PoolDescription {

    /**
     * Technical name of the pool. By default the canonical name of the class is used.
     *
     * @return name
     */
    String name() default "";
    /**
     * The description. Can be overwritten by i18n.
     *
     * @return description
     */
    String description() default "";
    /**
     * The displayed name. Can be overwritten by i18n.
     *
     * @return displayed name
     */
    String displayName() default "";
    /**
     * Set displayed names for the indexes (max 10). This can be overwritten by i18n.
     *
     * @return Index names
     */
    String[] indexDisplayNames() default {};

    /**
     * The default actor for everything.
     *
     * @return default actor
     */
    Class<? extends AActor> actorDefault() default EverybodyActor.class;
    /**
     * Actors are able to start a new process. By default everyone!
     *
     * @return actors
     */
    Class<? extends AActor>[] actorInitiator() default EverybodyActor.class;
    /**
     * Actors are able to read the process data via GUI.
     *
     * @return actors
     */
    Class<? extends AActor>[] actorRead() default {};
    /**
     * Actors are able to read and manipulate the process data via GUI.
     *
     * @return actors
     */
    Class<? extends AActor>[] actorWrite() default {};

    ExternalSource[] external() default {};

    /**
     * Define the form provider to create the start form of the process.
     *
     * @return initial form provider
     */
    Class<? extends IFormInformation> initialForm() default NoForm.class;

    /**
     * Define the form provider to display the running case. This form is read only for read only
     * actors and writable for write actors.
     *
     * @return display form provider
     */
    Class<? extends IFormInformation> displayForm() default NoForm.class;
}
