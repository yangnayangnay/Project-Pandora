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
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/UserManageView.vue'),
        meta: { title: '人员管理' }
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
        component: () => import('@/views/ai/MBTIReportView.vue'),
        meta: { title: 'AI分析' }
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
        path: 'config',
        name: 'Config',
        component: () => import('@/views/config/ThemeConfigView.vue'),
        meta: { title: '系统配置' }
      },
      {
        path: 'config/audit-log',
        name: 'AuditLog',
        component: () => import('@/views/config/AuditLogView.vue'),
        meta: { title: '审计日志' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  // ── 登录验证已注释（调试模式） ──
  next()
  /* 
  const token = getToken()
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/')
  } else {
    next()
  }
  ── 登录验证已注释结束 ── */
})

export default router