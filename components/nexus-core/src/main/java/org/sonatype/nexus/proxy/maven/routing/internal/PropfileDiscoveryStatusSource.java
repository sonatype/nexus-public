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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryStatus;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryStatus.DStatus;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class to persist discovery results. This is a simple implementation that uses {@link Properties} file, and
 * stores it in {@link MavenProxyRepository}'s local storage.
 *
 * @author cstamas
 * @since 2.4
 */
public class PropfileDiscoveryStatusSource
{
  private static final String DISCOVERY_STATUS_FILE_PATH = "/.meta/discovery-status.txt";

  private static final String LAST_DISCOVERY_STATUS_KEY = "lastDiscoveryStatus";

  private static final String LAST_DISCOVERY_STRATEGY_KEY = "lastDiscoveryStrategy";

  private static final String LAST_DISCOVERY_MESSAGE_KEY = "lastDiscoveryMessage";

  private static final String LAST_DISCOVERY_TIMESTAMP_KEY = "lastDiscoveryTimestamp";

  private final MavenProxyRepository mavenProxyRepository;

  /**
   * Constructor.
   */
  public PropfileDiscoveryStatusSource(final MavenProxyRepository mavenProxyRepository) {
    this.mavenProxyRepository = checkNotNull(mavenProxyRepository);
  }

  /**
   * Returns {@code true} if "last" results exists, or {@code false} if never run discovery yet.
   *
   * @return {@code true} if "last" results exists, or {@code false} if never run discovery yet.
   */
  public boolean exists() {
    try {
      return getFileItem() != null;
    }
    catch (IOException e) {
      // bam
    }
    return false;
  }

  /**
   * Reads up the last discovery status.
   *
   * @return last discovery status.
   */
  public DiscoveryStatus read()
      throws IOException
  {
    final StorageFileItem file = getFileItem();
    if (file == null) {
      return null;
    }

    final Properties props = new Properties();
    try (final InputStream inputStream = file.getInputStream()) {
      props.load(inputStream);
      final DStatus lastDiscoveryStatus = DStatus.valueOf(props.getProperty(LAST_DISCOVERY_STATUS_KEY));
      final String lastDiscoveryStrategy = props.getProperty(LAST_DISCOVERY_STRATEGY_KEY, "unknown");
      final String lastDiscoveryMessage = props.getProperty(LAST_DISCOVERY_MESSAGE_KEY, "");
      final long lastDiscoveryTimestamp =
          Long.parseLong(props.getProperty(LAST_DISCOVERY_TIMESTAMP_KEY, Long.toString(-1L)));

      return new DiscoveryStatus(lastDiscoveryStatus, lastDiscoveryStrategy, lastDiscoveryMessage,
          lastDiscoveryTimestamp);
    }
    catch (IllegalArgumentException e) {
      deleteFileItem();
      return null;
    }
    catch (NullPointerException e) {
      deleteFileItem();
      return null;
    }
  }

  /**
   * Persists last discovery status.
   */
  public void write(final DiscoveryStatus discoveryStatus)
      throws IOException
  {
    checkNotNull(discoveryStatus);
    final Properties props = new Properties();
    props.put(LAST_DISCOVERY_STATUS_KEY, discoveryStatus.getStatus().name());
    props.put(LAST_DISCOVERY_STRATEGY_KEY, discoveryStatus.getLastDiscoveryStrategy());
    props.put(LAST_DISCOVERY_MESSAGE_KEY, discoveryStatus.getLastDiscoveryMessage());
    props.put(LAST_DISCOVERY_TIMESTAMP_KEY, Long.toString(discoveryStatus.getLastDiscoveryTimestamp()));

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    props.store(bos, "Nexus discovery status");
    putFileItem(new ByteArrayContentLocator(bos.toByteArray(), "text/plain"));
  }

  /**
   * Deletes last discovery status.
   */
  public void delete()
      throws IOException
  {
    deleteFileItem();
  }

  // ==

  protected MavenProxyRepository getMavenProxyRepository() {
    return mavenProxyRepository;
  }

  protected StorageFileItem getFileItem()
      throws IOException
  {
    try {
      final ResourceStoreRequest request = new ResourceStoreRequest(DISCOVERY_STATUS_FILE_PATH);
      request.setRequestLocalOnly(true);
      request.setRequestGroupLocalOnly(true);
      @SuppressWarnings("deprecation")
      final StorageItem item = getMavenProxyRepository().retrieveItem(true, request);
      if (item instanceof StorageFileItem) {
        return (StorageFileItem) item;
      }
      else {
        return null;
      }
    }
    catch (IllegalOperationException e) {
      // eh?
      return null;
    }
    catch (ItemNotFoundException e) {
      // not present
      return null;
    }
  }

  protected void putFileItem(final ContentLocator content)
      throws IOException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(DISCOVERY_STATUS_FILE_PATH);
    request.setRequestLocalOnly(true);
    request.setRequestGroupLocalOnly(true);
    final DefaultStorageFileItem file =
        new DefaultStorageFileItem(getMavenProxyRepository(), request, true, true, content);
    try {
      getMavenProxyRepository().storeItem(true, file);
    }
    catch (UnsupportedStorageOperationException e) {
      // eh?
    }
    catch (IllegalOperationException e) {
      // eh?
    }
  }

  protected void deleteFileItem()
      throws IOException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(DISCOVERY_STATUS_FILE_PATH);
    request.setRequestLocalOnly(true);
    request.setRequestGroupLocalOnly(true);
    try {
      getMavenProxyRepository().deleteItemWithChecksums(true, request);
    }
    catch (ItemNotFoundException e) {
      // ignore
    }
    catch (UnsupportedStorageOperationException e) {
      // eh?
    }
    catch (IllegalOperationException e) {
      // ignore
    }
  }
}
