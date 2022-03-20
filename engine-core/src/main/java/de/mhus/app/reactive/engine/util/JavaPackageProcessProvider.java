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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.core.pojo.PojoModel;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.AActor;
import de.mhus.app.reactive.model.activity.AElement;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.activity.AProcess;
import de.mhus.app.reactive.model.activity.AStartPoint;
import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.ActorAssign;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.PoolDescription;
import de.mhus.app.reactive.model.annotations.ProcessDescription;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.model.annotations.SubDescription;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.annotations.Trigger.TYPE;
import de.mhus.app.reactive.model.engine.EAttribute;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.model.engine.ProcessLoader;
import de.mhus.app.reactive.model.engine.ProcessProvider;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.model.util.DefaultSwimlane;
import de.mhus.app.reactive.model.util.InactiveStartPoint;
import de.mhus.app.reactive.model.util.NobodyActor;

public class JavaPackageProcessProvider extends MLog implements ProcessProvider {

    protected HashMap<String, ProcessContainer> processes = new HashMap<>();
    protected LinkedList<String> warnings = new LinkedList<>();

    public List<String> addAllProcesses(ProcessLoader loader) throws MException {
        ArrayList<String> out = new ArrayList<>();

        for (Class<? extends AElement<?>> element : loader.getElements()) {
            if (!element.isInterface()
                    && de.mhus.app.reactive.model.activity.AProcess.class.isAssignableFrom(
                            element)) {
                String name = addProcess(loader, element.getPackageName());
                out.add(name);
            }
        }
        return out;
    }

    public String addProcess(ProcessLoader loader, String processPackage) throws MException {
        ProcessContainer container = new ProcessContainer(loader, processPackage);
        String name = container.getCanonicalName() + ":" + container.getVersion();
        if (processes.containsKey(name))
            log().w("Process already defined, overwrite", container.getProcessName());
        processes.put(name, container);
        return name;
    }

    public void removeProcess(String name) {
        processes.remove(name);
    }

    @Override
    public EProcess getProcess(String name, String version) {
        return processes.get(name + ":" + version);
    }

    @Override
    public EProcess getProcess(String nameVerion) {
        return processes.get(nameVerion);
    }

    public class ProcessContainer implements EProcess {

        private ProcessLoader loader;
        private String canonicalName;
        private Class<? extends AProcess> processClass;
        private ProcessDescription processDescription;
        private String processName;
        private HashMap<String, EPool> pools = new HashMap<>();
        private HashMap<String, EElement> elements = new HashMap<>();
        private String name;

        @SuppressWarnings("unchecked")
        public ProcessContainer(ProcessLoader loader, String processPackage) throws MException {
            this.loader = loader;
            // iterate all elements
            for (Class<? extends AElement<?>> element : loader.getElements()) {

                if (processPackage != null
                        && !element.getPackageName().startsWith(processPackage + ".")
                        && !element.getPackageName().equals(processPackage)) continue;

                // find the process description
                if (!element.isInterface()
                        && de.mhus.app.reactive.model.activity.AProcess.class.isAssignableFrom(
                                element)) {
                    if (processClass != null)
                        throw new MException(RC.ERROR,
                                "Multipe process definition classes found", processClass, element);
                    processClass = (Class<? extends AProcess>) element;
                }

                // find the pool descriptions
                if (!element.isInterface() && APool.class.isAssignableFrom(element)) {
                    try {
                        PoolContainer pool = new PoolContainer((Class<? extends APool<?>>) element);
                        if (pools.containsKey(pool.getCanonicalName()))
                            throw new MException(RC.ERROR,
                                    "Multiple pools with the same name", pool.getCanonicalName());
                        pool.setProcess(this);
                        pools.put(pool.getCanonicalName(), pool);
                    } catch (Throwable t) {
                        log().w("loading pool {1} failed", element, t);
                        warnings.add("Pool " + element.getCanonicalName() + ": " + t.getMessage());
                    }
                }

                // find all activities
                if (!element.isInterface() && AElement.class.isAssignableFrom(element)) {
                    try {
                        ElementContainer act =
                                new ElementContainer((Class<? extends AActivity<?>>) element);
                        if (elements.containsKey(act.getCanonicalName()))
                            throw new MException(RC.ERROR,
                                    "Multiple activities with the same name",
                                    act.getCanonicalName()); // should not happen
                        elements.put(act.getCanonicalName(), act);
                    } catch (Throwable t) {
                        log().w("loading element {1} failed", element, t);
                        warnings.add("Pool " + element.getCanonicalName() + ": " + t.getMessage());
                    }
                }
            }
            if (processClass == null) throw new MException(RC.ERROR, "process definition class not found");
            processDescription = processClass.getAnnotation(ProcessDescription.class);
            if (processDescription == null)
                throw new MException(RC.ERROR, "process definition annotation not found");
            processName = processClass.getCanonicalName();
            name =
                    MString.isEmpty(processDescription.name())
                            ? processClass.getSimpleName()
                            : processDescription.name();
            canonicalName = processClass.getCanonicalName() + ":" + processDescription.version();

            // init pools
            for (EPool pool : pools.values()) ((PoolContainer) pool).collectElements();
        }

