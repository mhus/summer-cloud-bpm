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
package de.mhus.app.reactive.model.util;

import org.summerclouds.common.core.form.ActionHandler;
import org.summerclouds.common.core.form.DefRoot;
import org.summerclouds.common.core.form.FormControl;
import org.summerclouds.common.core.form.IFormInformation;

public class NoForm implements IFormInformation {

    @Override
    public DefRoot getForm() {
        return null;
    }

    @Override
    public Class<? extends ActionHandler> getActionHandler() {
        return null;
    }

    @Override
    public Class<? extends FormControl> getFormControl() {
        return null;
    }
}
