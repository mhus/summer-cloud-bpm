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
package de.mhus.app.reactive.examples.simple1.forms;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.consts.GenerateConst;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.form.DefRoot;
import org.summerclouds.common.core.form.FormControl;
import org.summerclouds.common.core.form.Item;
import org.summerclouds.common.core.form.definition.FaColumns;
import org.summerclouds.common.core.form.definition.FaCustomDateFormat;
import org.summerclouds.common.core.form.definition.FaDefaultValue;
import org.summerclouds.common.core.form.definition.FaDisabled;
import org.summerclouds.common.core.form.definition.FaFullWidth;
import org.summerclouds.common.core.form.definition.FaHtml;
import org.summerclouds.common.core.form.definition.FaItemDefinition;
import org.summerclouds.common.core.form.definition.FaReadOnly;
import org.summerclouds.common.core.form.definition.FaShowInformationPanel;
import org.summerclouds.common.core.form.definition.FmAction;
import org.summerclouds.common.core.form.definition.FmCheckbox;
import org.summerclouds.common.core.form.definition.FmCombobox;
import org.summerclouds.common.core.form.definition.FmDate;
import org.summerclouds.common.core.form.definition.FmDate.FORMATS;
import org.summerclouds.common.core.form.definition.FmLabel;
import org.summerclouds.common.core.form.definition.FmLayout100;
import org.summerclouds.common.core.form.definition.FmLayout2x50;
import org.summerclouds.common.core.form.definition.FmLayout3x33;
import org.summerclouds.common.core.form.definition.FmLayoutWizard;
import org.summerclouds.common.core.form.definition.FmLink;
import org.summerclouds.common.core.form.definition.FmNumber;
import org.summerclouds.common.core.form.definition.FmNumber.TYPES;
import org.summerclouds.common.core.form.definition.FmOptions;
import org.summerclouds.common.core.form.definition.FmPassword;
import org.summerclouds.common.core.form.definition.FmRichText;
import org.summerclouds.common.core.form.definition.FmText;
import org.summerclouds.common.core.form.definition.FmTextArea;
import org.summerclouds.common.core.form.definition.FmVoid;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.pojo.Embedded;
import org.summerclouds.common.core.pojo.Public;
import org.summerclouds.common.core.util.Address.SALUTATION;

import de.mhus.app.reactive.examples.simple1.S1Pool;
import de.mhus.app.reactive.examples.simple1.S1TheEnd;
import de.mhus.app.reactive.examples.simple1._S1Pool;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.util.bpmn2.RUserTask;

@ActivityDescription(
        displayName = "Complex User Form 02",
        outputs = @Output(activity = S1TheEnd.class))
@GenerateConst
public class S1UserForm02 extends RUserTask<S1Pool> {

    @PropertyDescription private String text3 = "text3";
    @PropertyDescription private String option = "1";

    @PropertyDescription(persistent = false)
    @Public(name = "option.items")
    private Item[] optionOptions =
            new Item[] {
                new Item("1", "One"), new Item("2", "Two"),
            };

    @PropertyDescription @Embedded private Address owner = new Address();

    {
        owner.setSalutation(SALUTATION.MR);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setStreet("Baker Street");
        owner.setStreetNumber("221B");
        owner.setZip("12345");
        owner.setTown("Everville");
        owner.setEmail("john@everville.com");
        owner.setTelefon("0123 55 55 55");
    }

    @PropertyDescription(persistent = false)
    private Item[] salutationDef =
            new Item[] {
                new Item("MR", "Mr"), new Item("MRS", "Mrs"),
            };

