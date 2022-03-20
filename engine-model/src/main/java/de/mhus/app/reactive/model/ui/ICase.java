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
package de.mhus.app.reactive.model.ui;

import java.util.Map;
import java.util.UUID;

import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;

public interface ICase {

    String getUri();

    String getCanonicalName();

    String getCustomId();

    String getCustomerId();

    STATE_CASE getState();

    UUID getId();

    Map<String, String> getProperties();

    long getCreated();

    long getModified();

    int getPriority();

    int getScore();

    String getMilestone();
}
