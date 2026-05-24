import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Dashboard from '../views/Dashboard.vue'
import Board from '../views/Board.vue'
import HistoryDetail from '../views/HistoryDetail.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/dashboard', component: Dashboard, meta: { requiresAuth: true } },
  { path: '/board/:classroomId', component: Board, meta: { requiresAuth: true } },
  { path: '/history/:classroomId', component: HistoryDetail, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫：未登录重定向到 /login
import { useAuthStore } from '../stores/auth'
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.token) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router