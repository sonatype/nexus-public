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
package org.sonatype.nexus.internal.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * {@link ScriptTask} descriptor.
 *
 * @since 3.0
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class ScriptTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "script";

  public static final String LANGUAGE = "language";

  public static final String SOURCE = "source";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Admin - Execute script")
    String name();

    @DefaultMessage("Language")
    String languageLabel();

    @DefaultMessage("Script language")
    String languageHelpText();

    @DefaultMessage("Source")
    String sourceLabel();

    @DefaultMessage("Script source")
    String sourceHelpText();
  }

  private static final Messages messages = I18N.create(Messages.class);

  // TODO: validate language is registered?  Maybe use combo to only allow selection of valid languages?

  // TODO: validate source is valid?  This may not be easy to do or reliable w/o actually executing?

  // TODO: this task may expose a lot of potential for misuse, and may need to be optional enabled by system property

  @Inject
  public ScriptTaskDescriptor(final NodeAccess nodeAccess, @Named("${nexus.scripts.allowCreation:-false}") boolean allowCreation) {
    super(TYPE_ID,
        ScriptTask.class,
        messages.name(),
        VISIBLE,
        isExposed(allowCreation),
        new StringTextFormField(
            LANGUAGE,
            messages.languageLabel(),
            messages.languageHelpText(),
            FormField.MANDATORY
        ).withInitialValue(ScriptEngineManagerProvider.DEFAULT_LANGUAGE),
        new TextAreaFormField(
            SOURCE,
            messages.sourceLabel(),
            messages.sourceHelpText(),
            FormField.MANDATORY,
            null,
            !allowCreation
        ),
        nodeAccess.isClustered() ? newMultinodeFormField() : null);
  }

  /**
   * If the allowCreation flag is false we don't want this task exposed to user, but still want
   * existing scripts runnable
   */
  private static boolean isExposed(boolean allowCreation){
    return allowCreation;
  }
}
