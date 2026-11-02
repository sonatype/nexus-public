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
package org.sonatype.nexus.repository.manager;

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.common.app.trash.TrashContext;
import org.sonatype.nexus.common.app.trash.TrashFormatGate;
import org.sonatype.nexus.common.app.trash.TrashSchemaVersions;
import org.sonatype.nexus.common.app.trash.TrashedEntityService;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertThrows;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RepositoryDeletionServiceTest
{
  private static final String REPO = "my-repo";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private TrashedEntityService repositoryTrashService;

  @Mock
  private DatabaseCheck databaseCheck;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  @Mock
  private Type type;

  @Mock
  private Subject subject;

  @Before
  public void setUp() {
    when(repositoryManager.get(REPO)).thenReturn(repository);
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    when(repository.getType()).thenReturn(type);
    when(securityHelper.subject()).thenReturn(subject);
    when(subject.getPrincipal()).thenReturn("admin");
    when(repositoryTrashService.getEntityType()).thenReturn("repository");
  }

  private RepositoryDeletionService service(
      final boolean trashEnabled,
      final List<TrashedEntityService> services)
  {
    // Allowlist the setUp format (maven2) so "flag on" exercises the trash path under the
    // fail-closed gate (an empty allowlist would hard-delete everything).
    return service(trashEnabled, "maven2", services);
  }

  private RepositoryDeletionService service(
      final boolean trashEnabled,
      final String formatsAllowlist,
      final List<TrashedEntityService> services)
  {
    // Columns are assumed present in these tests, so schema-availability does not gate the flag path.
    when(databaseCheck.isAtLeast(TrashSchemaVersions.REPOSITORY)).thenReturn(true);
    RepositoryDeletionService svc = new RepositoryDeletionService(repositoryManager, securityHelper, services,
        new TrashFormatGate(trashEnabled, formatsAllowlist), databaseCheck);
    return svc;
  }

  @Test
  public void hostedRepoWithFlagOnIsTrashed() throws Exception {
    when(type.getValue()).thenReturn(HostedType.NAME);

    service(true, List.of(repositoryTrashService)).delete(REPO);

    verify(securityHelper).ensurePermitted(any(RepositoryAdminPermission.class));
    verify(repositoryTrashService).trash(eq(REPO), any(TrashContext.class));
    verify(repositoryManager, never()).delete(REPO);
  }

  @Test
  public void hostedRepoWithFlagOffIsHardDeleted() throws Exception {
    when(type.getValue()).thenReturn(HostedType.NAME);

    service(false, List.of(repositoryTrashService)).delete(REPO);

    verify(securityHelper).ensurePermitted(any(RepositoryAdminPermission.class));
    verify(repositoryManager).delete(REPO);
    verify(repositoryTrashService, never()).trash(any(), any());
  }

  @Test
  public void proxyRepoWithFlagOnIsHardDeleted() throws Exception {
    when(type.getValue()).thenReturn("proxy");

    service(true, List.of(repositoryTrashService)).delete(REPO);

    verify(repositoryManager).delete(REPO);
    verify(repositoryTrashService, never()).trash(any(), any());
  }

  @Test
  public void hostedRepoWithFlagOnButNoServiceIsHardDeleted() throws Exception {
    when(type.getValue()).thenReturn(HostedType.NAME);

    service(true, Collections.emptyList()).delete(REPO);

    verify(repositoryManager).delete(REPO);
  }

  @Test
  public void hostedRepoWithFlagOnButFormatNotAllowlistedIsHardDeleted() throws Exception {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(format.getValue()).thenReturn("npm");

    // Trash enabled but only maven2 allow-listed -> an npm repo hard-deletes (phased rollout).
    service(true, "maven2", List.of(repositoryTrashService)).delete(REPO);

    verify(repositoryManager).delete(REPO);
    verify(repositoryTrashService, never()).trash(any(), any());
  }

  @Test
  public void hostedRepoWithFlagOnButSchemaNotMigratedIsHardDeleted() throws Exception {
    // ZDU window: flag on and format allow-listed, but repository.deleted_on doesn't exist yet.
    // Must hard-delete rather than attempt a trash write that would fail on the missing column.
    when(databaseCheck.isAtLeast(TrashSchemaVersions.REPOSITORY)).thenReturn(false);
    RepositoryDeletionService svc = new RepositoryDeletionService(repositoryManager, securityHelper,
        List.of(repositoryTrashService), new TrashFormatGate(true, "maven2"), databaseCheck);

    svc.delete(REPO);

    verify(repositoryManager).delete(REPO);
    verify(repositoryTrashService, never()).trash(any(), any());
  }

  @Test
  public void hostedRepoWithFlagOnAndFormatAllowlistedIsTrashed() throws Exception {
    when(type.getValue()).thenReturn(HostedType.NAME);
    when(format.getValue()).thenReturn("maven2");

    service(true, "npm,maven2", List.of(repositoryTrashService)).delete(REPO);

    verify(repositoryTrashService).trash(eq(REPO), any(TrashContext.class));
    verify(repositoryManager, never()).delete(REPO);
  }

  @Test
  public void deletePermissionEnforcedBeforeAnyDelete() throws Exception {
    when(type.getValue()).thenReturn(HostedType.NAME);
    doThrow(new AuthorizationException("not permitted"))
        .when(securityHelper)
        .ensurePermitted(any(RepositoryAdminPermission.class));

    try {
      service(true, List.of(repositoryTrashService)).delete(REPO);
    }
    catch (AuthorizationException expected) {
      // expected
    }

    verify(repositoryTrashService, never()).trash(any(), any());
    verify(repositoryManager, never()).delete(REPO);
  }

  @Test
  public void deleteMissingRepositoryThrowsAuthorizationExceptionWithoutRevealingExistence() throws Exception {
    when(repositoryManager.get("does-not-exist")).thenReturn(null);

    // A missing repository must fail exactly like a forbidden one (AuthorizationException), so existence
    // isn't observable to a caller lacking DELETE permission (NEXUS-51733).
    assertThrows(AuthorizationException.class,
        () -> service(true, List.of(repositoryTrashService)).delete("does-not-exist"));

    verify(repositoryTrashService, never()).trash(any(), any());
    verify(repositoryManager, never()).delete("does-not-exist");
  }
}
