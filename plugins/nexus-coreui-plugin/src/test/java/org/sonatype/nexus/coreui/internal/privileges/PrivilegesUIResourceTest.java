/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.coreui.internal.privileges;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.SetOfCheckboxesFormField;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryAdminPrivilegeDescriptor;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPrivilegeDescriptor;
import org.sonatype.nexus.repository.security.RepositoryViewPrivilegeDescriptor;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.ScriptManager;
import org.sonatype.nexus.script.plugin.internal.security.ScriptPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.ApplicationPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrivilegesUIResourceTest
    extends TestSupport
{
  public static final String HELP_TEXT = "The actions you wish to allow";

  public static final String FORM_TYPE = "setOfCheckboxes";

  public static final String FORM_ID = "actions";

  public static final String ACTIONS_KEY = "options";

  public static final List<String> CRUD_ACTION_STRINGS = Arrays.asList(
    "create", "read", "update", "delete", "start", "stop", "associate", "disassociate");

  public static final List<String> BREAD_ACTION_STRINGS = Arrays.asList("browse", "read", "edit", "add", "delete");

  public static final List<String> BREAD_RUN_ACTION_STRINGS = Arrays.asList("browse", "read", "edit", "add", "delete", "run");

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private ScriptManager scriptManager;

  @Mock
  private Format format1;

  @Mock
  private Format format2;

  @Mock
  private Repository repository1;

  @Mock
  private Repository repository2;

  private PrivilegesUIResource underTest;

  @Before
  public void setup() {
    when(repository1.getFormat()).thenReturn(format1);
    when(repository2.getFormat()).thenReturn(format2);
    when(repositoryManager.get("repository1")).thenReturn(repository1);
    when(repositoryManager.get("repository2")).thenReturn(repository2);
    when(selectorManager.findByName(any())).thenReturn(Optional.of(mock(SelectorConfiguration.class)));
    when(scriptManager.get(any())).thenReturn(mock(Script.class));
    when(format1.getValue()).thenReturn("format1");
    when(format2.getValue()).thenReturn("format2");

    List<Format> formats = Arrays.asList(format1, format2);

    Map<String, PrivilegeDescriptor> privilegeDescriptors = new HashMap<>();
    privilegeDescriptors.put(ApplicationPrivilegeDescriptor.TYPE, new ApplicationPrivilegeDescriptor(true));
    privilegeDescriptors.put(RepositoryAdminPrivilegeDescriptor.TYPE,
        new RepositoryAdminPrivilegeDescriptor(repositoryManager, formats, true));
    privilegeDescriptors
        .put(RepositoryViewPrivilegeDescriptor.TYPE, new RepositoryViewPrivilegeDescriptor(repositoryManager, formats, true));
    privilegeDescriptors.put(RepositoryContentSelectorPrivilegeDescriptor.TYPE,
        new RepositoryContentSelectorPrivilegeDescriptor(repositoryManager, selectorManager, formats, true));
    privilegeDescriptors.put(ScriptPrivilegeDescriptor.TYPE, new ScriptPrivilegeDescriptor(scriptManager, true));

    underTest = new PrivilegesUIResource(privilegeDescriptors);
  }

  @Test
  public void listPrivilegesTypes() {
    List<PrivilegesTypesUIResponse> responses = underTest.listPrivilegesTypes();
    assertThat(responses.size(), is(5));

    PrivilegesTypesUIResponse responseApplication = responses.stream()
        .filter(r -> r.getId() == ApplicationPrivilegeDescriptor.TYPE)
        .findFirst().orElse(null);
    assertSetOfCheckboxesFormField(responseApplication, HELP_TEXT, FORM_TYPE, CRUD_ACTION_STRINGS);

    PrivilegesTypesUIResponse responseRepositoryAdmin = responses.stream()
        .filter(r -> r.getId() == RepositoryAdminPrivilegeDescriptor.TYPE)
        .findFirst().orElse(null);
    assertSetOfCheckboxesFormField(responseRepositoryAdmin, HELP_TEXT, FORM_TYPE, BREAD_ACTION_STRINGS);

    PrivilegesTypesUIResponse responseRepositoryView = responses.stream()
        .filter(r -> r.getId() == RepositoryViewPrivilegeDescriptor.TYPE)
        .findFirst().orElse(null);
    assertSetOfCheckboxesFormField(responseRepositoryView, HELP_TEXT, FORM_TYPE, BREAD_ACTION_STRINGS);

    PrivilegesTypesUIResponse responseRepositoryContentSelector = responses.stream()
        .filter(r -> r.getId() == RepositoryContentSelectorPrivilegeDescriptor.TYPE)
        .findFirst().orElse(null);
    assertSetOfCheckboxesFormField(responseRepositoryContentSelector, HELP_TEXT, FORM_TYPE, BREAD_ACTION_STRINGS);

    PrivilegesTypesUIResponse responseScript = responses.stream()
        .filter(r -> r.getId() == ScriptPrivilegeDescriptor.TYPE)
        .findFirst().orElse(null);
    assertSetOfCheckboxesFormField(responseScript, HELP_TEXT, FORM_TYPE, BREAD_RUN_ACTION_STRINGS);
  }

  private void assertSetOfCheckboxesFormField(PrivilegesTypesUIResponse response,
                               String helpText,
                               String type,
                               List<String> actions)
  {
    List<FormField> formFields = response.getFormFields();
    SetOfCheckboxesFormField formField = (SetOfCheckboxesFormField) formFields.stream()
        .filter(r -> r.getId() == FORM_ID).findFirst().orElse(null);
    assertThat(formField.getType(), is(type));
    assertThat(formField.getHelpText(), is(helpText));

    Map<String, Object> attributes = formField.getAttributes();
    assertThat(attributes.get(ACTIONS_KEY), is(actions));
  }
}
