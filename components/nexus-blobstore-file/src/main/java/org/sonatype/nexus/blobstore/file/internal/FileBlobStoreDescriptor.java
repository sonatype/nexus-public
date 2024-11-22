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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorSupport;
import org.sonatype.nexus.blobstore.BlobStoreUtil;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.rest.ValidationErrorsException;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.BASEDIR;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;
import static org.sonatype.nexus.formfields.FormField.MANDATORY;
import static org.sonatype.nexus.blobstore.BlobStoreSupport.MAX_NAME_LENGTH;

/**
 * A {@link BlobStoreDescriptor} for {@link FileBlobStore}.
 *
 * @since 3.6
 */
@AvailabilityVersion(from = "1.0")
@Named(FileBlobStore.TYPE)
public class FileBlobStoreDescriptor
    extends BlobStoreDescriptorSupport
{
  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("File")
    String name();

    @DefaultMessage("Path")
    String pathLabel();

    @DefaultMessage("An absolute path or a path relative to <data-directory>/blobs")
    String pathHelpText();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final ApplicationDirectories applicationDirectories;

  private final FormField<String> path;

  private final BlobStoreUtil blobStoreUtil;

  private final FileBlobStorePathValidator pathValidator;

  @Inject
  public FileBlobStoreDescriptor(
      final BlobStoreQuotaService quotaService,
      final ApplicationDirectories applicationDirectories,
      final BlobStoreUtil blobStoreUtil,
      final FileBlobStorePathValidator pathValidator)
  {
    super(quotaService);
    this.applicationDirectories = applicationDirectories;
    this.blobStoreUtil = blobStoreUtil;
    this.pathValidator = pathValidator;
    this.path = new StringTextFormField(PATH_KEY, messages.pathLabel(), messages.pathHelpText(), MANDATORY)
        .withAttribute("tokenReplacement", applicationDirectories.getWorkDirectory("blobs") + "/${name}")
        .withAttribute("long", Boolean.TRUE);
  }

  @Override
  public String getId() {
    return "file";
  }

  @Override
  public String getName() {
    return messages.name();
  }

  @Override
  public List<FormField> getFormFields() {
    return asList(path);
  }

  @Override
  public void validateConfig(final BlobStoreConfiguration config) {
    super.validateConfig(config);

    Path path = Optional.ofNullable(config.attributes(CONFIG_KEY).get(PATH_KEY, String.class))
        .filter(s -> blobStoreUtil.validateFilePath(s, MAX_NAME_LENGTH))
        .map(s -> Paths.get(s))
        .orElseThrow(() -> new ValidationErrorsException(
            format("The maximum name length for any folder in the path is %d.", MAX_NAME_LENGTH)));

    pathValidator.validatePathUniqueConstraint(config);

    try {
      if (!path.isAbsolute()) {
        Path baseDir = applicationDirectories.getWorkDirectory(BASEDIR).toPath().toRealPath().normalize();
        path = baseDir.resolve(path.normalize());
      }

      if (!Files.isDirectory(path)) {
        Files.createDirectories(path);
      }
      else if (Files.isRegularFile(path)) {
        throw new ValidationErrorsException(
            format("Blob store could not be written to path '%s' because it is a file not a directory", path));
      }
    }
    catch (IOException e) {
      throw new ValidationErrorsException(
          format("Blob store could not be written because the path %s could not be written to", path), e);
    }

    if (!Files.isWritable(path)) {
      throw new ValidationErrorsException(
          format("Blob store could not be written to path '%s' because it was not writable", path));
    }
  }
}
