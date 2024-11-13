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
package org.sonatype.nexus.capability;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.condition.Conditions;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.template.TemplateThrowableAdapter;
import org.sonatype.nexus.common.text.Strings2;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support for {@link Capability} implementations.
 *
 * @since 2.7
 */
public abstract class CapabilitySupport<ConfigT>
    extends ComponentSupport
    implements Capability
{
  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("FAILED: %s")
    String failure(String failureType);
  }

  private static final Messages messages = I18N.create(Messages.class);

  private CapabilityContext context;

  private ConfigT config;

  @Override
  public void init(final CapabilityContext context) {
    this.context = checkNotNull(context);
  }

  protected CapabilityContext context() {
    return context;
  }

  @Override
  public String status() {
    // If there is a failure render it
    if (context.hasFailure()) {
      return renderFailure(context.failure());
    }

    // Otherwise attempt to render normal status
    try {
      return renderStatus();
    }
    catch (Exception e) {
      log.warn("Failed to render status", e);
    }
    return null;
  }

  @Nullable
  protected String renderStatus() throws Exception {
    return null;
  }

  @Override
  public String description() {
    if (context.hasFailure()) {
      return messages.failure(context.failure().getClass().getName());
    }
    try {
      return renderDescription();
    }
    catch (Exception e) {
      log.warn("Failed to render description", e);
    }
    return null;
  }

  @Nullable
  protected String renderDescription() throws Exception {
    return null;
  }

  protected boolean isConfigured() {
    return config != null;
  }

  protected void ensureConfigured() {
    checkState(config != null, "Capability is not configured");
  }

  protected abstract ConfigT createConfig(final Map<String, String> properties) throws Exception;

  public ConfigT getConfig() {
    ensureConfigured();
    return config;
  }

  protected void configure(final ConfigT config) throws Exception {
    // nop
  }

  private void logLifecycle(final String msg, final ConfigT config) {
    if (log.isTraceEnabled()) {
      log.trace("{}: {}", msg, config);
    }
    else if (log.isDebugEnabled()) {
      log.debug(msg);
    }
  }

  @Override
  public void onCreate() throws Exception {
    checkState(config == null);
    config = createConfig(context().properties());
    logLifecycle("Creating", config);
    onCreate(config);
  }

  protected void onCreate(final ConfigT config) throws Exception {
    configure(config);
  }

  @Override
  public void onLoad() throws Exception {
    checkState(config == null);
    config = createConfig(context().properties());
    logLifecycle("Loading", config);
    onLoad(config);
  }

  protected void onLoad(final ConfigT config) throws Exception {
    configure(config);
  }

  @Override
  public void onUpdate() throws Exception {
    config = createConfig(context().properties());
    logLifecycle("Updating", config);
    onUpdate(config);
  }

  protected void onUpdate(final ConfigT config) throws Exception {
    configure(config);
  }

  @Override
  public void onRemove() throws Exception {
    ensureConfigured();
    logLifecycle("Removing", config);
    onRemove(config);
  }

  protected void onRemove(final ConfigT config) throws Exception {
    // nop
  }

  @Override
  public void onActivate() throws Exception {
    ensureConfigured();
    logLifecycle("Activating", config);
    onActivate(config);
  }

  protected void onActivate(final ConfigT config) throws Exception {
    // nop
  }

  @Override
  public void onPassivate() throws Exception {
    ensureConfigured();
    logLifecycle("Passivating", config);
    onPassivate(config);
  }

  protected void onPassivate(final ConfigT config) throws Exception {
    // nop
  }

  @Override
  public Condition activationCondition() {
    // no condition
    return null;
  }

  @Override
  public Condition validityCondition() {
    // no condition
    return null;
  }

  @Override
  public String toString() {
    String id = null;
    if (context != null) {
      id = "'" + context.id().toString() + "'";
    }
    return getClass().getSimpleName() + "{" +
        "id=" + id +
        ", config=" + config +
        '}';
  }

  //
  // Conditions support
  //

  private Conditions conditions;

  @Inject
  public void installConditionComponents(final Conditions conditions) {
    checkState(this.conditions == null);
    this.conditions = checkNotNull(conditions);
  }

  protected Conditions conditions() {
    checkState(conditions != null);
    return conditions;
  }

  //
  // Template support
  //

  private TemplateHelper templateHelper;

  @Inject
  public void setTemplateHelper(final TemplateHelper templateHelper) {
    this.templateHelper = checkNotNull(templateHelper);
  }

  protected String render(final String template, final TemplateParameters params) {
    // we need to check superclass in case subclass has no template e.g. failure.vm template
    URL url = Optional.ofNullable(getClass().getResource(template))
        .orElseGet(() -> getClass().getSuperclass().getResource(template));
    if (url == null) {
      log.warn("Template not found: {}. Could not render.", template);
      return null;
    }
    return templateHelper.render(url, params);
  }

  /**
   * @since 3.0
   */
  protected String render(final String template, final Map<String,Object> params) {
    return render(template, new TemplateParameters(params));
  }

  protected String render(final String template) {
    return render(template, new TemplateParameters());
  }

  protected String renderFailure(final Throwable cause) {
    return render("failure.vm", new TemplateParameters()
            .set("cause", new TemplateThrowableAdapter(cause))
    );
  }

  @Override
  public boolean isPasswordProperty(final String propertyName) {
    return Strings2.lower(propertyName).contains("password");
  }
}
