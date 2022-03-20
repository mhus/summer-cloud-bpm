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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.summerclouds.common.core.tool.MString;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.ACondition;
import de.mhus.app.reactive.model.activity.AEndPoint;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.activity.AStartPoint;
import de.mhus.app.reactive.model.activity.ASubProcess;
import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.SubDescription;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.annotations.Trigger.TYPE;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.util.DefaultSwimlane;

public class PoolValidator {

    private LinkedList<Finding> findings = new LinkedList<>();
    private EPool pool;

    public PoolValidator(EPool pool) {
        this.pool = pool;
    }

    public void validate() {

        for (String name : pool.getElementNames()) {
            validateElement(name);
        }
    }

    private void validateElement(String name) {
        EElement elem = pool.getElement(name);
        if (elem == null) {
            findings.add(new Finding(LEVEL.FATAL, name, "not found")); // should not happen
            return;
        }

        // test object instantiation
        try {
            elem.newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            findings.add(new Finding(LEVEL.FATAL, name, "Can't initialize: " + e.toString()));
            return;
        }

        // ignore conditions
        if (elem.is(ACondition.class)) return;

        if (elem.is(ASwimlane.class)) {
            validateSwimlane(name, elem);
            return;
        }
        if (elem.is(APool.class)) {
            validatePool(name, elem);
            return;
        }
        if (elem.is(AActivity.class)) {
            validateActivity(name, elem);
            return;
        }
        findings.add(new Finding(LEVEL.WARN, name, "unknown type"));
    }

    private void validateActivity(String name, EElement elem) {

        // test sub process
        if (elem.is(ASubProcess.class)) {
            SubDescription sub = elem.getSubDescription();
            if (sub == null)
                findings.add(new Finding(LEVEL.ERROR, name, "sub process without SubDescription"));
            else {
                if (MString.isEmpty(sub.uri()) && sub.start().isInterface())
                    findings.add(
                            new Finding(
                                    LEVEL.ERROR, name, "sub process with empty SubDescription"));
            }
        }

        // test outgoing
        if (elem.is(AActivity.class) && !elem.is(AEndPoint.class)) {
            Output[] outputs = elem.getOutputs();
            if (outputs.length == 0)
                findings.add(new Finding(LEVEL.WARN, name, "task without following activity"));
            for (Output output : outputs)
                if (pool.getElement(output.activity().getCanonicalName()) == null) {
                    findings.add(
                            new Finding(
                                    LEVEL.FATAL,
                                    name,
                                    "output not found in pool: "
                                            + output.activity().getCanonicalName()));
                }
        }
        // test previous
        if (!elem.is(AStartPoint.class)) {
            boolean foundPrev = false;
            for (String prevName : pool.getElementNames()) {
                EElement prev = pool.getElement(prevName);
                for (Output output : prev.getOutputs())
                    if (output.activity().getCanonicalName().equals(name)) {
                        foundPrev = true;
                        break;
                    }
            }
            if (!foundPrev)
                findings.add(new Finding(LEVEL.WARN, name, "task without previous activity"));
        }
        // test triggers
        boolean defaultErrorTrigger = false;
        boolean professionalErrorTrigger = false;
        boolean timerTrigger = false;
        for (Trigger trigger : elem.getTriggers()) {
            if (trigger.type() == TYPE.PROFESSIONAL_ERROR) {
                if (professionalErrorTrigger)
                    findings.add(
                            new Finding(
                                    LEVEL.FATAL,
                                    name,
                                    "task with more then one professional error trigger"));
                else professionalErrorTrigger = true;
            } else
            if (trigger.type() == TYPE.DEFAULT_ERROR) {
                if (defaultErrorTrigger)
                    findings.add(
                            new Finding(
                                    LEVEL.FATAL,
                                    name,
                                    "task with more then one default error trigger"));
                else defaultErrorTrigger = true;
            } else if (trigger.type() == TYPE.NOOP)
                findings.add(new Finding(LEVEL.TRIVIAL, name, "task without NOOP trigger"));
            else if (trigger.type() == TYPE.TIMER) {
                if (timerTrigger)
                    findings.add(
                            new Finding(
                                    LEVEL.WARN, name, "task without more then one timer trigger"));
                else timerTrigger = true;
                long next = EngineUtil.getNextScheduledTime(trigger.event());
                if (next < 0)
                    findings.add(
                            new Finding(
                                    LEVEL.FATAL,
                                    name,
                                    "timer trigger with invalid time interval definition"));
            }
        }
        // test lane
        Class<? extends ASwimlane<?>> lane = elem.getSwimlane();
        if (lane == null) {
            findings.add(new Finding(LEVEL.FATAL, name, "Activity without lane"));
        } else if (lane == DefaultSwimlane.class) {
            // nothing
        } else {
            EElement laneElem = pool.getElement(lane.getCanonicalName());
            if (laneElem == null)
                findings.add(
                        new Finding(
                                LEVEL.FATAL,
                                name,
                                "Swimlane not found in pool: " + lane.getCanonicalName()));
        }
    }

    private void validatePool(String name, EElement elem) {
        // TODO Auto-generated method stub

    }

    private void validateSwimlane(String name, EElement elem) {
        // TODO Auto-generated method stub

    }

    public enum LEVEL {
        TRIVIAL,
        WARN,
        ERROR,
        FATAL
    }

    public static class Finding {

        private LEVEL level;
        private String name;
        private String msg;

        public Finding(LEVEL level, String name, String msg) {
            this.level = level;
            this.name = name;
            this.msg = msg;
        }

        @Override
        public String toString() {
            return level.name() + " " + name + ": " + msg;
        }

        public LEVEL getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }

        public String getMsg() {
            return msg;
        }
    }

    public List<Finding> getFindings() {
        return Collections.unmodifiableList(findings);
    }

    public void clearFindings() {
        findings.clear();
    }
}
