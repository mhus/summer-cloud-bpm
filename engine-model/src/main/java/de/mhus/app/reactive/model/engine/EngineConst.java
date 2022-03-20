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
package de.mhus.app.reactive.model.engine;

import org.summerclouds.common.core.cfg.CfgLong;
import org.summerclouds.common.core.tool.MPeriod;

public class EngineConst {

    public static final String SCHEME_REACTIVE = "bpm";
    public static final String OPTION_CUSTOM_ID = "customId";
    public static final String OPTION_CUSTOMER_ID = "customerId";
    public static CfgLong DEFAULT_ACTIVITY_TIMEOUT =
            new CfgLong(
                    EEngine.class, "defaultActivityTimeout", MPeriod.MINUTE_IN_MILLISECONDS * 5);
    public static final int TRY_COUNT = 3;
    public static final int DEFAULT_TRY_COUNT = 1;
    public static final long END_OF_DAYS = Long.MAX_VALUE;
    public static final int MAX_INDEX_VALUES = 10;
    public static final String UI_PNODE_PREFIX = "pnode.";
    public static final String UI_CASE_PREFIX = "case.";
    public static final String UI_NODE_PREFIX = "node.";
    public static final int MAX_CREATE_ACTIVITY = 1000;
    public static final int ERROR_CODE_MAX_CREATE_ACTIVITY = -1000;

    public static final String MILESTONE_START = "NEW";
    public static final String MILESTONE_PROGRESS = "PROGRESS";
    public static final String MILESTONE_FINISHED = "FINISHED";
    /** Set this parameter to wait for milestone PROGRESS after start of case. */
    public static final String PARAM_PROGRESS = "progress";

    public static final String OPTION_UUID = "uuid";
    public static final String OPTION_CLOSE_ACTIVITY = "closeActivity";
    public static final String AREA_PREFIX = "area:";
    public static final String ACTION_LIST = "de.mhus.app.reactive.model.actionlist";
    public static final String FIELD_DUEDATE = "duedate";
    public static final String ACTION_FORM = "de.mhus.app.reactive.model.actionform";
    public static final String ACTION_RET_ACTION = "de.mhus.app.reactive.model.actionretaction";
    public static final String ACTION_RET_ACTION_CASE = "case";
    public static final String ACTION_RET_ACTION_NODE = "node";
    public static final String ACTION_RET_ACTION_FORM = "form";
    public static final String ACTION_RET_ID = "de.mhus.app.reactive.model.actionretid";
}
