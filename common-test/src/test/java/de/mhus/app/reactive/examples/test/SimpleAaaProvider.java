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
package de.mhus.app.reactive.examples.test;

import de.mhus.app.reactive.model.engine.AaaProvider;

public class SimpleAaaProvider implements AaaProvider {

    @Override
    public String getCurrentUserId() {
        return "me";
    }

    @Override
    public boolean hasAdminAccess(String user) {
        return false;
    }

    @Override
    public boolean hasGroupAccess(String user, String group) {
        return true;
    }

    @Override
    public boolean validatePassword(String user, String pass) {
        return true;
    }

    @Override
    public boolean isUserActive(String user) {
        return true;
    }

    @Override
    public boolean hasUserGeneralActorAccess(String uri, String canonicalName, String user) {
        return true;
    }
}
