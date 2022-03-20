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

/**
 * If you define new actors you must define it with this annotation.
 *
 * @author mikehummel
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ActorDescription {
    /**
     * Name of the actor.
     *
     * @return name
     */
    String name() default "";
    /**
     * Description for the actor.
     *
     * @return description
     */
    String description() default "";
    /**
     * Allowed group mapping
     *
     * @return groups
     */
    String[] groups() default {};
    /**
     * Allowed user mapping
     *
     * @return users
     */
    String[] users() default {};
}
