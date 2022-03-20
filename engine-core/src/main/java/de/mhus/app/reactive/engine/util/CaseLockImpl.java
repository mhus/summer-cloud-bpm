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

import java.io.Closeable;

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.tool.MTracing;
import org.summerclouds.common.core.tracing.IScope;
import org.summerclouds.common.core.tracing.ISpan;

import de.mhus.app.reactive.model.engine.PCase;

public class CaseLockImpl extends MLog implements Closeable {

    protected IScope scope;
    private String operation;
    private Object[] tagPairs;
    private boolean scopeOwner;
    private boolean master;
    protected ISpan span;

    public CaseLockImpl(boolean master, String operation, Object... tagPairs) {
        this.operation = operation;
        this.tagPairs = tagPairs;
        this.master = master;
    }

    public void startSpan(PCase caze) {
        if (operation == null) {
            scopeOwner = false;
            span = MTracing.current();
            return;
        }
        if (master && !operation.startsWith("global:")) {
            scopeOwner = true;
            scope = spanStart(caze, operation, tagPairs);
        } else {
            scopeOwner = true;
            scope = MTracing.enter(operation, tagPairs);
        }
        span = MTracing.current();
        if (span != null) span.setTag("operation", operation);
    }

    public static IScope spanStart(PCase caze, String operation, Object... tagPairs) {
//        SpanContext ctx =
//                ITracer.get()
//                        .tracer()
//                        .extract(
//                                Format.Builtin.TEXT_MAP,
//                                new TextMap() {
//
//                                    @Override
//                                    public Iterator<Entry<String, String>> iterator() {
//                                        HashMap<String, String> out = new HashMap<>();
//                                        caze.getParameters()
//                                                .forEach(
//                                                        (k, v) -> {
//                                                            if (k.startsWith("__tracer."))
//                                                                out.put(
//                                                                        k.substring(9),
//                                                                        v.toString());
//                                                        });
//                                        return out.entrySet().iterator();
//                                    }
//
//                                    @Override
//                                    public void put(String key, String value) {}
//                                });

        IScope scope = null;
//        if (ctx == null) {
//            Span span = ITracer.get().tracer().buildSpan(operation).start();
//            scope = ITracer.get().activate(span);
//        } else {
//            Span span =
//                    ITracer.get()
//                            .tracer()
//                            .buildSpan(operation)
//                            .addReference(References.FOLLOWS_FROM, ctx)
//                            .start();
//            scope = ITracer.get().activate(span);
//        }
        return scope;
    }

    @Override
    public void close() {
        if (scope != null && scopeOwner) {
            scope.close();
        }
        scope = null;
        span = null;
    }

    public ISpan getSpan() {
        return span;
    }
}
