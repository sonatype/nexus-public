/**
 * @function name
 * @returns string
 *
 * @flow
 */

import instance from './instance';

/**
 * Gets the name of the node or component for the SINGLE item
 */
function getNameFromRoot(root: Object): string {
  // shallow
  if (root.unrendered) {
    const { type } = root.unrendered;
    return type.name || type;
  }

  const inst = instance(root);
  if (inst) {
    return inst._tag;
  }

  // direct node
  if (typeof root.type === 'string') {
    return root.type;
  }

  return typeof root.name === 'function' ? root.name() : '(anonymous)';
}

/**
 * Can take any sort of wrapper. A single node, a component,
 * multiple nodes, multiple components.
 *
 * examples of outputs:
 * - "Fixture"
 * - "input"
 * - "(anonymous)"
 * - "2 "span" nodes found"
 * - "2 mixed nodes found"
 *
 * BUG: We used to be able to get the root node of an array of children elements by doing
 * `wrapper.root.unrendered.type`
 *
 * That is no longer exposed and Enzyme 3 may have a bug around this.
 * @see https://github.com/airbnb/enzyme/issues/1152
 *
 * If that issue is fixed, we may be able to bring back the "Fixture, 2 "span" nodes found"
 */
export default function getNameFromArbitraryWrapper(wrapper: Object): string {
  const nodeCount: number =
    typeof wrapper.getElements === 'function'
      ? wrapper.getElements().length
      : 0;

  switch (nodeCount) {
    case 0: {
      return '[empty set]';
    }
    case 1: {
      return getNameFromRoot(wrapper);
    }
    default: {
      const nodeTypeMap: Object = {};

      // determine if we have a mixed list of nodes or not
      wrapper.getElements().forEach(node => {
        const name: string = getNameFromRoot(node);
        nodeTypeMap[name] = (nodeTypeMap[name] || 0) + 1;
      });

      const nodeTypeList: Array<string> = Object.keys(nodeTypeMap);

      const nodeTypes: string =
        nodeTypeList.length === 1 ? nodeTypeList[0] : 'mixed';

      return `${nodeCount} ${nodeTypes} nodes found`;
    }
  }
}
