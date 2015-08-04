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
package org.sonatype.nexus.plugins.capabilities.internal.storage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.CCapability;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.CCapabilityProperty;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.Configuration;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.io.xpp3.NexusCapabilitiesConfigurationXpp3Reader;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.io.xpp3.NexusCapabilitiesConfigurationXpp3Writer;
import org.sonatype.nexus.util.Tokens;
import org.sonatype.sisu.goodies.common.io.FileReplacer;
import org.sonatype.sisu.goodies.common.io.FileReplacer.ContentWriter;
import org.sonatype.sisu.goodies.lifecycle.LifecycleSupport;

import com.google.common.collect.Maps;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * XML (modello) based {@link CapabilityStorage}.
 *
 * @since 2.9
 */
@Named("xml")
@Singleton
public class XmlCapabilityStorage
    extends LifecycleSupport
    implements CapabilityStorage
{
  private final File configurationFile;

  private final ReentrantLock lock = new ReentrantLock();

  private Configuration configuration;

  @Inject
  public XmlCapabilityStorage(final ApplicationConfiguration applicationConfiguration) {
    this.configurationFile = new File(applicationConfiguration.getConfigurationDirectory(), "capabilities.xml");
  }

  /**
   * @since 2.7
   */
  public File getConfigurationFile() {
    return configurationFile;
  }

  private final AtomicLong identityCounter = new AtomicLong(System.nanoTime());

  private CapabilityIdentity generateIdentity() {
    long id = identityCounter.incrementAndGet();
    byte[] bytes = ByteBuffer.allocate(8).putLong(id).array();
    return new CapabilityIdentity(Tokens.encodeHexString(bytes));
  }

  @Override
  public CapabilityIdentity add(final CapabilityStorageItem item)
      throws IOException
  {
    try {
      lock.lock();

      CapabilityIdentity identity = generateIdentity();
      load().addCapability(convert(identity, item));
      save();
      return identity;
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public boolean update(final CapabilityIdentity identity, final CapabilityStorageItem item)
      throws IOException
  {
    try {
      lock.lock();

      final CCapability capability = convert(identity, item);

      final CCapability stored = getInternal(capability.getId());

      if (stored == null) {
        return false;
      }
      load().removeCapability(stored);
      load().addCapability(capability);
      save();
      return true;
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public boolean remove(final CapabilityIdentity id)
      throws IOException
  {
    try {
      lock.lock();

      final CCapability stored = getInternal(id.toString());
      if (stored == null) {
        return false;
      }
      load().removeCapability(stored);
      save();
      return true;
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public Map<CapabilityIdentity, CapabilityStorageItem> getAll() throws IOException {
    Map<CapabilityIdentity, CapabilityStorageItem> entries = Maps.newHashMap();
    final List<CCapability> capabilities = load().getCapabilities();
    if (capabilities != null) {
      for (final CCapability element : capabilities) {
        CapabilityIdentity identity = new CapabilityIdentity(element.getId());
        entries.put(identity, convert(element));
      }
    }
    return entries;
  }

  private Configuration load()
      throws IOException
  {
    if (configuration != null) {
      return configuration;
    }

    lock.lock();

    Reader fr = null;
    FileInputStream is = null;

    try {
      final Reader r = new FileReader(configurationFile);

      Xpp3DomBuilder.build(r);

      is = new FileInputStream(configurationFile);

      final NexusCapabilitiesConfigurationXpp3Reader reader = new NexusCapabilitiesConfigurationXpp3Reader();

      fr = new InputStreamReader(is);

      configuration = reader.read(fr);
    }
    catch (final FileNotFoundException e) {
      // This is ok, may not exist first time around
      configuration = new Configuration();

      configuration.setVersion(Configuration.MODEL_VERSION);

      save();
    }
    catch (final IOException e) {
      log.error("IOException while retrieving configuration file", e);
    }
    catch (final XmlPullParserException e) {
      log.error("Invalid XML Configuration", e);
    }
    finally {
      IOUtil.close(fr);
      IOUtil.close(is);

      lock.unlock();
    }

    return configuration;
  }

  private void save()
      throws IOException
  {
    lock.lock();

    log.debug("Saving configuration: {}", configurationFile);
    try {
      final FileReplacer fileReplacer = new FileReplacer(configurationFile);
      // we save this file many times, don't litter backups
      fileReplacer.setDeleteBackupFile(true);
      fileReplacer.replace(new ContentWriter()
      {
        @Override
        public void write(final BufferedOutputStream output)
            throws IOException
        {
          new NexusCapabilitiesConfigurationXpp3Writer().write(output, configuration);
        }
      });
    }
    finally {
      lock.unlock();
    }
  }

  private CCapability getInternal(final String capabilityId)
      throws IOException
  {
    if (StringUtils.isEmpty(capabilityId)) {
      return null;
    }

    for (final CCapability element : load().getCapabilities()) {
      if (capabilityId.equals(element.getId())) {
        return element;
      }
    }

    return null;
  }

  private CapabilityStorageItem convert(final CCapability element) {
    final Map<String, String> properties = Maps.newHashMap();
    if (element.getProperties() != null) {
      for (final CCapabilityProperty property : element.getProperties()) {
        properties.put(property.getKey(), property.getValue());
      }
    }

    return new CapabilityStorageItem(
        element.getVersion(),
        element.getTypeId(),
        element.isEnabled(),
        element.getNotes(),
        properties
    );
  }

  private CCapability convert(final CapabilityIdentity identity, final CapabilityStorageItem item) {
    final CCapability element = new CCapability();
    element.setId(identity.toString());
    element.setVersion(item.getVersion());
    element.setTypeId(item.getType());
    element.setEnabled(item.isEnabled());
    element.setNotes(item.getNotes());
    if (item.getProperties() != null) {
      for (Map.Entry<String, String> entry : item.getProperties().entrySet()) {
        final CCapabilityProperty property = new CCapabilityProperty();
        property.setKey(entry.getKey());
        property.setValue(entry.getValue());
        element.addProperty(property);
      }
    }
    return element;
  }
}