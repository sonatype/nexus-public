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
package org.sonatype.nexus.proxy.walker;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.util.Node;
import org.sonatype.nexus.util.PathUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * A helper class to "optimize" when some sort of "gathering for later processing" (most notable collection of
 * directories to start Walks from) of repository paths happens, that are to be processed in some subsequent step by
 * "walking" them (recurse them, a la "visitor" pattern). This utility class simply maintains a "tree" with
 * <em>marked</em> nodes, while the meaning of "marked" is left to class user. One assumption does exists against
 * "marked": some recursive processing is planned against it. It currently applies these simple rules to it.
 * <p>
 * <ul>
 * <li>rule A: if parent node of the currently added node is already "marked", do not mark the currently added node
 * (reason: it will be anyway processes when marked parent processing starts to recurse)</li>
 * <li>rule B: if all the children nodes of the currently added node's parent as "marked", mark the parent and unmark
 * it's children (reason: it's usually cheaper to fire off one walk level above, from parent, instead firing, for
 * example 100, independent walks from children one by one)</li>
 * </ul>
 * <p>
 * Note: all the input paths are expected to be "normalized ones": being absolute, using generic "/" character as path
 * separator (since these are NOT File paths, but just hierarchical paths of strings). For example:
 * {@link RepositoryItemUid#getPath()} returns paths like these. See {@link RepositoryItemUid} for explanation.
 * <p>
 * This class also "optimizes" the tree size to lessen memory use. This "optimization" can be turned off, see
 * constructors.
 * <p>
 * This class makes use of {@link Node} to implement the tree hierarchy.
 *
 * @author cstamas
 * @since 2.0
 */
public class ParentOMatic
{
  public static class Payload
  {
    private boolean marked;

    public Payload() {
      this.marked = false;
    }

    public boolean isMarked() {
      return marked;
    }

    public void setMarked(boolean marked) {
      this.marked = marked;
    }
  }

  /**
   * If true, all the nodes below marked ones will be simply cut away, to lessen tree size and hence, memory
   * consumption. They have no need to stay in memory, since the result will not need them anyway, will not be
   * returned by {@link #getMarkedPaths()}.
   */
  private final boolean keepMarkedNodesOnly;

  private final boolean applyRuleA;

  private final boolean applyRuleB;

  /**
   * The root node.
   */
  private final Node<Payload> ROOT;

  /**
   * Creates new instance of ParentOMatic with default settings.
   */
  public ParentOMatic() {
    this(true);
  }

  public ParentOMatic(final boolean keepMarkedNodesOnly) {
    this(keepMarkedNodesOnly, true, true);
  }

  public ParentOMatic(final boolean keepMarkedNodesOnly, final boolean applyRuleA, final boolean applyRuleB) {
    this.keepMarkedNodesOnly = keepMarkedNodesOnly;
    this.applyRuleA = applyRuleA;
    this.applyRuleB = applyRuleB;
    this.ROOT = new Node<Payload>(null, "/", new Payload());
  }

  /**
   * Adds a path to this ParentOMatic instance without marking it.
   */
  public Node<Payload> addPath(final String path) {
    return addPath(path, true);
  }

  /**
   * Adds a path to this ParentOMatic and marks it. This might result in changes in tree that actually tries to
   * "optimize" the markings, and it may result in tree where the currently added and marked path is not marked, but
   * it's some parent is.
   */
  public void addAndMarkPath(final String path) {
    final Node<Payload> currentNode = addPath(path, false);

    // rule A: unmark children if any
    if (applyRuleA) {
      applyRecursively(currentNode, new Function<Node<Payload>, Node<Payload>>()
      {
        @Override
        public Node<Payload> apply(Node<Payload> input) {
          input.getPayload().setMarked(false);
          return input;
        }
      });
    }

    currentNode.getPayload().setMarked(true);

    // reorganize if needed
    final Node<Payload> flippedNode = reorganizeForRecursion(currentNode);

    // optimize tree size if asked for
    if (keepMarkedNodesOnly) {
      optimizeTreeSize(flippedNode);
    }
  }

  /**
   * Returns the list of the marked paths.
   */
  public List<String> getMarkedPaths() {
    // doing scanning
    final ArrayList<String> markedPaths = new ArrayList<String>();
    final Function<Node<Payload>, Node<Payload>> markedCollector = new Function<Node<Payload>, Node<Payload>>()
    {
      @Override
      public Node<Payload> apply(Node<Payload> input) {
        if (input.getPayload().isMarked()) {
          markedPaths.add(input.getPath());
        }
        return null;
      }
    };
    applyRecursively(ROOT, markedCollector);
    return markedPaths;
  }

  /**
   * Returns the list of all leaf paths.
   *
   * @since 2.4
   */
  public List<String> getAllLeafPaths() {
    // doing scanning
    final ArrayList<String> paths = new ArrayList<String>();
    final Function<Node<Payload>, Node<Payload>> markedCollector = new Function<Node<Payload>, Node<Payload>>()
    {
      @Override
      public Node<Payload> apply(Node<Payload> input) {
        if (input.isLeaf()) {
          paths.add(input.getPath());
        }
        return null;
      }
    };
    applyRecursively(ROOT, markedCollector);
    return paths;
  }

  /**
   * Cuts down tree to given maxDepth. After this method returns, this instance guarantees that there is no path
   * deeper than passed in maxDepth (shallower than it might exists!).
   */
  public void cutNodesDeeperThan(final int maxDepth) {
    applyRecursively(getRoot(), new Function<Node<Payload>, Node<Payload>>()
    {
      @Override
      public Node<Payload> apply(Node<Payload> input) {
        if (input.getDepth() == maxDepth) {
          // simply "cut off" children if any
          for (Node<Payload> child : input.getChildren()) {
            input.removeChild(child);
          }
        }
        return null;
      }
    });
  }

  // ==

  /**
   * Returns tree ROOT node.
   */
  public Node<Payload> getRoot() {
    return ROOT;
  }

  /**
   * Applies function recursively from the given node.
   */
  public void applyRecursively(final Node<Payload> fromNode, final Function<Node<Payload>, Node<Payload>> modifier) {
    modifier.apply(fromNode);

    for (Node<Payload> child : fromNode.getChildren()) {
      applyRecursively(child, modifier);
    }
  }

  // ==

  /**
   * "Dumps" the tree, for tests.
   */
  public String dump() {
    final StringBuilder sb = new StringBuilder();
    dump(ROOT, 0, sb);
    return sb.toString();
  }

  protected void dump(final Node<Payload> node, final int depth, final StringBuilder sb) {
    sb.append(Strings.repeat("  ", depth));
    sb.append(node.getLabel());
    sb.append(" (").append(node.getPath()).append(")");
    if (node.getPayload().isMarked()) {
      sb.append("*");
    }
    sb.append("\n");
    for (Node<Payload> child : node.getChildren()) {
      dump(child, depth + 1, sb);
    }
  }

  // ==

  protected Node<Payload> addPath(final String path, final boolean optimize) {
    final List<String> pathElems = getPathElements(Preconditions.checkNotNull(path));
    final List<String> actualPathElems = Lists.newArrayList();

    Node<Payload> currentNode = ROOT;

    for (String pathElem : pathElems) {
      actualPathElems.add(pathElem);
      final Node<Payload> node = currentNode.getChildByLabel(pathElem);

      if (node == null) {
        currentNode = currentNode.addChild(pathElem, new Payload());
      }
      else {
        currentNode = node;
      }
    }

    if (optimize) {
      optimizeTreeSize(currentNode);
    }

    return currentNode;
  }

  /**
   * Reorganizes the tree by applying the rules to the tree from the changed node and returns a node that was top
   * most
   * of the flipped ones.
   */
  protected Node<Payload> reorganizeForRecursion(final Node<Payload> changedNode) {
    // rule a: if parent is marked already, do not mark the child
    if (applyRuleA && isParentMarked(changedNode)) {
      changedNode.getPayload().setMarked(false);
      return changedNode.getParent();
    }

    // rule b: if this parent's all children are marked, mark parent, unmark children
    if (applyRuleB && isParentAllChildMarkedForRuleB(changedNode)) {
      changedNode.getParent().getPayload().setMarked(true);
      for (Node<Payload> child : changedNode.getParent().getChildren()) {
        child.getPayload().setMarked(false);
      }
      return changedNode.getParent();
    }

    return changedNode;
  }

  /**
   * Optimizes the tree by making the marked nodes as leafs, basically cutting all the branches that are below marked
   * node.
   */
  protected void optimizeTreeSize(final Node<Payload> changedNode) {
    // simply "cut off" children if any
    for (Node<Payload> child : changedNode.getChildren()) {
      changedNode.removeChild(child);
    }
  }

  /**
   * Returns true if parent exists (passed in node is not ROOT), and if parent {@link Payload#isMarked()} returns
   * {@code true}.
   */
  protected boolean isParentMarked(final Node<Payload> node) {
    final Node<Payload> parent = node.getParent();

    if (parent != null) {
      if (parent.getPayload().isMarked()) {
        return true;
      }
      else {
        return isParentMarked(parent);
      }
    }
    else {
      return false;
    }
  }

  /**
   * Returns true if parent exists (passed in node is not ROOT), and parent's all children are marked (their
   * {@link Payload#isMarked()} is {@code true} for all of them.
   */
  protected boolean isParentAllChildMarkedForRuleB(final Node<Payload> node) {
    final Node<Payload> parent = node.getParent();

    if (parent != null) {
      final List<Node<Payload>> children = parent.getChildren();

      if (children.size() < 2) {
        return false;
      }

      for (Node<Payload> child : children) {
        if (!child.getPayload().isMarked()) {
          return false;
        }
      }

      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Builds a path elements list from passed in path.
   */
  protected List<String> getPathElements(final String path) {
    return PathUtils.elementsOf(path);
  }
}
