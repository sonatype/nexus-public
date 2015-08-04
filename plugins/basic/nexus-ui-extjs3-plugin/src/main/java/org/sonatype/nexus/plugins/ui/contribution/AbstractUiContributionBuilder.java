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
package org.sonatype.nexus.plugins.ui.contribution;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for UI contribution builders
 *
 * @see org.sonatype.nexus.plugins.ui.contribution.UiContributionBuilder
 * @since 2.5
 */
public abstract class AbstractUiContributionBuilder<T>
    extends ComponentSupport
{

  protected final Object owner;

  protected final String groupId;

  protected final String artifactId;

  protected String encoding = "UTF-8";

  public AbstractUiContributionBuilder(final Object owner, final String groupId, final String artifactId) {
    this.owner = checkNotNull(owner);
    this.groupId = checkNotNull(groupId);
    this.artifactId = checkNotNull(artifactId);
  }

  /**
   * Attempt to detect version from the POM of owner.
   */
  private String detectVersion() {
    Properties props = new Properties();

    String path = String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
    InputStream input = owner.getClass().getResourceAsStream(path);

    if (input == null) {
      log.warn("Unable to detect version; failed to load: {}", path);
      return null;
    }

    try {
      props.load(input);
    }
    catch (IOException e) {
      log.warn("Failed to load POM: {}", path, e);
      return null;
    }

    return props.getProperty("version");
  }

  /**
   * Attempt to detect timestamp of the file referenced by the path. If the path is resolved from a jar, the jar
   * timestamp is used. If the path is resolved from filesystem directly, as is the case for exploded plugins for
   * example, the file timestamp is used. If the path is resolved from other sources or cannot be resolved, current
   * timestamp is used.
   */
  private long getTimestamp(String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    URL url = owner.getClass().getResource(path);
    if (url != null) {
      if ("file".equalsIgnoreCase(url.getProtocol())) {
        return new File(url.getFile()).lastModified();
      }
      String resolvedPath = url.toExternalForm();
      if (resolvedPath.toLowerCase().startsWith("jar:file:")) {
        resolvedPath = resolvedPath.substring("jar:file:".length());
        resolvedPath = resolvedPath.substring(0, resolvedPath.length() - path.length() - 1);
        File file = new File(resolvedPath);
        if (file.exists()) {
          return file.lastModified();
        }
      }
    }
    return System.currentTimeMillis();
  }

  /**
   * Return a string suitable for use as suffix to a plain-URL to enforce version/caching semantics.
   */
  public String getCacheBuster(String path) {
    String version = detectVersion();
    if (version == null) {
      return "";
    }
    else if (version.endsWith("SNAPSHOT")) {
      // append timestamp for SNAPSHOT versions to help sort out cache problems
      return String.format("?v=%s&t=%s", version, getTimestamp(path));
    }
    else {
      return "?v=" + version;
    }
  }

  /**
   * Returns the default relative url for the given extension.
   *
   * @param extension The file extension.
   * @param bust      Whether to append cachebuster parameters
   * @return A relative url of the form "static/$extension/$artifactId-all.$extension".
   */
  public String getDefaultPath(final String extension, final boolean bust) {
    final String path = String.format("static/%s/%s-all.%s", extension, artifactId, extension);
    if (bust) {
      return path + getCacheBuster(path);
    }
    return path;
  }

  public String getDefaultPath(final String extension) {
    return getDefaultPath(extension, true);
  }

  public abstract T build();
}
