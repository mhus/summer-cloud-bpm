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
package de.mhus.app.reactive.engine;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.error.WrongStateException;
import org.summerclouds.common.core.form.FormControl;
import org.summerclouds.common.core.form.IFormInformation;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.util.MUri;
import org.summerclouds.common.core.util.MutableUri;
import org.summerclouds.common.core.util.SoftHashMap;

import de.mhus.app.reactive.engine.util.EngineUtil;
import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.AUserTask;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.PoolDescription;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.model.engine.EAttribute;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.engine.EngineMessage;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.engine.PCaseLock;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.Result;
import de.mhus.app.reactive.model.engine.RuntimeNode;
import de.mhus.app.reactive.model.engine.SearchCriterias;
import de.mhus.app.reactive.model.ui.ICase;
import de.mhus.app.reactive.model.ui.ICaseDescription;
import de.mhus.app.reactive.model.ui.IEngine;
import de.mhus.app.reactive.model.ui.IModel;
import de.mhus.app.reactive.model.ui.INode;
import de.mhus.app.reactive.model.ui.INodeDescription;
import de.mhus.app.reactive.model.ui.IPool;
import de.mhus.app.reactive.model.ui.IProcess;
import de.mhus.app.reactive.model.uimp.UiCase;
import de.mhus.app.reactive.model.uimp.UiCaseDescription;
import de.mhus.app.reactive.model.uimp.UiFormInformation;
import de.mhus.app.reactive.model.uimp.UiModel;
import de.mhus.app.reactive.model.uimp.UiNode;
import de.mhus.app.reactive.model.uimp.UiNodeDescription;
import de.mhus.app.reactive.model.uimp.UiPool;
import de.mhus.app.reactive.model.uimp.UiProcess;

public class UiEngine extends MLog implements IEngine {

    private Engine engine;
    private String user;
    private SoftHashMap<String, Boolean> cacheAccessRead = new SoftHashMap<>();
    private SoftHashMap<String, Boolean> cacheAccessWrite = new SoftHashMap<>();
    private SoftHashMap<UUID, Boolean> cacheAccessExecute = new SoftHashMap<>();
    //	private SoftHashMap<String, EngineContext> cacheContext = new SoftHashMap<>();
    private Locale locale;
    private MProperties defaultProcessProperties = new MProperties();

    public UiEngine(Engine engine, String user, Locale locale) {
        this.engine = engine;
        this.user = user;
        this.locale = locale;
    }

    @Override
    public List<INode> searchNodes(
            SearchCriterias criterias, int page, int size, String... propertyNames)
            throws NotFoundException, IOException {
        if (engine == null) throw new WrongStateException();
        LinkedList<INode> out = new LinkedList<>();
        int cnt = 0;
        int first = page * size;
        Result<PNodeInfo> result = engine.storageSearchFlowNodes(criterias);
        for (PNodeInfo info : result) {
            if (user.equals(info.getAssigned()) || hasReadAccess(info.getUri())) {
                try {
                    if (cnt >= first) {

                        Map<String, String> properties = new TreeMap<>();
                        if (propertyNames == null
                                || propertyNames.length == 1 && "*".equals(propertyNames[0])) {
                            for (int i = 0; i < EngineConst.MAX_INDEX_VALUES; i++)
                                if (info.getIndexValue(i) != null)
                                    properties.put(
                                            EngineConst.UI_PNODE_PREFIX + "index" + i,
                                            info.getIndexValue(i));
                        } else {
                            for (String name : propertyNames) {
                                if (name == null) continue;
                                if (name.startsWith(EngineConst.UI_PNODE_PREFIX)) {
                                    // use switch instead of for loop to improve performance
                                    switch (name) {
                                        case "pnode.index0":
                                            properties.put("pnode.index0", info.getIndexValue(0));
                                            break;
                                        case "pnode.index1":
                                            properties.put("pnode.index1", info.getIndexValue(1));
                                            break;
                                        case "pnode.index2":
                                            properties.put("pnode.index2", info.getIndexValue(2));
                                            break;
                                        case "pnode.index3":
                                            properties.put("pnode.index3", info.getIndexValue(3));
                                            break;
                                        case "pnode.index4":
                                            properties.put("pnode.index4", info.getIndexValue(4));
                                            break;
                                        case "pnode.index5":
                                            properties.put("pnode.index5", info.getIndexValue(5));
                                            break;
                                        case "pnode.index6":
                                            properties.put("pnode.index6", info.getIndexValue(6));
                                            break;
                                        case "pnode.index7":
                                            properties.put("pnode.index7", info.getIndexValue(7));
                                            break;
                                        case "pnode.index8":
                                            properties.put("pnode.index8", info.getIndexValue(8));
                                            break;
                                        case "pnode.index9":
                                            properties.put("pnode.index9", info.getIndexValue(9));
                                            break;
                                    }
                                }
                            }
                        }
                        out.add(new UiNode(info, properties));
                    }
                    cnt++;
                } catch (Exception e) {
                    log().d("search node failed", info, e);
                }
                if (out.size() >= size) break;
            }
        }
        result.close();
        return out;
    }

