import request from '@/utils/request'

/**
 * 获取人员关系列表
 */
export function getRelations() {
  return request.get('/hierarchy/relations')
}

/**
 * 获取人员关系树（含孤儿节点）
 */
export function getTree() {
  return request.get('/hierarchy/tree')
}

/**
 * R38: 添加人员关系（superiorId 的下属为 subordinateId；forceReplace=下属已有上级时替换）
 */
export function addRelation(superiorId, subordinateId, forceReplace = false) {
  return request.post('/hierarchy/relation', { superiorId, subordinateId, forceReplace })
}

/**
 * R38: 删除人员关系
 */
export function removeRelation(superiorId, subordinateId) {
  return request.delete('/hierarchy/relation', { params: { superiorId, subordinateId } })
}