    @Override
    public DefRoot getForm() {
        return new DefRoot(
                new FaShowInformationPanel(),
                new FmLayoutWizard(
                        "wizard",
                        "",
                        "",
                        new FaFullWidth(),
                        new FmLayout3x33(
                                "t1",
                                "Case",
                                "",
                                new FmText(M.n(_S1Pool._TEXT1), "Text1", "", new FaReadOnly()),
                                new FmVoid(),
                                new FmText(M.n(_S1Pool._TEXT2), "Text2", "")),
                        new FmLayout3x33(
                                "t3",
                                "Node",
                                "",
                                new FmText(
                                        M.n(_S1UserForm02._TEXT3), "Text3", "", new FaColumns(3)),
                                new FmCombobox("option", "Option", "Sample Option with options"),
                                new FmVoid(new FaColumns(2)),
                                new FmAction("submit", "submit:action=submit", "Submit", "Send"),
                                new FmVoid(),
                                new FmAction(
                                        "actionrandom",
                                        "action:random",
                                        "Random",
                                        "Random values for text3")),
                        new FmLayout3x33(
                                "t2",
                                "Address",
                                "Embedded address",
                                Address.createForm(_S1UserForm02._OWNER)),
                        new FmLayout100(
                                "t4",
                                "Widgets",
                                "Test UI Widgets",
                                new FmText("xtext", "Text", "Simple Text Widget"),
                                new FmPassword("xpass", "Password", "Password Text Widget"),
                                new FmCombobox(
                                        "xcombo",
                                        "Combobox",
                                        "Combobox with items",
                                        new FaItemDefinition("salutationdef")),
                                new FmLabel(
                                        "xlabel",
                                        "Label",
                                        "Label widget",
                                        new FaDefaultValue("Static Label Text")),
                                new FmLabel(
                                        "xlabelhtml",
                                        "Html Label",
                                        "Label with html widget",
                                        new FaDefaultValue("Static <b>bold Label</b> Text"),
                                        new FaHtml()),
                                new FmNumber(
                                        "xint", TYPES.INTEGER, "Integer", "Integer number value"),
                                new FmNumber(
                                        "xdouble", TYPES.DOUBLE, "Double", "Double number value"),
                                new FmNumber("xfloat", TYPES.FLOAT, "Float", "Float number value"),
                                new FmNumber("xlong", TYPES.LONG, "Long", "Long number value"),
                                new FmCheckbox("xcheckbox", "Checkbox", "Checkbox boolean value"),
                                new FmDate("xdate", FORMATS.DATE, "Date", "Date value"),
                                new FmDate(
                                        "xdatetime",
                                        FORMATS.DATETIME,
                                        "Date Time",
                                        "Date and time values"),
                                new FmDate(
                                        "xdatetimesec",
                                        FORMATS.DATETIMESECONDS,
                                        "Date Time Seconds",
                                        "Date and time with seconds"),
                                new FmDate("xtime", FORMATS.TIME, "Time", "Time value"),
                                new FmDate(
                                        "xtimesec",
                                        FORMATS.TIMESECONDS,
                                        "Time Seconds",
                                        "Date incl. seconds value"),
                                new FmDate(
                                        "xtimecustom",
                                        FORMATS.CUSTOM,
                                        "Custom Date",
                                        "Date in format dd.MM.yyyy HH:mm",
                                        new FaCustomDateFormat("dd.MM.yyyy HH:mm")),
                                new FmLink(
                                        "xlink",
                                        "Label",
                                        "Link",
                                        "Text as link, static link to google",
                                        new FaDefaultValue("http://google.com")),
                                new FmOptions(
                                        "xoptions",
                                        "Options",
                                        "Options",
                                        new FaItemDefinition("salutationdef")),
                                new FmRichText(
                                        "xrich",
                                        "Rich Text",
                                        "Rich text widget",
                                        new FaFullWidth()),
                                new FmTextArea(
                                        "xtextarea",
                                        "Text Area",
                                        "Text Area widget",
                                        new FaFullWidth())),
                        new FmLayout100(
                                "t5",
                                "Control",
                                "Test Form Control",
                                new FmText("ctext1", "Text1", "Sync with Text2"),
                                new FmText("ctext2", "Text2", "Sync with Text1"),
                                new FmCombobox(
                                        "cgender",
                                        "Select Gender",
                                        "Change visibility be selecting items",
                                        new FaItemDefinition("salutationdef")),
                                new FmLayout100(
                                        "cmale",
                                        "Male",
                                        "",
                                        new FaDisabled(),
                                        new FmCheckbox("cmalesuit", "Suit", "Wear a suit")),
                                new FmLayout100(
                                        "cfemale",
                                        "Female",
                                        "",
                                        new FaDisabled(),
                                        new FmCheckbox("cfemaledress", "Dress", "Wear a dress")),
                                new FmLayout2x50(
                                        "cnow",
                                        "Control Action",
                                        "",
                                        new FmAction(
                                                "cnowaction",
                                                "control:now",
                                                "Now",
                                                "Send control:now to the controller, the controller will set the time into the right field"),
                                        new FmText("cnowtext", "Now", "", new FaReadOnly())))));
    }

    @Override
    public String[] createIndexValues(boolean init) {
        return null;
    }

    @Override
    protected void doSubmit() throws MException {}

    public String getText3() {
        return text3;
    }

    @Override
    public MProperties doAction(String action, IProperties values) {
        if (action.equals("random")) {
            log().i("Action Random");
            MProperties out = new MProperties();
            out.setString("text3", "" + Math.random());
            return out;
        }
        return null;
    }

    public Address getOwner() {
        return owner;
    }

    @Override
    public Class<? extends FormControl> getFormControl() {
        return S1UserForm02Control.class;
    }
}