    @Override
    public List<ICase> searchCases(
            SearchCriterias criterias, int page, int size, String... propertyNames)
            throws NotFoundException, IOException {
        if (engine == null) throw new WrongStateException();
        LinkedList<ICase> out = new LinkedList<>();
        int cnt = 0;
        int first = page * size;
        Result<PCaseInfo> result = engine.storageSearchCases(criterias);
        for (PCaseInfo info : result) {
            if (hasReadAccess(info.getUri())) {
                try {
                    if (cnt >= first) {

                        Map<String, String> properties = new TreeMap<>();
                        if (propertyNames == null
                                || propertyNames.length == 1 && "*".equals(propertyNames[0])) {
                            for (int i = 0; i < EngineConst.MAX_INDEX_VALUES; i++)
                                if (info.getIndexValue(i) != null)
                                    properties.put(
                                            EngineConst.UI_PNODE_PREFIX + "index" + i,
                                            info.getIndexValue(i));
                        } else {
                            for (String name : propertyNames) {
                                if (name == null) continue;
                                if (name.startsWith(EngineConst.UI_PNODE_PREFIX)) {
                                    // use switch instead of for loop to improve performance
                                    switch (name) {
                                        case "pnode.index0":
                                            properties.put("pnode.index0", info.getIndexValue(0));
                                            break;
                                        case "pnode.index1":
                                            properties.put("pnode.index1", info.getIndexValue(1));
                                            break;
                                        case "pnode.index2":
                                            properties.put("pnode.index2", info.getIndexValue(2));
                                            break;
                                        case "pnode.index3":
                                            properties.put("pnode.index3", info.getIndexValue(3));
                                            break;
                                        case "pnode.index4":
                                            properties.put("pnode.index4", info.getIndexValue(4));
                                            break;
                                        case "pnode.index5":
                                            properties.put("pnode.index5", info.getIndexValue(5));
                                            break;
                                        case "pnode.index6":
                                            properties.put("pnode.index6", info.getIndexValue(6));
                                            break;
                                        case "pnode.index7":
                                            properties.put("pnode.index7", info.getIndexValue(7));
                                            break;
                                        case "pnode.index8":
                                            properties.put("pnode.index8", info.getIndexValue(8));
                                            break;
                                        case "pnode.index9":
                                            properties.put("pnode.index9", info.getIndexValue(9));
                                            break;
                                    }
                                }
                            }
                        }

                        out.add(new UiCase(info, properties));
                    }
                    cnt++;
                } catch (Exception e) {
                    log().d("search case failed", info, e);
                }
                if (out.size() >= size) break;
            }
        }
        result.close();
        return out;
    }

    //	private EngineContext getContext(String uri) {
    //		synchronized (cacheContext) {
    //			EngineContext context = cacheContext.get(uri);
    //			if (context != null) return context;
    //		}
    //		MUri muri = MUri.toUri(uri);
    //		try {
    //			EProcess process = engine.getProcess(muri);
    //			EPool pool = engine.getPool(process, muri);
    //			EngineContext context = new EngineContext(engine);
    //			context.setEProcess(process);
    //			context.setEPool(pool);
    //			synchronized (cacheContext) {
    //				cacheContext.put(uri, context);
    //			}
    //			return context;
    //		} catch (Throwable t) {
    //			log().e(uri,user,t);
    //			return null;
    //		}
    //	}

