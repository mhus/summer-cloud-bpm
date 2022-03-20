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
package de.mhus.app.reactive.model.errors;

/**
 * Throw this exception to trigger a specified trigger.
 *
 * @author mikehummel
 */
public class TaskException extends Exception {

    private static final long serialVersionUID = 1L;
    private String trigger;
    public static final String DEFAULT_ERROR = "";

    public TaskException(String errorTrigger, String msg) {
        super(errorTrigger + ":" + msg);
        this.trigger = errorTrigger;
    }

    public String getTrigger() {
        return trigger;
    }
}
