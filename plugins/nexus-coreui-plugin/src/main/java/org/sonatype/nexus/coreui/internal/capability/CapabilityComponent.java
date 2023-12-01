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
package org.sonatype.nexus.coreui.internal.capability;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.coreui.FormFieldXO;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.capability.CapabilityIdentity.capabilityIdentity;
import static org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.capabilities;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

// FIXME: update action name after refactor to use coreui_*

/**
 * Capabilities {@link DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "capability_Capability")
public class CapabilityComponent
    extends DirectComponentSupport
    implements StateContributor
{
  private static final CapabilityReferenceFilter ALL_CREATED = capabilities().includeNotExposed();

  private static final CapabilityReferenceFilter ALL_ACTIVE = capabilities().includeNotExposed().active();

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  private final CapabilityRegistry capabilityRegistry;

  @Inject
  public CapabilityComponent(
      final CapabilityDescriptorRegistry capabilityDescriptorRegistry,
      final CapabilityRegistry capabilityRegistry)
  {
    this.capabilityDescriptorRegistry = checkNotNull(capabilityDescriptorRegistry);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
  }

  /**
   * Retrieves capabilities.
   *
   * @return a list of capabilities
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:capabilities:read")
  public List<CapabilityXO> read() {
    capabilityRegistry.pullAndRefreshReferencesFromDB();
    return capabilityRegistry.get(capabilities()).stream().map(this::asCapability).collect(toList());
  }

  /**
   * Retrieve available capabilities types.
   *
   * @return a list of capability types
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:capabilities:read")
  public List<CapabilityTypeXO> readTypes() {
    return Arrays.stream(capabilityDescriptorRegistry.getAll()).filter(CapabilityDescriptor::isExposed)
        .map(this::asCapabilityType).collect(toList());
  }

  /**
   * Creates a capability.
   *
   * @param capabilityXO to be created
   * @return created capability
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:create")
  @Validate(groups = {Create.class, Default.class})
  public CapabilityXO create(final @NotNull @Valid CapabilityXO capabilityXO) {
    return asCapability(capabilityRegistry.add(capabilityType(capabilityXO.getTypeId()), capabilityXO.getEnabled(),
        capabilityXO.getNotes(), capabilityXO.getProperties()));
  }

  /**
   * Updates a capability.
   *
   * @param capabilityXO to be updated
   * @return updated capability
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:update")
  @Validate(groups = {Update.class, Default.class})
  public CapabilityXO update(final @NotNull @Valid CapabilityXO capabilityXO) {
    CapabilityReference capabilityReference = capabilityRegistry.get(capabilityIdentity(capabilityXO.getId()));
    return asCapability(capabilityRegistry.update(capabilityIdentity(capabilityXO.getId()), capabilityXO.getEnabled(),
        capabilityXO.getNotes(),
        unfilterProperties(capabilityXO.getProperties(), capabilityReference.context().properties())));
  }

  /**
   * Updates capability notes.
   *
   * @param capabilityNotesXO to be updated
   * @return updated capability
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:update")
  @Validate(groups = {Update.class, Default.class})
  public CapabilityXO updateNotes(final @NotNull @Valid CapabilityNotesXO capabilityNotesXO) {
    CapabilityReference capabilityReference = capabilityRegistry.get(capabilityIdentity(capabilityNotesXO.getId()));
    return asCapability(
        capabilityRegistry.update(capabilityReference.context().id(), capabilityReference.context().isEnabled(),
            capabilityNotesXO.getNotes(), capabilityReference.context().properties()));
  }

  /**
   * Deletes a capability.
   *
   * @param id of capability to be deleted
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:delete")
  @Validate
  public void remove(final @NotEmpty String id) {
    capabilityRegistry.remove(capabilityIdentity(id));
  }

  /**
   * Enables an existing capability.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:update")
  @Validate
  public void enable(final @NotEmpty String id) {
    capabilityRegistry.enable(capabilityIdentity(id));
  }

  /**
   * Disables an existing capability.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions("nexus:capabilities:update")
  @Validate
  public void disable(final @NotEmpty String id) {
    capabilityRegistry.disable(capabilityIdentity(id));
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of("capabilityCreatedTypes",
        capabilityRegistry.get(ALL_CREATED).stream().map(this::capabilityToType).collect(toSet()),
        "capabilityActiveTypes",
        capabilityRegistry.get(ALL_ACTIVE).stream().map(this::capabilityToType).collect(toSet()));
  }

  private String capabilityToType(final CapabilityReference capabilityReference) {
    return capabilityReference.context().descriptor().type().toString();
  }

  private CapabilityXO asCapability(final CapabilityReference reference) {
    CapabilityDescriptor descriptor = reference.context().descriptor();
    Capability capability = reference.capability();

    CapabilityXO capabilityXO = new CapabilityXO();
    capabilityXO.setId(reference.context().id().toString());
    capabilityXO.setNotes(reference.context().notes());
    capabilityXO.setTypeId(descriptor.type().toString());
    capabilityXO.setTypeName(descriptor.name());
    capabilityXO.setEnabled(reference.context().isEnabled());
    capabilityXO.setActive(reference.context().isActive());
    capabilityXO.setError(reference.context().hasFailure());
    capabilityXO.setState("disabled");
    capabilityXO.setStateDescription(reference.context().stateDescription());
    capabilityXO.setProperties(filterProperties(reference.context().properties(), capability));
    capabilityXO.setDisableWarningMessage(descriptor.getDisableWarningMessage());
    capabilityXO.setDeleteWarningMessage(descriptor.getDeleteWarningMessage());

    if (capabilityXO.getEnabled() && capabilityXO.getError()) {
      capabilityXO.setState("error");
    }
    else if (capabilityXO.getEnabled() && capabilityXO.getActive()) {
      capabilityXO.setState("active");
    }
    else if (capabilityXO.getEnabled() && !capabilityXO.getActive()) {
      capabilityXO.setState("passive");
    }

    if (capability.description() != null) {
      capabilityXO.setDescription(capability.description());
    }

    if (capability.status() != null) {
      capabilityXO.setStatus(capability.status());
    }

    Set<Tag> tags = new HashSet<>();
    if (descriptor instanceof Taggable && ((Taggable) descriptor).getTags() != null) {
      tags.addAll(((Taggable) descriptor).getTags());
    }
    if (capability instanceof Taggable && ((Taggable) capability).getTags() != null) {
      tags.addAll(((Taggable) capability).getTags());
    }
    if (!tags.isEmpty()) {
      capabilityXO.setTags(tags.stream().collect(toMap(Tag::key, Tag::value)));
    }

    return capabilityXO;
  }

  private CapabilityTypeXO asCapabilityType(final CapabilityDescriptor capabilityDescriptor) {
    CapabilityTypeXO capabilityTypeXO = new CapabilityTypeXO();
    capabilityTypeXO.setId(capabilityDescriptor.type().toString());
    capabilityTypeXO.setName(capabilityDescriptor.name());
    capabilityTypeXO.setAbout(capabilityDescriptor.about());
    if (capabilityDescriptor.formFields() != null) {
      capabilityTypeXO.setFormFields(
          capabilityDescriptor.formFields().stream().map(FormFieldXO::create).collect(toList()));
    }
    return capabilityTypeXO;
  }

  private Map<String, String> filterProperties(final Map<String, String> properties, final Capability capability) {
    return properties.entrySet().stream()
        .filter(this::nonNullKeyAndValue)
        .map(entry -> {
          if (capability.isPasswordProperty(entry.getKey())) {
            if ("PKI".equals(properties.get("authenticationType"))) {
              return new SimpleEntry<>(entry.getKey(), "");
            }
            return new SimpleEntry<>(entry.getKey(), PasswordPlaceholder.get());
          }
          return entry;
        }).collect(toMap(Entry::getKey, Entry::getValue));
  }

  private Map<String, String> unfilterProperties(
      final Map<String, String> properties,
      final Map<String, String> referenceProperties)
  {
    return properties.entrySet().stream()
        .filter(this::nonNullKeyAndValue)
        .map(entry -> {
          if (PasswordPlaceholder.is(entry.getValue())) {
            return new SimpleEntry<>(entry.getKey(), referenceProperties.get(entry.getKey()));
          }
          return entry;
        }).collect(toMap(Entry::getKey, Entry::getValue));
  }

  private boolean nonNullKeyAndValue(final Entry<?,?> entry) {
    return entry != null && entry.getKey() != null && entry.getValue() != null;
  }
}