    public boolean hasReadAccess(String uri) {
        synchronized (cacheAccessRead) {
            Boolean hasAccess = cacheAccessRead.get(uri);
            if (hasAccess != null) return hasAccess;
        }

        boolean hasAccess = engine.hasReadAccess(uri, user);
        synchronized (cacheAccessRead) {
            cacheAccessRead.put(uri, hasAccess);
        }
        return hasAccess;
    }

    public boolean hasWriteAccess(String uri) {
        synchronized (cacheAccessWrite) {
            Boolean hasAccess = cacheAccessWrite.get(uri);
            if (hasAccess != null) return hasAccess;
        }

        boolean hasAccess = engine.hasWriteAccess(uri, user);
        synchronized (cacheAccessWrite) {
            cacheAccessWrite.put(uri, hasAccess);
        }
        return hasAccess;
    }

    public boolean hasWriteAccess(UUID nodeId) {
        synchronized (cacheAccessExecute) {
            Boolean hasAccess = cacheAccessExecute.get(nodeId);
            if (hasAccess != null) return hasAccess;
        }

        boolean hasAccess = engine.hasExecuteAccess(nodeId, user);
        synchronized (cacheAccessExecute) {
            cacheAccessExecute.put(nodeId, hasAccess);
        }
        return hasAccess;
    }

    @Override
    public IProcess getProcess(String uri) throws MException {
        if (engine == null) throw new WrongStateException();

        MProperties properties = new MProperties();

        EProcess process = engine.getProcess(MUri.toUri(uri));
        if (process == null) return null;
        for (String poolName : process.getPoolNames()) {
            EPool pool = process.getPool(poolName);
            String pUri =
                    EngineConst.SCHEME_REACTIVE
                            + "://"
                            + process.getCanonicalName()
                            + ":"
                            + process.getVersion()
                            + "/"
                            + pool.getCanonicalName();

            PoolDescription pd = pool.getPoolDescription();
            if (pd != null) { // paranoia
                properties.setString(
                        pUri + "#displayName",
                        pd.displayName().length() == 0 ? pool.getName() : pd.displayName());
                properties.setString(pUri + "#description", pd.description());
                String[] index = pd.indexDisplayNames();
                for (int i = 0; i < Math.min(index.length, EngineConst.MAX_INDEX_VALUES); i++) {
                    if (index[i] != null) properties.setString(pUri + "#pnode.index" + i, index[i]);
                }
                for (EAttribute attr : pool.getAttributes()) {
                    String name = attr.getName();
                    PropertyDescription desc = attr.getDescription();
                    if (desc.displayName().length() != 0) name = desc.displayName();
                    else if (desc.name().length() != 0) name = desc.name();
                    properties.setString(pUri + "#case." + attr.getName(), name);
                }
            }

            for (String eleName : pool.getElementNames()) {
                EElement ele = pool.getElement(eleName);
                ActivityDescription desc = ele.getActivityDescription();
                if (desc == null) continue;
                String eUri = pUri + "/" + ele.getCanonicalName();

                properties.setString(
                        eUri + "#displayName",
                        desc.displayName().length() == 0 ? ele.getName() : desc.displayName());
                properties.setString(eUri + "#description", desc.description());
                String[] index = desc.indexDisplayNames();
                for (int i = 0; i < Math.min(index.length, EngineConst.MAX_INDEX_VALUES); i++) {
                    if (index[i] != null) properties.setString(eUri + "#pnode.index" + i, index[i]);
                }
                for (EAttribute attr : ele.getAttributes()) {
                    String name = attr.getName();
                    PropertyDescription pdesc = attr.getDescription();
                    if (pdesc.displayName().length() != 0) name = pdesc.displayName();
                    else if (pdesc.name().length() != 0) name = pdesc.name();
                    properties.setString(pUri + "#node." + attr.getName(), name);
                }
            }
        }
        properties.putAll(defaultProcessProperties);

        UiProcess out = new UiProcess(properties, getLocale());
        return out;
    }

