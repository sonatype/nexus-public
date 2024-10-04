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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.groups.Default;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.validation.ConstraintViolations;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.capability.CapabilityDescriptor.ValidationMode.LOAD;
import static org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.capabilities;

/**
 * Support for {@link CapabilityDescriptor} implementations.
 *
 * @since 2.7
 */
public abstract class CapabilityDescriptorSupport<ConfigT>
    extends ComponentSupport
    implements CapabilityDescriptor
{
  private Provider<CapabilityRegistry> capabilityRegistry;

  private boolean exposed = true;

  private boolean hidden = false;

  @Inject
  public void installComponents(final Provider<CapabilityRegistry> capabilityRegistry) {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
  }

  @Override
  public String about() {
    try {
      return renderAbout();
    }
    catch (Exception e) {
      log.warn("Failed to render about", e);
    }
    return null;
  }

  protected String renderAbout() throws Exception {
    return null;
  }

  @Override
  public boolean isExposed() {
    return exposed;
  }

  protected void setExposed(final boolean exposed) {
    this.exposed = exposed;
  }

  @Override
  public boolean isHidden() {
    return hidden;
  }

  protected void setHidden(final boolean hidden) {
    this.hidden = hidden;
  }

  //
  // Version support
  //

  @Override
  public int version() {
    return 1;
  }

  @Override
  public Map<String, String> convert(final Map<String, String> properties, final int fromVersion) {
    return properties;
  }

  private Provider<Validator> validatorProvider;

  @Inject
  public void installValidationComponents(final Provider<Validator> validatorProvider) {
    checkState(this.validatorProvider == null);
    this.validatorProvider = checkNotNull(validatorProvider);
  }

  @Override
  public void validate(
      @Nullable final CapabilityIdentity id,
      final Map<String, String> properties,
      final ValidationMode validationMode)
  {
    validateConfig(properties, validationMode);
    // skip uniqueness check on load; defer to 'HasNoDuplicates' activation condition
    // but keep it for create and update, as we want early validation in those cases
    if (validationMode != LOAD) {
      validateUnique(id, properties);
    }
  }

  @Override
  public boolean isDuplicated(@Nullable final CapabilityIdentity id, final Map<String, String> properties) {
    return !capabilityRegistry.get().get(duplicatesFilter(id, properties)).isEmpty();
  }

  /**
   * Override and return the ids of properties that makes a capability unique. For example to be able to create multiple
   * capabilities of this type, one per repository, return the id of repository field.
   *
   * @return ids of the fields that makes the capability unique.
   */
  @Nullable
  protected Set<String> uniqueProperties() {
    return null;
  }

  protected void validateConfig(final Map<String, String> properties, final ValidationMode validationMode) {
    checkNotNull(properties);
    checkNotNull(validationMode);

    ConfigT config = createConfig(properties);
    if (config != null) {
      validate(config, validationMode.getGroupingClass());
    }
  }

  protected void validate(final Object value, final Class<?> validationGroup) {
    checkNotNull(value);
    checkNotNull(validationGroup);
    List<Class<?>> validationGroups = asList(validationGroup, Default.class);

    if (log.isTraceEnabled()) {
      log.trace("Validating: {} in groups: {}", value, validationGroups);
    }

    Validator validator = validatorProvider.get();
    Set<ConstraintViolation<Object>> violations = validator.validate(value, validationGroups.toArray(new Class<?>[]{}));
    ConstraintViolations.maybePropagate(violations, log);
  }

  private CapabilityReferenceFilter duplicatesFilter(
      @Nullable final CapabilityIdentity id,
      final Map<String, String> properties)
  {
    checkNotNull(properties);
    checkNotNull(capabilityRegistry);

    CapabilityReferenceFilter filter = capabilities().withType(type());
    if (id != null) {
      filter.ignore(id);
    }
    Set<String> uniqueProperties = uniqueProperties();
    if (uniqueProperties != null) {
      for (String key : uniqueProperties) {
        filter.withProperty(key, properties.get(key));
      }
    }

    return filter;
  }

  protected void validateUnique(@Nullable final CapabilityIdentity id, final Map<String, String> properties)
  {
    CapabilityReferenceFilter filter = duplicatesFilter(id, properties);

    log.trace("Validating that unique capability of type {} and properties {}", type(), filter.getProperties());

    Collection<? extends CapabilityReference> references = capabilityRegistry.get().get(filter);
    if (!references.isEmpty()) {
      StringBuilder message = new StringBuilder().append("Only one capability of type '").append(name()).append("'");
      for (Entry<String, String> entry : filter.getProperties().entrySet()) {
        message.append(", ").append(propertyName(entry.getKey()).toLowerCase()).append(" '").append(entry.getValue())
               .append("'");
      }
      message.append(" can be created");
      throw new ValidationException(message.toString());
    }
  }

  protected ConfigT createConfig(final Map<String, String> properties) { return null; }

  private String propertyName(final String key) {
    List<FormField> formFields = formFields();
    if (formFields != null) {
      for (FormField field : formFields) {
        if (Objects.equals(key, field.getId())) {
          return field.getLabel();
        }
      }
    }
    return key;
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
    URL url = getClass().getResource(template);
    return templateHelper.render(url, params);
  }

  protected String render(final String template) {
    return render(template, new TemplateParameters());
  }
}
