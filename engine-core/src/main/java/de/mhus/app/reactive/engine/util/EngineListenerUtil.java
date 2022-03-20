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
import java.io.FileWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.UUID;

import org.summerclouds.common.core.console.Console;
import org.summerclouds.common.core.console.Console.COLOR;
import org.summerclouds.common.core.error.DummyException;
import org.summerclouds.common.core.log.Log;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MDate;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.engine.Engine;
import de.mhus.app.reactive.engine.EngineConfiguration;
import de.mhus.app.reactive.model.engine.EngineListener;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCaseLock;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.RuntimeNode;

public class EngineListenerUtil {

    public static EngineListener createStdErrListener() {
        return (EngineListener)
                Proxy.newProxyInstance(
                        EngineListener.class.getClassLoader(),
                        new Class[] {EngineListener.class},
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                System.err.println(MSystem.toString(method.getName(), args));
                                return null;
                            }
                        });
    }

    public static EngineListener createStdOutListener() {
        return (EngineListener)
                Proxy.newProxyInstance(
                        EngineListener.class.getClassLoader(),
                        new Class[] {EngineListener.class},
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                System.out.println(MSystem.toString(method.getName(), args));
                                return null;
                            }
                        });
    }

    public static EngineListener createAnsiListener() {
        return (EngineListener)
                Proxy.newProxyInstance(
                        EngineListener.class.getClassLoader(),
                        new Class[] {EngineListener.class},
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                if (Console.get().isAnsi())
                                    Console.get().setColor(COLOR.MAGENTA, null);
                                System.out.println(MSystem.toString(method.getName(), args));
                                if (Console.get().isAnsi()) Console.get().cleanup();
                                return null;
                            }
                        });
    }

    public static EngineListener createQuietListener() {
        return (EngineListener)
                Proxy.newProxyInstance(
                        EngineListener.class.getClassLoader(),
                        new Class[] {EngineListener.class},
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                return null;
                            }
                        });
    }

    public static EngineListener createLogDebugListener(final LogConfiguration config) {
        return (EngineListener)
                Proxy.newProxyInstance(
                        EngineListener.class.getClassLoader(),
                        new Class[] {EngineListener.class},
                        new InvocationHandler() {
                            Log log = Log.getLog(Engine.class);

                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                doCaseTrace(config, method, args);
                                if (method.getName().equals("doStep")
                                        && config != null
                                        && !config.traceSteps) return null;
                                log.d(method.getName(), args);

                                if (config != null && config.stackTrace) {
                                    log.d(
                                            new DummyException(
                                                    null, Thread.currentThread().getStackTrace()));
                                }
                                return null;
                            }
                        });
    }

    protected static void doCaseTrace(LogConfiguration config, Method method, Object[] args) {
        if (args == null
                || config == null
                || !config.traceCases
                || args.length < 1
                || args[0] == null) return;

        UUID caseId = null;

        if (args[0] instanceof PCase) caseId = ((PCase) args[0]).getId();
        else if (args[0] instanceof PNode) caseId = ((PNode) args[0]).getCaseId();
        if (args[0] instanceof PCaseLock) caseId = ((PCaseLock) args[0]).getCaseId();

        if (caseId == null) return;

        File f = new File(config.traceDir, "case_" + caseId + ".log");
        try (FileWriter fw = new FileWriter(f, true)) {
            fw.append(MDate.toIsoDateTime(System.currentTimeMillis()))
                    .append(' ')
                    .append(caseId.toString())
                    .append(' ');
            fw.append(method.getName()).append(" [");
            for (int i = 0; i < args.length; i++) {
                if (i != 0) fw.append("][");
                fw.append(String.valueOf(args[i]));
            }
            fw.append("]\n");
            fw.append(MCast.toString(null, Thread.currentThread().getStackTrace()));
        } catch (Throwable t) {
        }
    }

    public static EngineListener createLogInfoListener(final LogConfiguration config) {
        return (EngineListener)
                Proxy.newProxyInstance(
                        EngineListener.class.getClassLoader(),
                        new Class[] {EngineListener.class},
                        new InvocationHandler() {
                            Log log = Log.getLog(Engine.class);

                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {
                                doCaseTrace(config, method, args);
                                if (method.getName().equals("doStep")
                                        && config != null
                                        && !config.traceSteps) return null;
                                log.i(method.getName(), args);

                                if (config != null && config.stackTrace) {
                                    log.i(
                                            new DummyException(
                                                    null, Thread.currentThread().getStackTrace()));
                                }
                                return null;
                            }
                        });
    }

    public static EngineListener createEngineEventProcessor(final EngineConfiguration config) {
        return (EngineListener)
                Proxy.newProxyInstance(
                        EngineListener.class.getClassLoader(),
                        new Class[] {EngineListener.class},
                        new InvocationHandler() {
                            //			Log log = Log.getLog(Engine.class);
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                    throws Throwable {

                                // invoke runtime is necessary
                                if (args != null
                                        && args.length >= 2
                                        && args[0] != null
                                        && args[0] instanceof RuntimeNode
                                        && args[1] != null
                                        && args[1] instanceof PNode) {
                                    try {
                                        ((RuntimeNode) args[0])
                                                .doEvent(
                                                        method.getName(), (PNode) args[1], 2, args);
                                    } catch (Throwable t) {
                                    	Log.getLog(EngineListenerUtil.class).e("do event {1} failed", method, args, t);
                                    }
                                }

                                // invoke cascaded listeners
                                LinkedList<EngineListener> l = config.listener;
                                if (l != null)
                                    for (EngineListener listener : l) {
                                        try {
                                            method.invoke(listener, args);
                                        } catch (Throwable t) {
                                        	Log.getLog(EngineListenerUtil.class).t("invoke listener failed", method, args, t);
                                        }
                                    }
                                return null;
                            }
                        });
    }
}
