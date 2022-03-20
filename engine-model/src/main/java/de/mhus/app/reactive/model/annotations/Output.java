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

/** Define an output in ActivityDescription outputs list. */
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.ACondition;
import de.mhus.app.reactive.model.util.NoCondition;

@Retention(RetentionPolicy.RUNTIME)
public @interface Output {
    /**
     * Mapped name, to define the default output set to an empty string (default). Only one default
     * output should exist.
     *
     * @return name
     */
    String name() default "";
    /**
     * Description
     *
     * @return description
     */
    String description() default "";
    /**
     * In case of gateways a condition can be defined.
     *
     * @return condition type
     */
    Class<? extends ACondition<?>> condition() default NoCondition.class;
    /**
     * The next linked activity to follow if using this output.
     *
     * @return next activity
     */
    Class<? extends AActivity<?>> activity();
}
