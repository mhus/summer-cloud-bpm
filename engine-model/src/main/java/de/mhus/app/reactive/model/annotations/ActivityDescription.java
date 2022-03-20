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

import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.engine.RuntimeNode;
import de.mhus.app.reactive.model.util.DefaultSwimlane;

/**
 * Use this annotation to describe every Activity.
 *
 * @author mikehummel
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivityDescription {

    /**
     * Define a list of outputs in case of start or parallel gateway the will be executed in
     * parallel otherwise one of the is selected to execute. Set a default (without name) output at
     * least.
     *
     * @return List of outputs
     */
    public Output[] outputs() default {};

    /**
     * Define a list of triggers / events for this activity. E.g. error handlers, signals or timer.
     *
     * @return List of triggers
     */
    public Trigger[] triggers() default {};

    /**
     * Define the swim lane if it's not the default swim lane.
     *
     * @return The swim lane
     */
    public Class<? extends ASwimlane<?>> lane() default DefaultSwimlane.class;

    /**
     * Set default description for the activity. This can be overwritten by i18n.
     *
     * @return The description
     */
    String description() default "";

    /**
     * Set the displayed name. This can be overwritten by i18n.
     *
     * @return The displayed name
     */
    String displayName() default "";

    /**
     * Set displayed names for the indexes (max 10). This can be overwritten by i18n.
     *
     * @return Index names
     */
    String[] indexDisplayNames() default {};

    /**
     * If a special runtime node type is needed, define here. For start points only.
     *
     * @return runtime node type.
     */
    public Class<? extends RuntimeNode> runtime() default RuntimeNode.class;

    /**
     * Define the technical name of the node. By default the canonical name of the class is used.
     *
     * @return Technical name
     */
    public String name() default "";

    /**
     * Define a event. This is used for even driven activities like signal or message receivers.
     * Also the scheduled time or retry time is defined as event.
     *
     * @return The event definition
     */
    String event() default "";
}
