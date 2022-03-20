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

@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyDescription {

    String name() default "";

    String description() default "";

    String displayName() default "";
    /**
     * Set writable by forms.
     *
     * @return true
     */
    boolean writable() default true;
    /**
     * Set readable by forms.
     *
     * @return true
     */
    boolean readable() default true;
    /**
     * False if value is not stored by default.
     *
     * @return true
     */
    boolean persistent() default true;
    /**
     * Set true if the parameter can be set creating the case.
     *
     * @return true
     */
    boolean initial() default false;

    /**
     * Set true if the parameter must be set at start.
     *
     * @return true
     */
    boolean mandatory() default false;
}
