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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.Entity;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

import com.google.common.base.Throwables;
import org.apache.commons.beanutils.BeanUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Jersey {@link Entity} entity support.
 *
 * @since 2.3
 */
public abstract class JerseyEntitySupport<E extends Entity<E>, S>
    extends SubsystemSupport<JerseyNexusClient>
    implements Entity<E>
{

  private final String id;

  private final S settings;

  private boolean shouldCreate;

  public JerseyEntitySupport(final JerseyNexusClient nexusClient,
                             final String id)
  {
    super(nexusClient);
    this.id = id;
    this.settings = checkNotNull(createSettings(id));
    this.shouldCreate = true;
  }

  public JerseyEntitySupport(final JerseyNexusClient nexusClient,
                             final String id,
                             final S settings)
  {
    this(nexusClient, checkNotNull(id));
    this.shouldCreate = false;
    overwriteWith(settings);
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public synchronized E refresh() {
    S processed = null;

    if (!shouldCreate) {
      processed = doGet();
    }

    shouldCreate = processed == null;

    overwriteWith(processed);

    return me();
  }

  @Override
  public synchronized E save() {
    final S processed;
    if (shouldCreate) {
      processed = doCreate();
    }
    else {
      processed = doUpdate();
    }
    overwriteWith(processed);
    shouldCreate = false;
    return me();
  }

  @Override
  public synchronized E remove() {
    doRemove();
    shouldCreate = true;
    return me();
  }

  @Override
  public String toString() {
    return String.format("%s{id=%s}", getClass().getSimpleName(), id());
  }

  public void overwriteWith(final S source) {
    try {
      BeanUtils.copyProperties(settings(), source == null ? checkNotNull(createSettings(id)) : source);
    }
    catch (final Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected S settings() {
    return settings;
  }

  protected boolean shouldCreate() {
    return shouldCreate;
  }

  @SuppressWarnings("unchecked")
  private E me() {
    return (E) this;
  }

  protected abstract S createSettings(final String id);

  protected abstract S doGet();

  protected abstract S doCreate();

  protected abstract S doUpdate();

  protected abstract void doRemove();

}
