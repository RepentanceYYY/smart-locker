import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 路由规则数组
const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')  // 路由懒加载
  },
 {
    path: '/borrow',
    name: 'Borrow',
    component: () => import('@/views/Borrow.vue')
  },
  {
    path: '/return',
    name: 'Return',
    component: () => import('@/views/Return.vue')
  },
  {
    path: '/inventory',
    name: 'InventoryDialog',
    component: () => import('@/views/InventoryDialog.vue')
  },
    {
        path: '/settings',
        name: 'Settings',
        component: () => import('@/views/SettingsPage.vue')
    },
    {
        path: '/log-detail',
        name: 'LogDetail',
        component: () => import('@/views/LogDetail.vue')
    },
    {
        path: '/hardware-detail',
        name: 'HardwareDetail',
        component: () => import('@/views/HardwareDetail.vue')
    },
    {
        path: '/temp-humidity-detail',
        name: 'tempHumidity',
        component: () => import('@/views/TempHumidityDetail.vue')
    }
]

// 创建 router 实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
