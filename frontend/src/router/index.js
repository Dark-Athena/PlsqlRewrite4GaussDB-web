import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser } from '@/api/auth';
import Layout from '@/components/Layout.vue';

const routes = [{
        path: '/login',
        name: 'Login',
        component: () =>
            import ('@/views/Login.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/',
        component: Layout,
        redirect: '/projects',
        children: [{
                path: '/projects',
                name: 'ProjectList',
                component: () =>
                    import ('@/views/ProjectList.vue'),
                meta: { title: '项目管理', icon: 'list' }
            },
            {
                path: '/project/create',
                name: 'ProjectCreate',
                component: () =>
                    import ('@/views/ProjectCreate.vue'),
                meta: { title: '创建项目', icon: 'plus' },
            },
            {
                path: '/user',
                name: 'UserManagement',
                component: () =>
                    import ('@/views/UserManagement.vue'),
                meta: { title: '用户管理', icon: 'user' }
            },
            {
                path: '/group',
                name: 'GroupManagement',
                component: () =>
                    import ('@/views/GroupManagement.vue'),
                meta: { title: '用户组管理', icon: 'user-group' }
            },
            {
                path: '/sql/rewrite',
                name: 'SqlRewrite',
                component: () =>
                    import ('@/views/SqlRewrite.vue')
            },
            // {
            //     path: '/template/manage',
            //     name: 'TemplateManage',
            //     component: () =>
            //         import ('@/views/TemplateManage.vue')
            // },
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

// 路由守卫：未登录跳转登录页，已登录访问 /login 跳转首页
router.beforeEach(async(to, from, next) => {
    const isLoginPage = to.path === '/login';
    let loggedIn = false;
    try {
        await getCurrentUser();
        loggedIn = true;
    } catch {
        loggedIn = false;
    }
    if (!loggedIn && !isLoginPage) {
        // 记录原始目标页面
        next({ path: '/login', query: { redirect: to.fullPath } });
    } else if (loggedIn && isLoginPage) {
        next('/');
    } else {
        next();
    }
});

// 404 兜底路由
router.addRoute({
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () =>
        import ('@/views/NotFound.vue')
});

export default router