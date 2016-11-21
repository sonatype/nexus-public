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
package org.sonatype.nexus.internal.backup

import java.util.concurrent.Callable

import org.sonatype.goodies.common.MultipleFailures.MultipleFailuresException
import org.sonatype.nexus.common.node.NodeAccess

import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

/**
 * Tests for {@link DatabaseBackupTask}
 */
class DatabaseBackupTaskTest
    extends Specification
{

  def 'task should be able to identify if it is on the right node in a cluster'() {
    setup:
      def databaseBackup = Mock(DatabaseBackup)
      def nodeAccess = Mock(NodeAccess)
      def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'it is clustered setup and the right node'
      nodeAccess.isClustered() >> true
      nodeAccess.id >> 'myId'
      dbBackupTask.nodeId = 'myId'

    then: 'task should want to run'
      assertThat(dbBackupTask.runOnMe(), is(true))
  }

  def 'task should be able to identify that it does not run the task in a cluster'() {
    setup:
      def databaseBackup = Mock(DatabaseBackup)
      def nodeAccess = Mock(NodeAccess)
      def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'it is clustered setup and the wrong node'
      nodeAccess.isClustered() >> true
      nodeAccess.id >> 'myId'
      dbBackupTask.nodeId = 'notMyId'
      nodeAccess.memberIds >> ['notMyId']

    then: 'task should want to run'
      assertThat(dbBackupTask.runOnMe(), is(false))
  }

  def 'task should know that it is not in a cluster and should run'() {
    setup:
      def databaseBackup = Mock(DatabaseBackup)
      def nodeAccess = Mock(NodeAccess)
      def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'it is not clustered setup'
      nodeAccess.isClustered() >> false

    then: 'task should want to run'
      assertThat(dbBackupTask.runOnMe(), is(true))
  }

  def 'task should identify that it went from unclustered to clustered and complain'() {
    setup:
      def databaseBackup = Mock(DatabaseBackup)
      def nodeAccess = Mock(NodeAccess)
      def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'it is now a clustered setup, but nodeId was never specified'
      nodeAccess.isClustered() >> true
      dbBackupTask.nodeId = null
      dbBackupTask.runOnMe()

    then: 'should throw an exception'
      thrown IllegalStateException
  }

  def 'task should identify when the configured node is not in the cluster and complain'() {
    setup:
      def databaseBackup = Mock(DatabaseBackup)
      def nodeAccess = Mock(NodeAccess)
      def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'it is a cluster, but the correct node is not found'
      nodeAccess.isClustered() >> true
      nodeAccess.getId() >> 'notMyId'
      nodeAccess.getMemberIds() >> ['notMyId']
      dbBackupTask.nodeId = 'myId'
      dbBackupTask.runOnMe()

    then: 'should throw an exception'
      thrown IllegalStateException
  }

  def 'task should execute properly'() {
    def databaseBackup = Mock(DatabaseBackup)
    def nodeAccess = Mock(NodeAccess)
    def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup service should be called appropriately'
      1 * nodeAccess.isClustered() >> false
      1 * databaseBackup.dbNames() >> ['test']
      1 * databaseBackup.fullBackup('target', 'test') >> dumbBackupJob
  }

  def 'task should fail somewhat gracefully if the file cannot be created'() {
    def databaseBackup = Mock(DatabaseBackup)
    def nodeAccess = Mock(NodeAccess)
    def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup service should be called appropriately'
      1 * nodeAccess.isClustered() >> false
      1 * databaseBackup.dbNames() >> ['test']
      1 * databaseBackup.fullBackup('target', 'test') >> { String backupFolder, String dbName ->
        throw new IOException("mocked exception")
      }
      thrown(MultipleFailuresException)
  }

  def 'task should try to backup all files when told to do so'() {
    def databaseBackup = Mock(DatabaseBackup)
    def nodeAccess = Mock(NodeAccess)
    def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup service should be called appropriately'
      1 * nodeAccess.isClustered() >> false
      1 * databaseBackup.dbNames() >> ['test1', 'test2']
      1 * databaseBackup.fullBackup('target', 'test1') >> dumbBackupJob
      1 * databaseBackup.fullBackup('target', 'test2') >> dumbBackupJob
      notThrown(MultipleFailuresException)
  }

  def 'task should be okay if one file fails, but others can work'() {
    def databaseBackup = Mock(DatabaseBackup)
    def nodeAccess = Mock(NodeAccess)
    def dbBackupTask = new DatabaseBackupTask(nodeAccess, databaseBackup)

    when: 'the task is executed with good values'
      dbBackupTask.location = 'target'
      dbBackupTask.execute()

    then: 'the databaseBackup service should be called appropriately'
      1 * nodeAccess.isClustered() >> false
      1 * databaseBackup.dbNames() >> ['test1', 'test2']
      1 * databaseBackup.fullBackup('target', 'test1') >> { String backupFolder, String dbName ->
        throw new IOException("mocked exception")
      }
      1 * databaseBackup.fullBackup('target','test2') >> dumbBackupJob
      thrown(MultipleFailuresException)
  }

  def dumbBackupJob = new Callable<Void>() {
    @Override
    Void call() throws Exception {
      null
    }
  }

}