    @Override
    public IPool getPool(String uri) throws MException {
        if (engine == null) throw new WrongStateException();
        MUri u = MUri.toUri(uri);
        EProcess process = engine.getProcess(u);
        EPool pool = engine.getPool(process, u);

        if (pool == null) return null;

        MProperties properties = new MProperties();
        String pUri =
                EngineConst.SCHEME_REACTIVE
                        + "://"
                        + process.getCanonicalName()
                        + ":"
                        + process.getVersion()
                        + "/"
                        + pool.getCanonicalName();

        PoolDescription pd = pool.getPoolDescription();
        if (pd != null) { // paranoia
            properties.setString(
                    pUri + "#displayName",
                    pd.displayName().length() == 0 ? pool.getName() : pd.displayName());
            properties.setString(pUri + "#description", pd.description());
            String[] index = pd.indexDisplayNames();
            for (int i = 0; i < Math.min(index.length, EngineConst.MAX_INDEX_VALUES); i++) {
                if (index[i] != null) properties.setString(pUri + "#pnode.index" + i, index[i]);
            }
            for (EAttribute attr : pool.getAttributes()) {
                String name = attr.getName();
                PropertyDescription desc = attr.getDescription();
                if (desc.displayName().length() != 0) name = desc.displayName();
                else if (desc.name().length() != 0) name = desc.name();
                properties.setString(pUri + "#case." + attr.getName(), name);
            }
        }
        properties.putAll(defaultProcessProperties);

        UiPool out = new UiPool(pUri, pd, properties);
        return out;
    }

    @Override
    public ICase getCase(String id, String... propertyNames) throws Exception {
        if (engine == null) throw new WrongStateException();
        PCaseInfo info = EngineUtil.getCaseInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) return null;

