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