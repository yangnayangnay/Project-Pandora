import { createRouter, createWebHistory } from 'vue-router'
import { getToken, isTokenValid, removeToken, removeUser } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/RegisterView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/user',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/GlobalDashboardView.vue'),
        meta: { title: '全局看板', removed: true }
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/task/TaskKanbanView.vue'),
        meta: { title: '任务管理', removed: true }
      },
      {
        path: 'task/quadrant',
        name: 'TaskQuadrant',
        component: () => import('@/views/task/TaskQuadrantView.vue'),
        meta: { title: '四象限看板', removed: true }
      },
      {
        path: 'task/all',
        name: 'AllTasks',
        component: () => import('@/views/task/AllTasksView.vue'),
        meta: { title: '全量任务', removed: true }
      },
      {
        path: 'log/all',
        name: 'AllLogs',
        component: () => import('@/views/log/AllLogsView.vue'),
        meta: { title: '全量日志', removed: true }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/UserManageView.vue'),
        meta: { title: '人员管理' }
      },
      {
        path: 'account/create',
        name: 'AccountCreate',
        component: () => import('@/views/user/AccountCreateView.vue'),
        meta: { title: '账号创建' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'user/dept-team',
        name: 'DeptTeam',
        component: () => import('@/views/user/DeptTeamView.vue'),
        meta: { title: '部门团队', requiresAuth: true }
      },
      {
        path: 'user/hierarchy-relation',
        name: 'HierarchyRelation',
        component: () => import('@/views/user/HierarchyRelationView.vue'),
        meta: { title: '人员关系', requiresAuth: true }
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('@/views/ai/TaskCompletionView.vue'),
        meta: { title: '任务完成分析', removed: true }
      },
      {
        path: 'ai/keyword',
        name: 'KeywordAnalysis',
        component: () => import('@/views/ai/KeywordAnalysisView.vue'),
        meta: { title: '关键词分析', removed: true }
      },
      {
        path: 'ai/info-map',
        name: 'AIInfoMap',
        component: () => import('@/views/ai/AIInfoMapView.vue'),
        meta: { title: 'AI信息地图', removed: true }
      },
      {
        path: 'ai/mbti-report',
        name: 'MBTIReport',
        component: () => import('@/views/ai/MBTIReportView.vue'),
        meta: { title: 'MBTI报告', removed: true }
      },
      {
        path: 'permission/approval',
        name: 'Approval',
        component: () => import('@/views/permission/ApprovalView.vue'),
        meta: { title: '账号审批', removed: true }
      },
      {
        path: 'permission/audit-rule',
        name: 'AuditRule',
        component: () => import('@/views/permission/AuditRuleView.vue'),
        meta: { title: '审核规则', removed: true }
      },
      {
        path: 'permission/invitation',
        name: 'Invitation',
        component: () => import('@/views/permission/InvitationView.vue'),
        meta: { title: '邀约请示', removed: true }
      },
      {
        path: 'config/audit-log',
        name: 'AuditLog',
        component: () => import('@/views/config/AuditLogView.vue'),
        meta: { title: '系统日志' }
      },
      {
        path: 'config/system-config',
        name: 'SystemConfig',
        component: () => import('@/views/config/SystemConfigView.vue'),
        meta: { title: '系统配置', removed: true }
      },
      {
        path: 'top10/company-important',
        name: 'CompanyImportant',
        component: () => import('@/views/top10/CompanyImportantView.vue'),
        meta: { title: '公司十大事项' }
      },
      {
        path: 'permission/function',
        name: 'FunctionPermission',
        component: () => import('@/views/permission/FunctionPermissionView.vue'),
        meta: { title: '功能权限', removed: true }
      },
      {

        path: 'template',
        name: 'TemplateManage',
        component: () => import('@/views/template/TemplateManageView.vue'),
        meta: { title: '模板管理', removed: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  // Round19 REQ-19-01: 被移除菜单路径重定向至个人中心
  if (to.meta.removed) {
    next('/profile')
    return
  }
  const token = getToken()
  if (to.meta.requiresAuth !== false && !isTokenValid()) {
    if (token) { removeToken(); removeUser() }
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
})

export default router