        // load properties
        Map<String, String> properties = new TreeMap<>();
        PCase caze = null;
        if (propertyNames == null || propertyNames.length == 1 && "*".equals(propertyNames[0])) {
            for (int i = 0; i < EngineConst.MAX_INDEX_VALUES; i++)
                if (info.getIndexValue(i) != null)
                    properties.put(
                            EngineConst.UI_PNODE_PREFIX + "index" + i, info.getIndexValue(i));
            caze = engine.getCaseWithoutLock(info.getId());
            for (Entry<String, Object> entry : caze.getParameters().entrySet())
                properties.put(
                        EngineConst.UI_CASE_PREFIX + entry.getKey(),
                        String.valueOf(entry.getValue()));
        } else {
            for (String name : propertyNames) {
                if (name == null) continue;
                if (name.startsWith(EngineConst.UI_PNODE_PREFIX)) {
                    // use switch instead of for loop to improve performance
                    switch (name) {
                        case "pnode.index0":
                            properties.put("pnode.index0", info.getIndexValue(0));
                            break;
                        case "pnode.index1":
                            properties.put("pnode.index1", info.getIndexValue(1));
                            break;
                        case "pnode.index2":
                            properties.put("pnode.index2", info.getIndexValue(2));
                            break;
                        case "pnode.index3":
                            properties.put("pnode.index3", info.getIndexValue(3));
                            break;
                        case "pnode.index4":
                            properties.put("pnode.index4", info.getIndexValue(4));
                            break;
                        case "pnode.index5":
                            properties.put("pnode.index5", info.getIndexValue(5));
                            break;
                        case "pnode.index6":
                            properties.put("pnode.index6", info.getIndexValue(6));
                            break;
                        case "pnode.index7":
                            properties.put("pnode.index7", info.getIndexValue(7));
                            break;
                        case "pnode.index8":
                            properties.put("pnode.index8", info.getIndexValue(8));
                            break;
                        case "pnode.index9":
                            properties.put("pnode.index9", info.getIndexValue(9));
                            break;
                    }
                } else if (name.startsWith(EngineConst.UI_CASE_PREFIX)) {
                    if (caze == null) caze = engine.getCaseWithoutLock(info.getId());
                    Object v =
                            caze.getParameters()
                                    .get(name.substring(EngineConst.UI_CASE_PREFIX.length()));
                    if (v != null) properties.put(name, String.valueOf(v));
                }
            }
        }
        return new UiCase(info, properties);
    }

    @Override
    public INode getNode(String id, String... propertyNames) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) return null;

        // load properties
        Map<String, String> properties = new TreeMap<>();
        PCase caze = null;
        PNode node = null;
        if (propertyNames == null || propertyNames.length == 1 && "*".equals(propertyNames[0])) {
            for (int i = 0; i < EngineConst.MAX_INDEX_VALUES; i++)
                if (info.getIndexValue(i) != null)
                    properties.put(
                            EngineConst.UI_PNODE_PREFIX + "index" + i, info.getIndexValue(i));
            caze = engine.getCaseWithoutLock(info.getCaseId());
            for (Entry<String, Object> entry : caze.getParameters().entrySet())
                properties.put(
                        EngineConst.UI_CASE_PREFIX + entry.getKey(),
                        String.valueOf(entry.getValue()));
            node = engine.getNodeWithoutLock(info.getId());
            for (Entry<String, Object> entry : node.getParameters().entrySet())
                properties.put(
                        EngineConst.UI_NODE_PREFIX + entry.getKey(),
                        String.valueOf(entry.getValue()));
        } else {
            for (String name : propertyNames) {
                if (name == null) continue;
                if (name.startsWith(EngineConst.UI_PNODE_PREFIX)) {
                    // use switch instead of for loop to improve performance
                    switch (name) {
                        case "pnode.index0":
                            properties.put("pnode.index0", info.getIndexValue(0));
                            break;
                        case "pnode.index1":
                            properties.put("pnode.index1", info.getIndexValue(1));
                            break;
                        case "pnode.index2":
                            properties.put("pnode.index2", info.getIndexValue(2));
                            break;
                        case "pnode.index3":
                            properties.put("pnode.index3", info.getIndexValue(3));
                            break;
                        case "pnode.index4":
                            properties.put("pnode.index4", info.getIndexValue(4));
                            break;
                        case "pnode.index5":
                            properties.put("pnode.index5", info.getIndexValue(5));
                            break;
                        case "pnode.index6":
                            properties.put("pnode.index6", info.getIndexValue(6));
                            break;
                        case "pnode.index7":
                            properties.put("pnode.index7", info.getIndexValue(7));
                            break;
                        case "pnode.index8":
                            properties.put("pnode.index8", info.getIndexValue(8));
                            break;
                        case "pnode.index9":
                            properties.put("pnode.index9", info.getIndexValue(9));
                            break;
                    }
                } else if (name.startsWith(EngineConst.UI_CASE_PREFIX)) {
                    if (caze == null) caze = engine.getCaseWithoutLock(info.getCaseId());
                    Object v =
                            caze.getParameters()
                                    .get(name.substring(EngineConst.UI_CASE_PREFIX.length()));
                    if (v != null) properties.put(name, String.valueOf(v));
                } else if (name.startsWith(EngineConst.UI_NODE_PREFIX)) {
                    if (node == null) node = engine.getNodeWithoutLock(info.getId());
                    Object v =
                            node.getParameters()
                                    .get(name.substring(EngineConst.UI_NODE_PREFIX.length()));
                    if (v != null) properties.put(name, String.valueOf(v));
                }
            }
        }

        return new UiNode(info, properties);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public MProperties getDefaultProcessProperties() {
        return defaultProcessProperties;
    }

    @Override
    public String getUser() {
        return user;
    }

    public void setDefaultProcessProperties(MProperties defaultProcessProperties) {
        this.defaultProcessProperties = defaultProcessProperties;
    }

    @Override
    public Object doExecute(String uri) throws Exception {
        if (engine == null) throw new WrongStateException();
        MutableUri u = (MutableUri) MUri.toUri(uri);
        u.setUsername(user);
        return engine.execute(u);
    }

    @Override
    public Object doExecute2(String uri, IProperties properties) throws Exception {
        if (engine == null) throw new WrongStateException();
        MutableUri u = (MutableUri) MUri.toUri(uri);
        u.setUsername(user);
        return engine.execute(u, properties);
    }

    @Override
    public ICaseDescription getCaseDescription(String uri) throws Exception {
        IProcess process = null;
        try {
            process = getProcess(uri);
        } catch (MException e) {
            log().d(uri, e);
        }
        return new UiCaseDescription(uri, process);
    }

    @Override
    public INodeDescription getNodeDescription(String uri, String name) throws Exception {
        IProcess process = null;
        try {
            process = getProcess(uri);
        } catch (MException e) {
            log().d(uri, e);
        }
        return new UiNodeDescription(uri, name, process);
    }

    @Override
    public void doArchive(UUID caseId) throws Exception {
        if (engine == null) throw new WrongStateException();
        PCaseInfo caze = engine.getCaseInfo(caseId);
        if (caze.getState() != STATE_CASE.CLOSED) throw new MException(RC.BUSY, "wrong case {1} state {2}", caseId, caze.getState());
        engine.archiveCase(caseId);
    }

    @Override
    public void close() {
        // engine = null; // do not close - this instance will survive
    }

    @Override
    public boolean isClosed() {
        return engine == null;
    }

    @Override
    public IModel getModel(UUID nodeId) throws Exception {
        PNodeInfo node = engine.getFlowNodeInfo(nodeId);
        if (!hasReadAccess(node.getUri())) return null;
        return newUiModel(this, engine, nodeId);
    }

    @Override
    public IModel[] getCaseModels(UUID caseId) throws Exception {
        if (engine == null) throw new WrongStateException();
        PCaseInfo caze = engine.getCaseInfo(caseId);
        if (!hasReadAccess(caze.getUri())) return null;
        LinkedList<IModel> out = new LinkedList<>();
        for (PNodeInfo node : engine.storageGetFlowNodes(caseId, null)) {
            if (node.getState() != STATE_NODE.CLOSED && node.getState() != STATE_NODE.SEVERE)
                out.add(newUiModel(this, engine, node.getId()));
        }
        return out.toArray(new IModel[out.size()]);
    }

    private IModel newUiModel(UiEngine ui, Engine engine, UUID nodeId) throws Exception {

        INodeDescription[] outputs;
        EngineMessage[] messages;
        INodeDescription predecessor = null;
        INodeDescription node;

        PNode pNode = engine.getNodeWithoutLock(nodeId);
        PCase caze = engine.getCaseWithoutLock(pNode.getCaseId());
        MUri uri = MUri.toUri(caze.getUri());
        EProcess process = engine.getProcess(uri);
        EPool pool = engine.getPool(process, uri);
        String uriStr = "bpm://" + process.getCanonicalName() + "/" + pool.getCanonicalName();
        node = ui.getNodeDescription(uriStr, pNode.getCanonicalName());
        EElement element = pool.getElement(pNode.getCanonicalName());
        ActivityDescription desc = element.getActivityDescription();

        Output[] out = desc.outputs();
        outputs = new INodeDescription[out.length];
        for (int i = 0; i < out.length; i++)
            outputs[i] = ui.getNodeDescription(uriStr, out[i].activity().getCanonicalName());

        EngineContext context = engine.createContext(null, caze, pNode);
        PNode pRuntime = engine.getRuntimeForPNode(context, pNode);
        RuntimeNode aRuntime = engine.createRuntimeObject(context, pRuntime);

        messages = aRuntime.getMessages().toArray(new EngineMessage[0]);

        for (EngineMessage msg : messages) {
            if (msg.getToNode() != null
                    && msg.getFromNode() != null
                    && msg.getToNode().equals(nodeId)) {
                PNodeInfo predecessorNode = engine.getFlowNodeInfo(msg.getFromNode());
                predecessor = ui.getNodeDescription(uriStr, predecessorNode.getCanonicalName());
                break;
            }
        }

        return new UiModel(nodeId, outputs, messages, predecessor, node);
    }

    @Override
    public IFormInformation getNodeUserForm(String id) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        // TODO check assign
        try {
            engine.assignUserTask(info.getId(), getUser());
        } catch (IOException | MException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        AUserTask<?> un = (AUserTask<?>) initANode(info);
        return new UiFormInformation(un.getForm(), un.getActionHandler(), un.getFormControl());
    }

    @Override
    public IProperties getNodeUserFormValues(String id) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        return ((AUserTask<?>) initANode(info)).getFormValues();
    }

    @Override
    public Class<? extends FormControl> getNodeUserFormControl(String id) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        return ((AUserTask<?>) initANode(info)).getFormControl();
    }

    private synchronized AActivity<?> initANode(PNodeInfo info) {
        try {
            PNode node = engine.getNodeWithoutLock(info.getId());
            PCase caze = engine.getCaseWithoutLock(node.getCaseId());
            EngineContext context = engine.createContext(null, caze, node);
            return context.getANode();
        } catch (Throwable t) {
            log().e(t);
            return null;
        }
    }

    @Override
    public void submitUserTask(String id, IProperties values) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        engine.submitUserTask(info.getId(), values);
    }

    @Override
    public void doUnassignUserTask(String id) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        engine.unassignUserTask(info.getId());
    }

    @Override
    public void doAssignUserTask(String id) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        engine.assignUserTask(info.getId(), getUser());
    }

    @Override
    public void setDueDays(String id, int days) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        try (PCaseLock lock = engine.getCaseLock(info, "setDueDays", "days", days)) {
            PNode node = lock.getFlowNode(info);
            node.setDueDays(days);
            lock.saveFlowNode(node);
        }
    }

    @Override
    public MProperties onUserTaskAction(String id, String action, MProperties values)
            throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        return engine.onUserTaskAction(info.getId(), action, values);
    }

    @Override
    public MProperties onUserCaseAction(String id, String action, MProperties values)
            throws Exception {
        if (engine == null) throw new WrongStateException();
        PCaseInfo info = EngineUtil.getCaseInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Case", id);
        return engine.onUserCaseAction(info.getId(), action, values);
    }

    @Override
    public List<EngineMessage[]> getCaseRuntimeMessages(String id) throws Exception {
        if (engine == null) throw new WrongStateException();
        PCaseInfo info = EngineUtil.getCaseInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Case", id);

        LinkedList<EngineMessage[]> out = new LinkedList<>();
        PCase caze = engine.getCaseWithoutLock(info.getId());
        EngineContext context = engine.createContext(caze);

        for (PNodeInfo node : engine.storageGetFlowNodes(info.getId(), null)) {
            if (node.getType() == TYPE_NODE.RUNTIME) {
                System.out.println(">>> RUNTIME " + node.getId() + " " + node.getState());
                try {
                    PNode pRuntime = engine.getNodeWithoutLock(node.getId());
                    RuntimeNode aRuntime = engine.createRuntimeObject(context, pRuntime);
                    List<EngineMessage> messages = aRuntime.getMessages();
                    out.add(messages.toArray(new EngineMessage[messages.size()]));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        return out;
    }

    @Override
    public EngineMessage[] getNodeRuntimeMessage(String id) throws Exception {
        if (engine == null) throw new WrongStateException();
        PNodeInfo info = EngineUtil.getFlowNodeInfo(engine, id);
        if (!engine.hasReadAccess(info.getUri(), user)) throw new NotFoundException("Node", id);
        PNode node = engine.getNodeWithoutLock(info.getId());
        PCase caze = engine.getCaseWithoutLock(info.getCaseId());
        EngineContext context = engine.createContext(null, caze, node);
        PNode pRuntime = engine.getRuntimeForPNode(context, node);
        RuntimeNode aRuntime = engine.createRuntimeObject(context, pRuntime);
        List<EngineMessage> messages = aRuntime.getMessages();
        return messages.toArray(new EngineMessage[messages.size()]);
    }
}
