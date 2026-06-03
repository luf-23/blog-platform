import { createRouter, createWebHistory } from "vue-router";
import { ElMessage } from "element-plus";
import { useTokenStore } from "../store/token.js";
import { useUserInfoStore } from "../store/userInfo.js";
import AppLayout from "../layouts/AppLayout.vue";
import AuthLayout from "../layouts/AuthLayout.vue";

const routes = [
  { path: "/", redirect: "/home" },
  {
    path: "/",
    component: AuthLayout,
    children: [
      {
        path: "login",
        name: "Login",
        component: () => import("../views/Login.vue"),
        meta: { title: "登录", guest: true }
      }
    ]
  },
  {
    path: "/",
    component: AppLayout,
    children: [
      // Public routes
      {
        path: "home",
        name: "Home",
        component: () => import("../views/Home.vue"),
        meta: { title: "发现" }
      },
      {
        path: "article/:id",
        name: "ArticleDetail",
        component: () => import("../views/article/Article.vue"),
        meta: { title: "文章详情" }
      },
      {
        path: "announcement",
        name: "Announcement",
        component: () => import("../views/Announcement.vue"),
        meta: { title: "系统公告" }
      },
      {
        path: "profile/:username?",
        name: "Profile",
        component: () => import("../views/Profile.vue"),
        meta: { title: "个人主页" }
      },
      {
        path: "ai/chat",
        name: "Chat",
        component: () => import("../views/ai/Chat.vue"),
        meta: { title: "AI 助手", requireAuth: true }
      },
      // Auth-required routes
      {
        path: "article",
        meta: { requireAuth: true },
        children: [
          { path: "", redirect: { name: "MyArticles" } },
          {
            path: "my",
            name: "MyArticles",
            component: () => import("../views/article/ArticleList.vue"),
            meta: { title: "我的博客" }
          },
          {
            path: "categories",
            name: "ArticleCategories",
            component: () => import("../views/article/ArticleCategory.vue"),
            meta: { title: "分类管理" }
          },
          {
            path: "write",
            name: "ArticleWrite",
            component: () => import("../views/article/SaveArticle.vue"),
            meta: { title: "写文章" }
          },
          {
            path: "edit/:id",
            name: "ArticleEdit",
            component: () => import("../views/article/SaveArticle.vue"),
            meta: { title: "编辑文章" }
          }
        ]
      },
      // Admin routes
      {
        path: "admin",
        meta: { requireAuth: true, requireAdmin: true },
        children: [
          { path: "", redirect: { name: "AdminHome" } },
          {
            path: "home",
            name: "AdminHome",
            component: () => import("../views/admin/AdminHome.vue"),
            meta: { title: "管理控制台" }
          },
          {
            path: "articles",
            name: "AdminArticles",
            component: () => import("../views/admin/ArticleListManager.vue"),
            meta: { title: "文章管理" }
          },
          {
            path: "users",
            name: "AdminUsers",
            component: () => import("../views/admin/UseManager.vue"),
            meta: { title: "用户管理" }
          }
        ]
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: () => import("../views/default/NotFound.vue"),
    meta: { title: "页面未找到" }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 }; }
});

router.beforeEach((to, _from, next) => {
  const tokenStore = useTokenStore();
  const userInfoStore = useUserInfoStore();
  const token = tokenStore.token;

  if (to.meta.title) {
    document.title = `${to.meta.title} · Blog Platform`;
  }

  if (to.meta.guest && token) { next("/home"); return; }

  if (to.matched.some(r => r.meta.requireAuth) && !token) {
    next({ path: "/login", query: { redirect: to.fullPath } });
    return;
  }

  if (to.matched.some(r => r.meta.requireAdmin)) {
    const u = userInfoStore.userInfo;
    if (!u || (u.username !== 'admin' && u.role !== 'admin')) {
      ElMessage.warning("权限不足，仅管理员可访问");
      next("/home");
      return;
    }
  }

  next();
});

export default router;
