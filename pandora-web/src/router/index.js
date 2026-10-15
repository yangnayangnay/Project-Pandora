import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

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
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/GlobalDashboardView.vue'),
        meta: { title: '全局看板' }
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/task/TaskKanbanView.vue'),
        meta: { title: '任务管理' }
      },
      {
        path: 'task/quadrant',
        name: 'TaskQuadrant',
        component: () => import('@/views/task/TaskQuadrantView.vue'),
        meta: { title: '四象限看板' }
      },
      {
        path: 'task/all',
        name: 'AllTasks',
        component: () => import('@/views/task/AllTasksView.vue'),
        meta: { title: '全量任务' }
      },
      {
        path: 'log/all',
        name: 'AllLogs',
        component: () => import('@/views/log/AllLogsView.vue'),
        meta: { title: '全量日志' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/UserManageView.vue'),
        meta: { title: '人员管理' }
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
        meta: { title: '部门团队' }
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('@/views/ai/TaskCompletionView.vue'),
        meta: { title: '任务完成分析' }
      },
      {
        path: 'ai/keyword',
        name: 'KeywordAnalysis',
        component: () => import('@/views/ai/KeywordAnalysisView.vue'),
        meta: { title: '关键词分析' }
      },
      {
        path: 'ai/info-map',
        name: 'AIInfoMap',
        component: () => import('@/views/ai/AIInfoMapView.vue'),
        meta: { title: 'AI信息地图' }
      },
      {
        path: 'ai/mbti-report',
        name: 'MBTIReport',
        component: () => import('@/views/ai/MBTIReportView.vue'),
        meta: { title: 'MBTI报告' }
      },
      {
        path: 'permission/approval',
        name: 'Approval',
        component: () => import('@/views/permission/ApprovalView.vue'),
        meta: { title: '账号审批' }
      },
      {
        path: 'permission/audit-rule',
        name: 'AuditRule',
        component: () => import('@/views/permission/AuditRuleView.vue'),
        meta: { title: '审核规则' }
      },
      {
        path: 'permission/invitation',
        name: 'Invitation',
        component: () => import('@/views/permission/InvitationView.vue'),
        meta: { title: '邀约请示' }
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
        meta: { title: '系统配置' }
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
        meta: { title: '功能权限' }
      },
      {
        path: 'config/ai',
        name: 'AiConfig',
        component: () => import('@/views/config/AiConfigView.vue'),
        meta: { title: 'AI配置' }
      },
      {
        path: 'template',
        name: 'TemplateManage',
        component: () => import('@/views/template/TemplateManageView.vue'),
        meta: { title: '模板管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
})

export default router