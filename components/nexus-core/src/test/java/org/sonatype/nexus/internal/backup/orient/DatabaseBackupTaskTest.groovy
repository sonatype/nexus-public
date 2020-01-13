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
package org.sonatype.nexus.internal.backup.orient

import java.util.concurrent.Callable

import org.sonatype.goodies.common.MultipleFailures.MultipleFailuresException
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService
import org.sonatype.nexus.orient.freeze.FreezeRequest
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType
import org.sonatype.nexus.scheduling.TaskConfiguration

import spock.lang.Specification

/**
 * Tests for {@link DatabaseBackupTask}
 */
class DatabaseBackupTaskTest
    extends Specification
{

  def databaseBackup = Mock(DatabaseBackup)
  def freezeService = Mock(DatabaseFreezeService)
  def mockRequest = new FreezeRequest(InitiatorType.SYSTEM, "DatabaseBackupTaskTest")

  def 'task should execute properly'() {
    def dbBackupTask = new DatabaseBackupTask(databaseBackup, freezeService)
    dbBackupTask.configure(taskConfiguration())
    freezeService.requestFreeze(_, _) >> mockRequest
    freezeService.releaseRequest(mockRequest) >> true

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup and freeze services should be called appropriately'
      1 * databaseBackup.dbNames() >> ['test']
      1 * databaseBackup.fullBackup('target', 'test', _) >> dumbBackupJob
  }

  def 'task should fail somewhat gracefully if the file cannot be created'() {
    def dbBackupTask = new DatabaseBackupTask(databaseBackup, freezeService)
    dbBackupTask.configure(taskConfiguration())
    freezeService.requestFreeze(_, _) >> mockRequest
    freezeService.releaseRequest(mockRequest) >> true

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup service should be called appropriately'
      1 * databaseBackup.dbNames() >> ['test']
      1 * databaseBackup.fullBackup('target', 'test', _) >> { String backupFolder, String dbName ->
        throw new IOException("mocked exception")
      }
      thrown(MultipleFailuresException)
  }

  def 'task should try to backup all files when told to do so'() {
    def dbBackupTask = new DatabaseBackupTask(databaseBackup, freezeService)
    dbBackupTask.configure(taskConfiguration())
    freezeService.requestFreeze(_, _) >> mockRequest
    freezeService.releaseRequest(mockRequest) >> true

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup service should be called appropriately'
      1 * databaseBackup.dbNames() >> ['test1', 'test2']
      1 * databaseBackup.fullBackup('target', 'test1', _) >> dumbBackupJob
      1 * databaseBackup.fullBackup('target', 'test2', _) >> dumbBackupJob
      notThrown(MultipleFailuresException)
  }

  def 'task should be okay if one file fails, but others can work'() {
    def dbBackupTask = new DatabaseBackupTask(databaseBackup, freezeService)
    dbBackupTask.configure(taskConfiguration())
    freezeService.requestFreeze(_, _) >> mockRequest
    freezeService.releaseRequest(mockRequest) >> true

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup service should be called appropriately'
      1 * databaseBackup.dbNames() >> ['test1', 'test2']
      1 * databaseBackup.fullBackup('target', 'test1', _) >> { String backupFolder, String dbName ->
        throw new IOException("mocked exception")
      }
      1 * databaseBackup.fullBackup('target','test2', _) >> dumbBackupJob
      MultipleFailuresException exception = thrown()
      exception.getFailures().get(0).getMessage().contains('please check filesystem permissions')
  }

  def 'failure to release freeze is treated as a task failure'() {
    def dbBackupTask = new DatabaseBackupTask(databaseBackup, freezeService)
    dbBackupTask.configure(taskConfiguration())
    freezeService.requestFreeze(_, _) >> mockRequest
    freezeService.releaseRequest(mockRequest) >> false

    when: 'the task is executed'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'backup completes ok, but release is captured'
      1 * databaseBackup.dbNames() >> ['test']
      1 * databaseBackup.fullBackup('target', 'test', _) >> dumbBackupJob
      MultipleFailuresException exception = thrown()
      exception.getFailures().get(0).getMessage().startsWith('failed to automatically release read-only state')
  }

  def dumbBackupJob = new Callable<Void>() {
    @Override
    Void call() throws Exception {
      null
    }
  }

  TaskConfiguration taskConfiguration() {
    def config = new TaskConfiguration()
    config.setId('DatabaseBackupTaskTest')
    config.setTypeId(DatabaseBackupTaskDescriptor.TYPE_ID)
    config.setName('backup')
    return config
  }
}