        @Override
        public String getProcessName() {
            return canonicalName;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getCanonicalName() {
            return processName;
        }

        @Override
        public String getVersion() {
            return processDescription.version();
        }

        @Override
        public List<Class<? extends AElement<?>>> getElements() {
            return loader.getElements();
        }

        @Override
        public String toString() {
            return canonicalName;
        }

        @Override
        public EPool getPool(String name) {
            return pools.get(name);
        }

        @Override
        public Set<String> getPoolNames() {
            return pools.keySet();
        }

        @Override
        public EElement getElement(String name) {
            return elements.get(name);
        }

        @Override
        public Set<String> getElementNames() {
            return elements.keySet();
        }

        @Override
        public ProcessDescription getProcessDescription() {
            return processDescription;
        }

        @Override
        public Class<? extends AProcess> getProcessClass() {
            return processClass;
        }

        @Override
        public AProcess newInstance() throws MException {
            try {
                return getProcessClass().getConstructor().newInstance();
            } catch (InstantiationException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | SecurityException e) {
                throw new MException(RC.STATUS.ERROR, getName(), e);
            }
        }
    }

    public class PoolContainer implements EPool {

        private Class<? extends APool<?>> pool;
        private HashMap<String, EElement> poolElements = new HashMap<>();
        private PoolDescription poolDescription;
        private String name;
        private ProcessContainer process;
        private TreeSet<EAttribute> attributes;

        public PoolContainer(Class<? extends APool<?>> pool) throws MException {
            this.pool = pool;
            poolDescription = pool.getAnnotation(PoolDescription.class);
            if (poolDescription == null)
                throw new MException(RC.ERROR, "Pool without description annotation found", pool);
            name =
                    MString.isEmpty(poolDescription.name())
                            ? pool.getSimpleName()
                            : poolDescription.name();
        }

        public void setProcess(ProcessContainer process) {
            this.process = process;
        }

        public EProcess getProcess() {
            return process;
        }

        public void collectElements() {
            for (EElement element : process.elements.values())
                if (isElementOfPool(element)) poolElements.put(element.getCanonicalName(), element);
        }

        @Override
        public boolean isElementOfPool(EElement element) {
            Class<? extends AElement<?>> clazz = ((ElementContainer) element).getElementClass();
            String elementPool = MSystem.getTemplateCanonicalName(clazz, 0);
            // for direct check
            // if (pool.getCanonicalName().equals(elementPool))
            // with this check also pool subclasses are possible
            EElement poolContainer = process.elements.get(elementPool);
            if (poolContainer == null) return false;
            Class<? extends AElement<?>> poolClass =
                    ((ElementContainer) poolContainer).getElementClass();
            return (pool.isAssignableFrom(poolClass));
        }

