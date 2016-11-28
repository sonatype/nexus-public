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
package org.sonatype.nexus.quartz.internal.orient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.ORecordNotFoundException;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State.ACQUIRED;
import static org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State.BLOCKED;
import static org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State.COMPLETE;
import static org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State.ERROR;
import static org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State.PAUSED;
import static org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State.PAUSED_BLOCKED;
import static org.sonatype.nexus.quartz.internal.orient.TriggerEntity.State.WAITING;
import static org.sonatype.nexus.scheduling.TaskDescriptorSupport.LIMIT_NODE_KEY;

/**
 * Orient {@link JobStore}.
 *
 * @since 3.0
 */
@Named("orient")
@Singleton
public class JobStoreImpl
    extends ComponentSupport
    implements JobStore
{
  private static final String NODE_ID = "node.identity";

  private final Provider<DatabaseInstance> databaseInstance;

  private final JobDetailEntityAdapter jobDetailEntityAdapter;

  private final TriggerEntityAdapter triggerEntityAdapter;

  private final CalendarEntityAdapter calendarEntityAdapter;

  private final NodeAccess nodeAccess;

  private SchedulerSignaler signaler;

  private String instanceName;

  private String instanceId;

  @Inject
  public JobStoreImpl(@Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> databaseInstance,
                      final JobDetailEntityAdapter jobDetailEntityAdapter,
                      final TriggerEntityAdapter triggerEntityAdapter,
                      final CalendarEntityAdapter calendarEntityAdapter,
                      final NodeAccess nodeAccess)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.jobDetailEntityAdapter = checkNotNull(jobDetailEntityAdapter);
    this.triggerEntityAdapter = checkNotNull(triggerEntityAdapter);
    this.calendarEntityAdapter = checkNotNull(calendarEntityAdapter);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  public boolean supportsPersistence() {
    return true;
  }

  @Override
  public boolean isClustered() {
    return nodeAccess.isClustered();
  }

  @Override
  public void setInstanceName(final String instanceName) {
    // scheduler-name (ie. Nexus)
    this.instanceName = instanceName;
  }

  @Override
  public void setInstanceId(final String instanceId) {
    // scheduler-id (ie. local node-id)
    this.instanceId = instanceId;
  }

  /**
   * Ignored.
   */
  @Override
  public void setThreadPoolSize(final int poolSize) {
    // ignore
  }

  //
  // Database
  //

  /**
   * Database operation which returns a value.
   *
   * Return value may be {@code null} if return type of caller is void.
   */
  private interface Operation<T>
  {
    @Nullable
    T execute(ODatabaseDocumentTx db) throws JobPersistenceException;
  }

  private final Object monitor = new Object();

  /**
   * Execute operation within transaction and propagate/translate exceptions.
   */
  private <T> T execute(final Operation<T> operation) throws JobPersistenceException {
    try {
      synchronized (monitor) {
        return inTx(databaseInstance)
            .retryOn(ONeedRetryException.class, ORecordNotFoundException.class)
            .throwing(JobPersistenceException.class)
            .call(operation::execute);
      }
    }
    catch (Exception e) {
      log.warn("Execution failed", e);
      Throwables.propagateIfPossible(e, JobPersistenceException.class);
      throw new JobPersistenceException(e.toString(), e);
    }
  }

  /**
   * Execute operation and propagate exceptions.
   *
   * Most {@link JobStore} declares throwing {@link JobPersistenceException}.
   * This is used for the few places that do not confirm, but can produce exceptions.
   */
  private <T> T executeAndPropagate(final Operation<T> operation) {
    try {
      return execute(operation);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // Lifecycle
  //

  @Override
  public void initialize(final ClassLoadHelper loadHelper, final SchedulerSignaler signaler)
      throws SchedulerConfigException
  {
    log.info("Instance name: {}; ID: {}", instanceName, instanceId);

    // TODO: Should we consider using ClassLoadHelper?
    this.signaler = checkNotNull(signaler);

    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      jobDetailEntityAdapter.register(db);
      triggerEntityAdapter.register(db);
      calendarEntityAdapter.register(db);
    }

    log.info("Initialized");
  }

  /**
   * Ignored.
   */
  @Override
  public void schedulerStarted() throws SchedulerException {
    execute(
        db -> {
          // check state of local triggers
          for (TriggerEntity triggerEntity : local(triggerEntityAdapter.browse(db))) {

            // reset states
            switch (triggerEntity.getState()) {
              case ACQUIRED:
              case BLOCKED:
                triggerEntity.setState(WAITING);
                break;
              case PAUSED_BLOCKED:
                triggerEntity.setState(PAUSED);
                break;
            }

            // remove COMPLETE ones, others needs awake
            if (COMPLETE == triggerEntity.getState()) {
              triggerEntityAdapter.deleteEntity(db, triggerEntity);
            }
            else {
              applyMisfire(db, triggerEntity);
              triggerEntityAdapter.editEntity(db, triggerEntity);
            }
          }

          // TODO: recover jobs marked for recovery that were not fully executed
          return null;
        }
    );
  }

  /**
   * Ignored.
   */
  @Override
  public void schedulerPaused() {
    // empty
  }

  /**
   * Ignored.
   */
  @Override
  public void schedulerResumed() {
    // empty
  }

  /**
   * Ignored.
   */
  @Override
  public void shutdown() {
    // empty
  }

  //
  // Other
  //

  @Override
  public long getEstimatedTimeToReleaseAndAcquireTrigger() {
    // Using same value from (JDBC) JobStoreSupport
    return 70;
  }

  @Override
  public void clearAllSchedulingData() throws JobPersistenceException {
    execute(db -> {
      jobDetailEntityAdapter.deleteAll(db);
      triggerEntityAdapter.deleteAll(db);
      calendarEntityAdapter.deleteAll(db);
      return null;
    });
  }

  //
  // Jobs
  //

  @Override
  public void storeJob(final JobDetail jobDetail, final boolean replaceExisting) throws JobPersistenceException {
    execute(db -> {
      storeJob(db, jobDetail, replaceExisting);
      return null;
    });
  }

  private void storeJob(final ODatabaseDocumentTx db, final JobDetail jobDetail, final boolean replaceExisting)
      throws JobPersistenceException
  {
    log.debug("Store job: jobDetail={}, replaceExisting={}", jobDetail, replaceExisting);

    JobDetailEntity entity = jobDetailEntityAdapter.readByKey(db, jobDetail.getKey());
    if (entity == null) {
      // no existing entity, add new one
      entity = new JobDetailEntity(jobDetail);
      jobDetailEntityAdapter.addEntity(db, entity);
    }
    else {
      // otherwise entity exists, maybe replace if allowed
      if (replaceExisting) {
        entity.setValue(jobDetail);
        jobDetailEntityAdapter.editEntity(db, entity);
      }
      else {
        throw new ObjectAlreadyExistsException(jobDetail);
      }
    }
  }

  @Override
  public boolean removeJob(final JobKey jobKey) throws JobPersistenceException {
    log.debug("Remove job: {}", jobKey);
    return execute(db -> removeJob(db, jobKey));
  }

  private boolean removeJob(final ODatabaseDocumentTx db, final JobKey jobKey) throws JobPersistenceException {
    boolean deleted = jobDetailEntityAdapter.deleteByKey(db, jobKey);
    triggerEntityAdapter.deleteByJobKey(db, jobKey);
    return deleted;
  }

  @Override
  public boolean removeJobs(final List<JobKey> jobKeys) throws JobPersistenceException {
    log.debug("Remove jobs: {}", jobKeys);
    return execute(db -> {
      boolean allDeleted = true;
      for (JobKey key : jobKeys) {
        allDeleted = removeJob(db, key) && allDeleted;
      }
      return allDeleted;
    });
  }

  @Override
  @Nullable
  public JobDetail retrieveJob(final JobKey jobKey) throws JobPersistenceException {
    return execute(db -> {
      JobDetailEntity entity = jobDetailEntityAdapter.readByKey(db, jobKey);
      return entity != null ? entity.getValue() : null;
    });
  }

  @Override
  public boolean checkExists(final JobKey jobKey) throws JobPersistenceException {
    return execute(db -> jobDetailEntityAdapter.existsByKey(db, jobKey));
  }

  @Override
  public int getNumberOfJobs() throws JobPersistenceException {
    return execute(db -> jobDetailEntityAdapter.countI(db));
  }

  @Override
  public List<String> getJobGroupNames() throws JobPersistenceException {
    return execute(db -> {
      ArrayList<String> result = new ArrayList<>();
      for (JobDetailEntity entity : jobDetailEntityAdapter.browse(db)) {
        result.add(entity.getGroup());
      }
      return result.stream().distinct().collect(Collectors.toList());
    });
  }

  @Override
  public Set<JobKey> getJobKeys(final GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    return execute(db -> getJobKeys(db, matcher));
  }

  private Set<JobKey> getJobKeys(final ODatabaseDocumentTx db, final GroupMatcher<JobKey> matcher)
      throws JobPersistenceException
  {
    Iterable<JobDetailEntity> matches = jobDetailEntityAdapter.browseWithPredicate
        (db, input -> matcher.isMatch(input.getValue().getKey()));

    Set<JobKey> result = new HashSet<>();
    for (JobDetailEntity entity : matches) {
      result.add(entity.getValue().getKey());
    }
    return result;
  }

  @Override
  public void pauseJob(final JobKey jobKey) throws JobPersistenceException {
    execute(db -> {
      pauseJob(db, jobKey);
      return null;
    });
  }

  private void pauseJob(final ODatabaseDocumentTx db, final JobKey jobKey) throws JobPersistenceException {
    log.debug("Pause job: {}", jobKey);

    for (OperableTrigger trigger : getTriggersForJob(db, jobKey)) {
      pauseTrigger(db, trigger.getKey());
    }
  }

  @Override
  public Collection<String> pauseJobs(final GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    log.debug("Pause jobs: {}", matcher);

    return execute(db -> {
      Set<String> groups = new HashSet<>();
      for (JobKey jobKey : getJobKeys(db, matcher)) {
        groups.add(jobKey.getGroup());
      }

      for (String group : groups) {
        for (JobKey jobKey : getJobKeys(db, GroupMatcher.jobGroupEquals(group))) {
          pauseJob(db, jobKey);
        }
      }
      return groups;
    });
  }

  @Override
  public void resumeJob(final JobKey jobKey) throws JobPersistenceException {
    execute(db -> {
      resumeJob(db, jobKey);
      return null;
    });
  }

  private void resumeJob(final ODatabaseDocumentTx db, final JobKey jobKey) throws JobPersistenceException {
    log.debug("Resume job: {}", jobKey);

    for (OperableTrigger trigger : getTriggersForJob(db, jobKey)) {
      resumeTrigger(db, trigger.getKey());
    }
  }

  @Override
  public Collection<String> resumeJobs(final GroupMatcher<JobKey> matcher) throws JobPersistenceException {
    log.debug("Resume jobs: {}", matcher);

    return execute(db -> {
      Set<String> groups = new HashSet<>();
      for (JobKey jobKey : getJobKeys(db, matcher)) {
        groups.add(jobKey.getGroup());
      }

      for (String group : groups) {
        for (JobKey jobKey : getJobKeys(db, GroupMatcher.jobGroupEquals(group))) {
          resumeJob(db, jobKey);
        }
      }
      return groups;
    });
  }

  @Override
  public void storeJobAndTrigger(final JobDetail jobDetail, final OperableTrigger trigger)
      throws JobPersistenceException
  {
    execute(db -> {
      storeJob(db, jobDetail, false);
      storeTrigger(db, trigger, false);
      return null;
    });
  }

  @Override
  public void storeJobsAndTriggers(final Map<JobDetail, Set<? extends Trigger>> jobsAndTriggers,
                                   final boolean replace)
      throws JobPersistenceException
  {
    execute(db -> {
      for (Entry<JobDetail, Set<? extends Trigger>> entry : jobsAndTriggers.entrySet()) {
        JobDetail jobDetail = entry.getKey();
        storeJob(db, jobDetail, replace);

        Set<? extends Trigger> triggers = entry.getValue();
        for (Trigger trigger : triggers) {
          storeTrigger(db, (OperableTrigger) trigger, replace);
        }
      }
      return null;
    });
  }

  //
  // Triggers
  //

  @Override
  public void storeTrigger(final OperableTrigger trigger, final boolean replaceExisting)
      throws JobPersistenceException
  {
    execute(db -> {
      storeTrigger(db, trigger, replaceExisting);
      return null;
    });
  }

  private void storeTrigger(final ODatabaseDocumentTx db,
                            final OperableTrigger trigger,
                            final boolean replaceExisting)
      throws JobPersistenceException
  {
    log.debug("Store trigger: trigger={}, replaceExisting={}", trigger, replaceExisting);

    if (isClustered()) {
      // associate trigger with the node that created it
      trigger.getJobDataMap().put(NODE_ID, nodeAccess.getId());
    }

    TriggerEntity entity = triggerEntityAdapter.readByKey(db, trigger.getKey());
    if (entity == null) {
      // no existing entity, add new one
      entity = new TriggerEntity(trigger, WAITING);
      triggerEntityAdapter.addEntity(db, entity);
    }
    else {
      // otherwise entity exists, maybe replace if allowed
      if (replaceExisting) {
        entity.setValue(trigger);
        triggerEntityAdapter.editEntity(db, entity);
      }
      else {
        throw new ObjectAlreadyExistsException(trigger);
      }
    }
  }

  @Override
  public boolean removeTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
    log.debug("Remove trigger: {}", triggerKey);
    return execute(db -> removeTrigger(db, triggerKey));
  }

  private boolean removeTrigger(final ODatabaseDocumentTx db, final TriggerKey triggerKey)
      throws JobPersistenceException
  {
    TriggerEntity entity = triggerEntityAdapter.readByKey(db, triggerKey);
    
    if (entity == null) {
      log.debug("No matching Trigger to remove for key: {}", triggerKey);
      return false;
    }

    // resolve job-key before deleting for orphan cleanup
    JobKey jobKey = entity.getValue().getJobKey();

    boolean deleted = triggerEntityAdapter.deleteByKey(db, triggerKey);
    log.debug("Trigger deleted: {} for key: {}", deleted, triggerKey);

    // delete related job if there are no triggers for it
    if (deleted) {
      Iterable<TriggerEntity> jobTriggers = triggerEntityAdapter.browseByJobKey(db, jobKey);

      // if there are no other triggers for the job then delete it
      if (!jobTriggers.iterator().hasNext()) {
        // lookup the job-detail so we can check if its durable or not
        JobDetailEntity jobDetailEntity = jobDetailEntityAdapter.readByKey(db, jobKey);

        if (jobDetailEntity != null && !jobDetailEntity.getValue().isDurable()) {
          // job is not durable, delete it
          boolean jobDeleted = jobDetailEntityAdapter.deleteByKey(db, jobKey);
          log.debug("Job deleted: {} for jobKey: {}", deleted, jobKey);

          if (jobDeleted) {
            signaler.notifySchedulerListenersJobDeleted(jobKey);
          }
        }
      }
    }

    return deleted;
  }

  @Override
  public boolean removeTriggers(final List<TriggerKey> triggerKeys) throws JobPersistenceException {
    log.debug("Remove triggers: {}", triggerKeys);

    return execute(db -> {
      boolean allDeleted = true;
      for (TriggerKey key : triggerKeys) {
        allDeleted = removeTrigger(db, key) && allDeleted;
      }
      return allDeleted;
    });
  }

  @Override
  public boolean replaceTrigger(final TriggerKey triggerKey, final OperableTrigger trigger)
      throws JobPersistenceException
  {
    log.debug("Replace trigger: triggerKey={}, trigger={}", triggerKey, trigger);

    if (isClustered()) {
      // associate trigger with the node that replaced it
      trigger.getJobDataMap().put(NODE_ID, nodeAccess.getId());
    }

    return execute(db -> {
      TriggerEntity entity = triggerEntityAdapter.readByKey(db, triggerKey);
      if (entity != null) {
        // if entity exists, ensure trigger is associate with the same job
        if (!entity.getValue().getJobKey().equals(trigger.getJobKey())) {
          throw new JobPersistenceException("New trigger is not related to the same job as the old trigger");
        }
        entity.setValue(trigger);
        triggerEntityAdapter.editEntity(db, entity);
        return true;
      }
      else {
        // otherwise add new entity
        entity = new TriggerEntity(trigger, WAITING);
        triggerEntityAdapter.addEntity(db, entity);
        return false;
      }
    });
  }

  @Override
  @Nullable
  public OperableTrigger retrieveTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
    return execute(db -> {
      TriggerEntity entity = triggerEntityAdapter.readByKey(db, triggerKey);
      return entity != null ? entity.getValue() : null;
    });
  }

  @Override
  public boolean checkExists(final TriggerKey triggerKey) throws JobPersistenceException {
    return execute(db -> triggerEntityAdapter.existsByKey(db, triggerKey));
  }

  @Override
  public int getNumberOfTriggers() throws JobPersistenceException {
    return execute(db -> triggerEntityAdapter.countI(db));
  }

  @Override
  public Set<TriggerKey> getTriggerKeys(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    return execute(db -> getTriggerKeys(db, matcher));
  }

  /**
   * Returns all trigger keys that are matched by passed in matcher.
   */
  private Set<TriggerKey> getTriggerKeys(final ODatabaseDocumentTx db,
                                         final GroupMatcher<TriggerKey> matcher)
      throws JobPersistenceException
  {
    Iterable<TriggerEntity> matches = triggerEntityAdapter.browseWithPredicate
        (db, input -> matcher.isMatch(input.getValue().getKey()));

    Set<TriggerKey> result = new HashSet<>();
    for (TriggerEntity entity : matches) {
      result.add(entity.getValue().getKey());
    }
    return result;
  }

  /**
   * Returns all trigger groups that are matched by passed in matcher.
   */
  private Set<String> getTriggerGroups(final ODatabaseDocumentTx db, final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    Set<String> groups = new HashSet<>();
    for (TriggerKey triggerKey : getTriggerKeys(db, matcher)) {
      groups.add(triggerKey.getGroup());
    }
    return groups;
  }

  @Override
  public List<String> getTriggerGroupNames() throws JobPersistenceException {
    return execute(db -> ImmutableList.copyOf(getTriggerGroups(db, GroupMatcher.anyGroup())));
  }

  @Override
  public List<OperableTrigger> getTriggersForJob(final JobKey jobKey) throws JobPersistenceException {
    return execute(db -> getTriggersForJob(db, jobKey));
  }

  private List<OperableTrigger> getTriggersForJob(final ODatabaseDocumentTx db, final JobKey jobKey) {
    List<OperableTrigger> result = new ArrayList<>();
    for (TriggerEntity entity : triggerEntityAdapter.browseByJobKey(db, jobKey)) {
      result.add(entity.getValue());
    }
    return result;
  }

  @Override
  public TriggerState getTriggerState(final TriggerKey triggerKey) throws JobPersistenceException {
    return execute(db -> {
      TriggerEntity entity = triggerEntityAdapter.readByKey(db, triggerKey);
      if (entity == null) {
        return TriggerState.NONE;
      }

      // convert entity state to trigger state
      switch (entity.getState()) {
        case COMPLETE:
          return TriggerState.COMPLETE;

        case PAUSED:
        case PAUSED_BLOCKED:
          return TriggerState.PAUSED;

        case BLOCKED:
          return TriggerState.BLOCKED;

        case ERROR:
          return TriggerState.ERROR;

        default:
          return TriggerState.NORMAL;
      }
    });
  }

  @Override
  public void pauseTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
    execute(db -> {
      pauseTrigger(db, triggerKey);
      return null;
    });
  }

  private void pauseTrigger(final ODatabaseDocumentTx db, final TriggerKey triggerKey) throws JobPersistenceException {
    log.debug("Pause trigger: {}", triggerKey);

    TriggerEntity entity = triggerEntityAdapter.readByKey(db, triggerKey);
    if (entity == null) {
      return;
    }

    // transition state to paused
    switch (entity.getState()) {
      case COMPLETE:
        // ignore pause on completed trigger
        return;

      case BLOCKED:
        // special pause for blocked trigger
        entity.setState(PAUSED_BLOCKED);
        break;

      default:
        // otherwise transition to paused
        entity.setState(PAUSED);
        break;
    }

    triggerEntityAdapter.editEntity(db, entity);
  }

  @Override
  public Collection<String> pauseTriggers(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    log.debug("Pause triggers: {}", matcher);

    return execute(db -> {
      Set<String> groups = new HashSet<>();
      for (TriggerKey triggerKey : getTriggerKeys(db, matcher)) {
        groups.add(triggerKey.getGroup());
      }

      for (String group : groups) {
        for (TriggerKey triggerKey : getTriggerKeys(db, GroupMatcher.triggerGroupEquals(group))) {
          pauseTrigger(db, triggerKey);
        }
      }
      return groups;
    });
  }

  @Override
  public void pauseAll() throws JobPersistenceException {
    log.debug("Pause all");

    execute(db -> {
      for (TriggerKey triggerKey : getTriggerKeys(db, GroupMatcher.<TriggerKey>anyGroup())) {
        pauseTrigger(db, triggerKey);
      }
      return null;
    });
  }

  @Override
  public void resumeTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
    execute(db -> {
      resumeTrigger(db, triggerKey);
      return null;
    });
  }

  private void resumeTrigger(final ODatabaseDocumentTx db, final TriggerKey triggerKey) throws JobPersistenceException {
    log.debug("Resume trigger: {}", triggerKey);

    TriggerEntity entity = triggerEntityAdapter.readByKey(db, triggerKey);
    if (entity == null) {
      return;
    }

    if (entity.getState() == PAUSED_BLOCKED) {
      entity.setState(BLOCKED);
    }
    else {
      entity.setState(WAITING);
    }

    applyMisfire(db, entity);

    triggerEntityAdapter.editEntity(db, entity);
  }

  @Override
  public Collection<String> resumeTriggers(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
    log.debug("Resume triggers: {}", matcher);

    return execute(db -> {
      Set<String> groups = getTriggerGroups(db, matcher);
      for (String group : groups) {
        for (TriggerKey triggerKey : getTriggerKeys(db, GroupMatcher.triggerGroupEquals(group))) {
          resumeTrigger(db, triggerKey);
        }
      }
      return groups;
    });
  }

  @Override
  public void resumeAll() throws JobPersistenceException {
    log.debug("Resume all");

    execute(db -> {
      for (TriggerKey triggerKey : getTriggerKeys(db, GroupMatcher.<TriggerKey>anyGroup())) {
        resumeTrigger(db, triggerKey);
      }
      return null;
    });
  }

  @Override
  public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
    return execute(
        db -> {
          Set<String> pausedGroups = new HashSet<>();
          Set<String> groups = getTriggerGroups(db, GroupMatcher.anyGroup());
          for (String group : groups) {
            boolean allPaused = !ImmutableList.copyOf(triggerEntityAdapter.browseByGroup(db, group))
                .stream().anyMatch(e -> PAUSED != e.getState() && PAUSED_BLOCKED != e.getState());
            if (allPaused) {
              pausedGroups.add(group);
            }
          }
          return pausedGroups;
        }
    );
  }

  @Override
  public List<OperableTrigger> acquireNextTriggers(final long noLaterThan,
                                                   final int maxCount,
                                                   final long timeWindow)
      throws JobPersistenceException
  {
    log.debug("Acquire next triggers: noLaterThan={}, maxCount={}, timeWindow={}", noLaterThan, maxCount, timeWindow);

    return execute(db -> {
      // find all local triggers in WAITING state
      Iterator<TriggerEntity> matches = local(triggerEntityAdapter.browseByState(db, WAITING)).iterator();

      // short-circuit if no matches
      if (!matches.hasNext()) {
        return Collections.emptyList();
      }

      long noEarlierThan = getMisfireTime();
      List<TriggerEntity> resultEntities = new ArrayList<>();

      while (matches.hasNext()) {
        TriggerEntity entity = matches.next();
        OperableTrigger trigger = entity.getValue();

        // skip triggers which have no next fire time
        if (trigger.getNextFireTime() == null) {
          continue;
        }

        // skip triggers which match misfire fudge logic
        if (applyMisfire(db, entity)) {
          continue;
        }

        // skip triggers which fire outside of requested window
        if (trigger.getNextFireTime().getTime() > noLaterThan + timeWindow) {
          continue;
        }

        // skip triggers which fire outside of requested window
        if (trigger.getMisfireInstruction() != Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY &&
            trigger.getNextFireTime().getTime() < noEarlierThan) {
          continue;
        }

        // check after misfire logic to avoid repeated log-spam
        if (isClustered() && isLimitedToMissingNode(trigger)) {
          continue;
        }

        // track result
        resultEntities.add(entity);
      }

      // sort candidates
      Collections.sort(resultEntities, TRIGGER_COMPARATOR);

      // cope with jobs which have disallowed concurrent execution
      // gather set of job-keys acquired which have concurrent execution disabled and eliminate dupe triggers
      Set<JobKey> jobsAcquired = new HashSet<>();
      Iterator<TriggerEntity> triggerEntityIterator = resultEntities.iterator();
      while (triggerEntityIterator.hasNext()) {
        TriggerEntity triggerEntity = triggerEntityIterator.next();
        OperableTrigger trigger = triggerEntity.getValue();
        JobKey jobKey = trigger.getJobKey();
        JobDetailEntity jobDetailEntity = jobDetailEntityAdapter.readByKey(db, jobKey);
        if (jobDetailEntity != null && jobDetailEntity.getValue().isConcurrentExectionDisallowed()) {
          if (jobsAcquired.contains(jobKey)) {
            // trigger for job disallowing concurrent execution already acquired
            triggerEntityIterator.remove();
          }
          else {
            jobsAcquired.add(jobKey);
          }
        }
      }

      // limit result set if needed
      if (!resultEntities.isEmpty() && maxCount < resultEntities.size()) {
        resultEntities = resultEntities.subList(0, maxCount);
      }

      // set state on selected ones
      List<OperableTrigger> result = new ArrayList<>();
      for (TriggerEntity entity : resultEntities) {
        OperableTrigger trigger = entity.getValue();
        // TODO: Sort out if this is needed, maybe set to entity-id.value?
        // TODO: JDBC store impl uses this to do some validation on triggersFired()
        trigger.setFireInstanceId(UUID.randomUUID().toString());
        if (isClustered()) {
          // associate trigger with the node that acquired it
          trigger.getJobDataMap().put(NODE_ID, nodeAccess.getId());
        }
        result.add(trigger);
        entity.setState(ACQUIRED);
        triggerEntityAdapter.editEntity(db, entity);
      }

      log.trace("Acquired triggers: {}", result);
      return result;
    });
  }

  /**
   * Comparator used for trigger selection: sorts by next fire time, then by priority.
   */
  private static final Comparator<TriggerEntity> TRIGGER_COMPARATOR = new Comparator<TriggerEntity>()
  {
    @Override
    public int compare(final TriggerEntity o1, final TriggerEntity o2) {
      int res;
      // nextFireTime ASC
      res = o1.getValue().getNextFireTime().compareTo(o2.getValue().getNextFireTime());
      if (res != 0) {
        return res;
      }
      // priority DESC
      return o2.getValue().getPriority() - o1.getValue().getPriority();
    }
  };

  @Override
  public void releaseAcquiredTrigger(final OperableTrigger trigger) {
    log.debug("Release acquired trigger: {}", trigger);

    executeAndPropagate(db -> {
      TriggerEntity entity = triggerEntityAdapter.readByKey(db, trigger.getKey());

      // update state to WAITING if the current state is ACQUIRED
      if (entity != null && entity.getState() == ACQUIRED) {
        entity.setState(WAITING);
        triggerEntityAdapter.editEntity(db, entity);
      }

      return null;
    });
  }

  @Override
  public List<TriggerFiredResult> triggersFired(final List<OperableTrigger> triggers) throws JobPersistenceException {
    log.debug("Triggers fired: {}", triggers);

    return execute(db -> {
      List<TriggerFiredResult> results = new ArrayList<>();

      for (OperableTrigger trigger : triggers) {
        TriggerFiredResult result;

        try {
          TriggerFiredBundle bundle = triggerFired(db, trigger);
          result = new TriggerFiredResult(bundle);
        }
        catch (Exception e) {
          log.warn("Trigger fired failure", e);
          result = new TriggerFiredResult(e);
        }

        results.add(result);
      }

      log.trace("Triggers fired results: {}", results);
      return results;
    });
  }

  /**
   * Processes a fired trigger, if the trigger (and related entities) still exist and the trigger is in proper state.
   */
  @Nullable
  private TriggerFiredBundle triggerFired(final ODatabaseDocumentTx db, final OperableTrigger firedTrigger) {
    log.debug("Trigger fired: {}", firedTrigger);

    // resolve the entity for fired trigger
    final TriggerKey triggerKey = firedTrigger.getKey();
    TriggerEntity entity = triggerEntityAdapter.readByKey(db, triggerKey);

    // skip if trigger was deleted
    if (entity == null) {
      log.trace("Trigger deleted; skipping");
      return null;
    }

    // skip if trigger is not in ACQUIRED state
    if (entity.getState() != ACQUIRED) {
      log.trace("Trigger state != ACQUIRED; skipping");
      return null;
    }

    OperableTrigger trigger = entity.getValue();

    // resolve trigger calender if there is one
    Calendar calendar = null;
    if (trigger.getCalendarName() != null) {
      calendar = findCalendar(db, trigger.getCalendarName());
      if (calendar == null) {
        log.trace("Calender was deleted; skipping");
        return null;
      }
    }

    Date prevFireTime = trigger.getPreviousFireTime();

    // inform both scheduler and persistent instances were triggered
    firedTrigger.triggered(calendar);
    trigger.triggered(calendar);

    // update trigger to WAITING state
    entity.setState(WAITING);
    triggerEntityAdapter.editEntity(db, entity);

    // re-resolve trigger value after edit for sanity
    trigger = entity.getValue();

    // resolve the job-detail for this trigger
    JobDetailEntity jobDetailEntity = jobDetailEntityAdapter.readByKey(db, trigger.getJobKey());
    checkState(jobDetailEntity != null, "Missing job-detail for trigger-key: %s", triggerKey);
    JobDetail jobDetail = jobDetailEntity.getValue();

    // block other triggers for job if concurrent execution is disallowed
    if (jobDetail.isConcurrentExectionDisallowed()) {
      blockOtherTriggers(db, triggerKey, jobDetail.getKey());
    }

    return new TriggerFiredBundle(
        jobDetail,
        trigger,
        calendar,
        false,
        new Date(),
        trigger.getPreviousFireTime(),
        prevFireTime,
        trigger.getNextFireTime()
    );
  }

  /**
   * Helper to update all triggers for a job (except the fired trigger)
   * which are WAITING/PAUSED to BLOCKED/PAUSE_BLOCKED state.
   *
   * @see #triggersFired
   */
  private void blockOtherTriggers(final ODatabaseDocumentTx db,
                                  final TriggerKey firedTriggerKey,
                                  final JobKey jobKey)
  {
    log.trace("Blocking other triggers: firedTriggerKey={}, jobKey={}", firedTriggerKey, jobKey);

    Iterable<TriggerEntity> matches = triggerEntityAdapter.browseWithPredicate
        (db, input -> {
          switch (input.getState()) {
            case WAITING:
            case PAUSED:
              return jobKey.equals(input.getValue().getJobKey());
          }
          return false;
        });

    for (TriggerEntity entity : matches) {
      // skip the trigger which was fired
      if (firedTriggerKey.equals(entity.getValue().getKey())) {
        continue;
      }

      if (entity.getState() == PAUSED) {
        entity.setState(PAUSED_BLOCKED);
      }
      else {
        entity.setState(BLOCKED);
      }
      triggerEntityAdapter.editEntity(db, entity);
    }
  }

  @Override
  public void triggeredJobComplete(final OperableTrigger trigger,
                                   final JobDetail jobDetail,
                                   final CompletedExecutionInstruction instruction)
  {
    log.debug("Triggered job complete: trigger={}, jobDetail={}, instruction={}", trigger, jobDetail, instruction);

    executeAndPropagate(db -> {
      TriggerEntity triggerEntity = triggerEntityAdapter.readByKey(db, trigger.getKey());
      JobDetailEntity jobDetailEntity = jobDetailEntityAdapter.readByKey(db, jobDetail.getKey());

      if (jobDetailEntity != null) {
        // save job-data-map if needed
        if (jobDetail.isPersistJobDataAfterExecution() && jobDetail.getJobDataMap() != null) {
          jobDetailEntity.setValue(jobDetail);
          jobDetailEntityAdapter.editEntity(db, jobDetailEntity);
        }

        // unblock triggers if concurrent execution is disallowed
        if (jobDetail.isConcurrentExectionDisallowed()) {
          unblockTriggers(db, jobDetail.getKey());
          signaler.signalSchedulingChange(0L);
        }
      }

      if (triggerEntity != null) {
        switch (instruction) {
          case DELETE_TRIGGER:
            if (trigger.getNextFireTime() == null) {
              // double check for possible reschedule within job execution, which would cancel the need to delete
              if (triggerEntity.getValue().getNextFireTime() == null) {
                triggerEntityAdapter.deleteEntity(db, triggerEntity);
              }
            }
            else {
              triggerEntityAdapter.deleteEntity(db, triggerEntity);
              signaler.signalSchedulingChange(0L);
            }
            break;

          case SET_TRIGGER_COMPLETE:
            triggerEntity.setState(COMPLETE);
            triggerEntityAdapter.editEntity(db, triggerEntity);
            signaler.signalSchedulingChange(0L);
            break;

          case SET_TRIGGER_ERROR:
            triggerEntity.setState(ERROR);
            triggerEntityAdapter.editEntity(db, triggerEntity);
            signaler.signalSchedulingChange(0L);
            break;

          case SET_ALL_JOB_TRIGGERS_COMPLETE:
            updateTriggerState(db, jobDetail.getKey(), COMPLETE);
            signaler.signalSchedulingChange(0L);
            break;

          case SET_ALL_JOB_TRIGGERS_ERROR:
            updateTriggerState(db, jobDetail.getKey(), ERROR);
            signaler.signalSchedulingChange(0L);
            break;
        }
      }

      return null;
    });
  }

  /**
   * Helper to update all triggers for a job which are BLOCKED/PAUSED_BLOCKED to WAITING/PAUSED state.
   *
   * @see #triggeredJobComplete
   */
  private void unblockTriggers(final ODatabaseDocumentTx db, final JobKey jobKey) {
    log.trace("Unblock triggers: jobKey={}", jobKey);

    Iterable<TriggerEntity> matches = triggerEntityAdapter.browseWithPredicate
        (db, input -> {
          switch (input.getState()) {
            case BLOCKED:
            case PAUSED_BLOCKED:
              return jobKey.equals(input.getValue().getJobKey());
          }
          return false;
        });

    for (TriggerEntity entity : matches) {
      if (entity.getState() == PAUSED_BLOCKED) {
        entity.setState(PAUSED);
      }
      else {
        entity.setState(WAITING);
      }
      triggerEntityAdapter.editEntity(db, entity);
    }
  }

  /**
   * Helper to update the state of all triggers for a job.
   *
   * @see #triggeredJobComplete
   */
  private void updateTriggerState(final ODatabaseDocumentTx db, final JobKey jobKey, final TriggerEntity.State state) {
    log.trace("Updating job trigger states: jobKey={}, state={}", jobKey, state);

    Iterable<TriggerEntity> matches = triggerEntityAdapter.browseByJobKey(db, jobKey);

    for (TriggerEntity entity : matches) {
      entity.setState(state);
      triggerEntityAdapter.editEntity(db, entity);
    }
  }

  // TODO: sort out if we want/need this to be configurable, else a static constant could be better
  private final long misfireThreshold = Time.minutes(1).toMillis();

  private long getMisfireTime() {
    long misfireTime = System.currentTimeMillis();
    if (misfireThreshold > 0) {
      misfireTime -= misfireThreshold;
    }
    return (misfireTime > 0) ? misfireTime : 0;
  }

  /**
   * Determine if trigger has misfired.
   */
  private boolean applyMisfire(final ODatabaseDocumentTx db, final TriggerEntity triggerEntity) {
    log.trace("Checking for misfire: {}", triggerEntity);

    OperableTrigger trigger = triggerEntity.getValue();

    long misfireTime = getMisfireTime();

    Date nextFireTime = trigger.getNextFireTime();
    if (nextFireTime == null || nextFireTime.getTime() > misfireTime ||
        trigger.getMisfireInstruction() == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) {
      return false;
    }

    // resolve trigger calender if there is one
    Calendar calendar = null;
    if (trigger.getCalendarName() != null) {
      calendar = findCalendar(db, trigger.getCalendarName());
    }

    signaler.notifyTriggerListenersMisfired(trigger);
    trigger.updateAfterMisfire(calendar);

    if (trigger.getNextFireTime() == null) {
      triggerEntity.setState(COMPLETE);
      triggerEntityAdapter.editEntity(db, triggerEntity);
      signaler.notifySchedulerListenersFinalized(trigger);
    }
    else if (nextFireTime.equals(trigger.getNextFireTime())) {
      return false;
    }

    return true;
  }

  //
  // Calenders
  //

  @Override
  public void storeCalendar(final String name,
                            final Calendar calendar,
                            final boolean replaceExisting,
                            final boolean updateTriggers)
      throws JobPersistenceException
  {
    log.debug("Store calendar: name={}, calendar={}, replaceExisting={}, updateTriggers={}",
        name, calendar, replaceExisting, updateTriggers);

    execute(db -> {
      CalendarEntity entity = calendarEntityAdapter.readByName(db, name);
      if (entity == null) {
        // no existing entity, add new one
        entity = new CalendarEntity(name, calendar);
        calendarEntityAdapter.addEntity(db, entity);
      }
      else {
        // otherwise entity exists, maybe replace if allowed
        if (replaceExisting) {
          entity.setValue(calendar);
          calendarEntityAdapter.editEntity(db, entity);
        }
      }

      if (updateTriggers) {
        // update all triggers using this calender
        for (TriggerEntity triggerEntity : triggerEntityAdapter.browseByCalendarName(db, name)) {
          triggerEntity.getValue().updateWithNewCalendar(calendar, misfireThreshold);
          triggerEntityAdapter.editEntity(db, triggerEntity);
        }
      }

      return null;
    });
  }

  @Override
  public boolean removeCalendar(final String name) throws JobPersistenceException {
    log.debug("Remove calendar: {}", name);

    return execute(db -> calendarEntityAdapter.deleteByName(db, name));
  }

  @Override
  @Nullable
  public Calendar retrieveCalendar(final String name) throws JobPersistenceException {
    return execute(db -> {
      CalendarEntity entity = calendarEntityAdapter.readByName(db, name);
      return entity != null ? entity.getValue() : null;
    });
  }

  @Override
  public int getNumberOfCalendars() throws JobPersistenceException {
    return execute(db -> calendarEntityAdapter.countI(db));
  }

  @Override
  public List<String> getCalendarNames() throws JobPersistenceException {
    return execute(db -> calendarEntityAdapter.browseNames(db));
  }

  /**
   * Helper to find a calendar by name.
   */
  @Nullable
  private Calendar findCalendar(final ODatabaseDocumentTx db, final String name) {
    CalendarEntity calendarEntity = calendarEntityAdapter.readByName(db, name);
    if (calendarEntity != null) {
      return calendarEntity.getValue();
    }
    return null;
  }

  /**
   * Helper to get locally-owned (as well as orphaned) triggers.
   */
  private Iterable<TriggerEntity> local(final Iterable<TriggerEntity> triggers) {
    if (isClustered()) {
      String localId = nodeAccess.getId();
      Set<String> memberIds = nodeAccess.getMemberIds();

      return Iterables.filter(triggers, (entity) -> {
        JobDataMap triggerDetail = entity.getValue().getJobDataMap();
        if (triggerDetail.containsKey(LIMIT_NODE_KEY)) {
          // filter limited triggers to those limited to run on this node, or orphaned
          String limitedNodeId = triggerDetail.getString(LIMIT_NODE_KEY);
          return localId.equals(limitedNodeId) || !memberIds.contains(limitedNodeId);
        }
        // filter all other triggers to those "owned" by this node, or orphaned
        String owner = triggerDetail.getString(NODE_ID);
        return localId.equals(owner) || !memberIds.contains(owner);
      });
    }
    return triggers;
  }

  /**
   * Helper to warn when a limited trigger won't fire because its node is missing.
   */
  private boolean isLimitedToMissingNode(final OperableTrigger trigger) {
    JobDataMap triggerDetail = trigger.getJobDataMap();
    if (triggerDetail.containsKey(LIMIT_NODE_KEY)) {
      String limitedNodeId = triggerDetail.getString(LIMIT_NODE_KEY);
      // can skip members check here because "local()" has already filtered limited triggers down to those
      // which are either limited to run on the current node, or on a missing node (ie. have been orphaned)
      if (!nodeAccess.getId().equals(limitedNodeId)) {
        // not limited to this node, so must be an orphaned trigger
        String description = trigger.getDescription();
        if (Strings2.isBlank(description)) {
          description = trigger.getJobKey().getName();
        }
        if (Strings2.isBlank(limitedNodeId)) {
          log.warn("Cannot run task '{}' because it is not configured for HA", description);
        }
        else {
          log.warn("Cannot run task '{}' because it uses node {} which is not a member of this cluster", description,
              limitedNodeId);
        }
        return true;
      }
    }
    return false;
  }
}
