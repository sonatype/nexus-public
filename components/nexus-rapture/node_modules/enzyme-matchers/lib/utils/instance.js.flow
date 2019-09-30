// @flow

function internalInstanceKey(node) {
  return Object.keys(Object(node)).filter(key =>
    key.match(/^__reactInternalInstance\$/)
  )[0];
}

export default function internalInstance(inst: Object) {
  return inst._reactInternalInstance || inst[internalInstanceKey(inst)];
}