        @Override
        public String getCanonicalName() {
            return pool.getCanonicalName();
        }

        @Override
        public List<EElement> getStartPoints(boolean activeOnly) {
            LinkedList<EElement> out = new LinkedList<>();
            for (EElement element : poolElements.values()) {
                Class<? extends AElement<?>> clazz = ((ElementContainer) element).getElementClass();
                if (element.is(AStartPoint.class)
                        && (!activeOnly || !InactiveStartPoint.class.isAssignableFrom(clazz)))
                    out.add(element);
            }
            return out;
        }

        protected Class<? extends APool<?>> getPoolClass() {
            return pool;
        }

        @Override
        public EElement getElement(String name) {
            return poolElements.get(name);
        }

        @Override
        public Set<String> getElementNames() {
            return poolElements.keySet();
        }

        @Override
        public List<EElement> getElements(Class<? extends AElement<?>> ifc) {
            LinkedList<EElement> out = new LinkedList<>();
            for (EElement element : poolElements.values())
                if (ifc.isAssignableFrom(((ElementContainer) element).getElementClass()))
                    out.add(element);
            return out;
        }

        @Override
        public List<EElement> getOutputElements(EElement element) {
            LinkedList<EElement> out = new LinkedList<>();
            for (Output output : element.getOutputs()) {
                EElement o = getElement(output.activity().getCanonicalName());
                if (o != null) out.add(o);
            }
            return out;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public PoolDescription getPoolDescription() {
            return poolDescription;
        }

        @Override
        public String toString() {
            return getCanonicalName();
        }

        @Override
        public APool<?> newInstance() throws MException {
            try {
                return getPoolClass().getDeclaredConstructor().newInstance();
            } catch (InstantiationException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | SecurityException e) {
                throw new MException(RC.STATUS.ERROR, getName(), e);
            }
        }

        @Override
        public Set<EAttribute> getAttributes() {
            if (attributes == null) {
                attributes = new TreeSet<>();
                PojoModel pojoModel = ActivityUtil.createPojoModel(getPoolClass());
                for (PojoAttribute<?> attr : pojoModel) {
                    String name = attr.getName();
                    PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
                    if (desc != null) {
                        attributes.add(new AttributeContainer(name, desc));
                    }
                }
            }
            return attributes;
        }
    }

    public class AttributeContainer implements EAttribute, Comparable<AttributeContainer> {

        private String name;
        private PropertyDescription desc;

        public AttributeContainer(String name, PropertyDescription desc) {
            this.name = name;
            this.desc = desc;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public PropertyDescription getDescription() {
            return desc;
        }

        @Override
        public int compareTo(AttributeContainer o) {
            if (o == null || o.name == null) return -1;
            if (name == null) return 1; // paranoia
            return name.compareTo(o.name);
        }
    }

    public class ElementContainer implements EElement, Comparable<ElementContainer> {

        private Class<? extends AElement<?>> element;
        private String name;
        private ActivityDescription actDescription;
        private TreeSet<EAttribute> attributes;

        public ElementContainer(Class<? extends AElement<?>> element) throws MException {
            this.element = element;
            if (AActivity.class.isAssignableFrom(element)) {
                actDescription = element.getAnnotation(ActivityDescription.class);
                if (actDescription == null)
                    throw new MException(RC.ERROR, "Activity without description annotation", element);
            }
            name =
                    actDescription == null || MString.isEmpty(actDescription.name())
                            ? element.getSimpleName()
                            : actDescription.name();
        }

        @Override
        public String getCanonicalName() {
            return element.getCanonicalName();
        }

