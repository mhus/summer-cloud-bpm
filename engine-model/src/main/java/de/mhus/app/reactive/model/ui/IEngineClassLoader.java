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

public class IEngineClassLoader {

    private static IEngineClassLoader instance;

    public static synchronized IEngineClassLoader instance() {
        if (instance == null) instance = new IEngineClassLoader();
        return instance;
    }

    public static void setInstance(IEngineClassLoader instance) {
        IEngineClassLoader.instance = instance;
    }

    public Class<?> load(String name) throws ClassNotFoundException {
        return this.getClass().getClassLoader().loadClass(name);
    }
}
