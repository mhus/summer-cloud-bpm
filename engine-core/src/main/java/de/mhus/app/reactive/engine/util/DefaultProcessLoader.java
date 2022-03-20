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
package de.mhus.app.reactive.engine.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.activity.AElement;
import de.mhus.app.reactive.model.engine.ProcessLoader;

public class DefaultProcessLoader extends MLog implements ProcessLoader {

    protected LinkedList<URL> classLoaderUrls = new LinkedList<>();
    protected LinkedList<URL> searchLoaderUrls = new LinkedList<>();
    protected LinkedList<Class<? extends AElement<?>>> elementClasses = new LinkedList<>();
    protected URLClassLoader classLoader;
    private String filter;

    public DefaultProcessLoader(File[] dirs) {
        this(dirs, null, null);
    }

    /**
     * Create a new Processor Loader loading from class path
     *
     * @param dirs Class path elements
     * @param search Search in this class path elements for processes (or null for all)
     * @param filter Filter packages or null
     */
    public DefaultProcessLoader(File[] dirs, File[] search, String filter) {
        if (search == null) {
            for (File dir : dirs) {
                load(dir);
                search(dir);
            }
        } else {
            for (File dir : dirs) load(dir);
            for (File dir : search) {
                load(dir);
                search(dir);
            }
        }
        this.filter = filter;
        init();
    }

    @SuppressWarnings("unchecked")
    protected void init() {
        classLoader =
                new URLClassLoader(
                        classLoaderUrls.toArray(new URL[classLoaderUrls.size()]),
                        getClass().getClassLoader());

        LinkedList<String> classNames = new LinkedList<>();
        // load from jar files
        for (URL url : searchLoaderUrls) {
            try {
                File file = new File(url.getFile());
                if (file.isDirectory() && file.getName().equals("classes")) {
                    findClasses(file, classNames, file.getAbsolutePath());
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    JarFile jar = new JarFile(file);
                    for (Enumeration<JarEntry> enu = jar.entries(); enu.hasMoreElements(); ) {
                        JarEntry entry = enu.nextElement();
                        String name = entry.getName();
                        if (MSystem.isWindows())
                            name = name.replace('\\', '/'); // windows compatible
                        if (name.endsWith(".class") && name.indexOf('$') < 0) {
                            if (name.startsWith("/")) name = name.substring(1);
                            String canonicalName =
                                    MString.beforeLastIndex(name, '.').replace('/', '.');
                            if (filter == null || canonicalName.startsWith(filter))
                                classNames.add(canonicalName);
                        }
                    }
                    jar.close();
                }
            } catch (Throwable t) {
                log().w("process loading for {1} failed", url, t);
            }
        }

        // load class and test if it's Element
        for (String name : classNames) {
            try {
                if (MSystem.isWindows()) name = name.replace('\\', '/');
                Class<?> clazz = classLoader.loadClass(name);
                if (AElement.class.isAssignableFrom(clazz))
                    elementClasses.add((Class<? extends AElement<?>>) clazz);
            } catch (Throwable t) {
                log().w(name, t);
            }
        }
    }

    private void findClasses(File dir, LinkedList<String> classNames, String base) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory() && !file.getName().startsWith("."))
                findClasses(file, classNames, base);
            else if (file.isFile() && file.getName().endsWith(".class")) {
                String name = file.getAbsolutePath().substring(base.length());
                if (MSystem.isWindows()) name = name.replace('\\', '/'); // windows compatible
                if (name.startsWith("/")) name = name.substring(1);
                String canonicalName = MString.beforeLastIndex(name, '.').replace('/', '.');
                if (filter == null || canonicalName.startsWith(filter))
                    classNames.add(canonicalName);
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void load(File dir) {
        if (dir.isDirectory() && dir.getName().equals("classes")) {
            try {
                classLoaderUrls.add(dir.toURL());
            } catch (MalformedURLException e) {
                log().w("malformed url {1}", dir, e);
            }
        } else if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".jar"))
                        try {
                            classLoaderUrls.add(file.toURL());
                        } catch (MalformedURLException e) {
                            log().w("malformed url {1}", file, e);
                        }
                }
            }
        } else if (dir.isFile() && dir.getName().endsWith(".jar")) {
            try {
                classLoaderUrls.add(dir.toURL());
            } catch (MalformedURLException e) {
                log().w("malformed url {1}", dir, e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void search(File dir) {
        if (dir.isDirectory() && dir.getName().equals("classes")) {
            try {
                searchLoaderUrls.add(dir.toURL());
            } catch (MalformedURLException e) {
                log().w("malformed url {1}", dir, e);
            }
        } else if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".jar"))
                        try {
                            searchLoaderUrls.add(file.toURL());
                        } catch (MalformedURLException e) {
                            log().w("malformed url {1}", file, e);
                        }
                }
            }
        } else if (dir.isFile() && dir.getName().endsWith(".jar")) {
            try {
                searchLoaderUrls.add(dir.toURL());
            } catch (MalformedURLException e) {
                log().w("malformed url {1}", dir, e);
            }
        }
    }

    @Override
    public List<Class<? extends AElement<?>>> getElements() {
        return Collections.unmodifiableList(elementClasses);
    }
}