        protected Class<? extends AElement<?>> getElementClass() {
            return element;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean is(Class<? extends AElement> ifc) {
            if (ifc == null) return false;
            return ifc.isAssignableFrom(element);
        }

        @Override
        public Output[] getOutputs() {
            if (actDescription == null) return new Output[0];
            return actDescription.outputs();
        }

        @Override
        public Trigger[] getTriggers() {
            if (actDescription == null
                    || actDescription.triggers().length == 0
                    || actDescription.triggers().length == 1
                            && actDescription.triggers()[0].type() == TYPE.NOOP)
                return new Trigger[0];
            return actDescription.triggers();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends ASwimlane<?>> getSwimlane() {
            if (actDescription == null) return null;
            @SuppressWarnings("rawtypes")
            Class<? extends ASwimlane> lane = actDescription.lane();
            return (Class<? extends ASwimlane<?>>) lane;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isInterface(Class<?> ifc) {
            if (ifc == null) return false;
            return ifc.isAssignableFrom(element);
        }

        @Override
        public ActivityDescription getActivityDescription() {
            return actDescription;
        }

        @Override
        public String toString() {
            return getCanonicalName();
        }

        @Override
        public HashMap<String, Long> getSchedulerList() {
            Trigger[] triggers = getTriggers();
            if (triggers.length == 0) return null;
            HashMap<String, Long> out = new HashMap<>();
            int cnt = 0;
            for (Trigger trigger : triggers) {
                if (trigger.type() == TYPE.TIMER) {
                    long time = EngineUtil.getNextScheduledTime(trigger.event());
                    out.put(trigger.name().length() == 0 ? "trigger." + cnt : trigger.name(), time);
                }
                cnt++;
            }
            return out;
        }

        @Override
        public HashMap<String, String> getSignalList() {
            Trigger[] triggers = getTriggers();
            if (triggers.length == 0) return null;
            HashMap<String, String> out = new HashMap<>();
            for (Trigger trigger : triggers) {
                if (trigger.type() == TYPE.SIGNAL) {
                    out.put(trigger.name(), trigger.event());
                }
            }
            return out;
        }

        @Override
        public HashMap<String, String> getMessageList() {
            Trigger[] triggers = getTriggers();
            if (triggers.length == 0) return null;
            HashMap<String, String> out = new HashMap<>();
            for (Trigger trigger : triggers) {
                if (trigger.type() == TYPE.MESSAGE) {
                    out.put(trigger.name(), trigger.event());
                }
            }
            return out;
        }

        @Override
        public Class<? extends AActor> getAssignedActor(EPool pool) {
            ActorAssign actorAssign = getElementClass().getAnnotation(ActorAssign.class);
            if (actorAssign != null) return actorAssign.value();
            Class<? extends ASwimlane<?>> lane = getSwimlane();
            if (lane == null) lane = DefaultSwimlane.class; // for secure
            actorAssign = lane.getAnnotation(ActorAssign.class);
            if (actorAssign != null) return actorAssign.value();
            if (pool != null) return pool.getPoolDescription().actorDefault();
            return NobodyActor.class;
        }

        @Override
        public SubDescription getSubDescription() {
            return element.getAnnotation(SubDescription.class);
        }

        @Override
        public AElement<?> newInstance() throws MException {
            try {
                return getElementClass().getDeclaredConstructor().newInstance();
            } catch (InstantiationException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | SecurityException e) {
                throw new MException(RC.STATUS.ERROR, getName(), e);
            }
        }

        @Override
        public Set<EAttribute> getAttributes() {
            if (attributes == null) {
                attributes = new TreeSet<>();
                PojoModel pojoModel = ActivityUtil.createPojoModel(getElementClass());
                for (PojoAttribute<?> attr : pojoModel) {
                    String name = attr.getName();
                    PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
                    if (desc != null) {
                        attributes.add(new AttributeContainer(name, desc));
                    }
                }
            }
            return attributes;
        }

        @Override
        public int compareTo(ElementContainer o) {
            if (o == null || o.name == null) return -1;
            if (name == null) return 1; // paranoia
            return name.compareTo(o.name);
        }
    }

    public Set<String> getProcessNames() {
        return processes.keySet();
    }
}
