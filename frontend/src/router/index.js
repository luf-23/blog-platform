import { createRouter, createWebHistory } from "vue-router";
import { ElMessage } from "element-plus";

import { useTokenStore } from "../store/token.js";
import { useUserInfoStore } from "../store/userInfo.js";

import AppLayout from "../layouts/AppLayout.vue";
import AuthLayout from "../layouts/AuthLayout.vue";

const routes = [
  {
    path: "/",
    redirect: "/home"
  },
  {
    path: "/login",
    component: AuthLayout,
    children: [
      {
        path: "",
        name: "Login",
        component: () => import("../views/Login.vue"),
        meta: { title: "登录", guest: true }
      }
    ]
  },
  {
    path: "/",
    component: AppLayout,
    meta: { requireAuth: true },
    children: [
      {
        path: "home",
        name: "Home",
        component: () => import("../views/Home.vue"),
        meta: { title: "首页" }
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("../views/Profile.vue"),
        meta: { title: "个人主页" }
      },
      {
        path: "community",
        name: "Community",
        component: () => import("../views/Community.vue"),
        meta: { title: "社区" }
      },
      {
        path: "announcement",
        name: "Announcement",
        component: () => import("../views/Announcement.vue"),
        meta: { title: "系统公告" }
      },
      {
        path: "ai/chat",
        name: "Chat",
        component: () => import("../views/ai/Chat.vue"),
        meta: { title: "AI 助手" }
      },
      {
        path: "article",
        meta: { title: "我的文章" },
        children: [
          {
            path: "",
            redirect: { name: "ArticleCategory" }
          },
          {
            path: "category",
            name: "ArticleCategory",
            component: () => import("../views/article/ArticleCategory.vue"),
            meta: { title: "我的分类" }
          },
          {
            path: "list",
            name: "ArticleList",
            component: () => import("../views/article/ArticleList.vue"),
            meta: { title: "文章列表" }
          },
          {
            path: "detail",
            name: "ArticleDetail",
            component: () => import("../views/article/Article.vue"),
            meta: { title: "文章详情" }
          },
          {
            path: "add",
            name: "ArticleAdd",
            component: () => import("../views/article/SaveArticle.vue"),
            meta: { title: "新建文章" }
          },
          {
            path: "edit",
            name: "ArticleEdit",
            component: () => import("../views/article/SaveArticle.vue"),
            meta: { title: "编辑文章" }
          }
        ]
      },
      {
        path: "admin",
        meta: { title: "管理后台", requireAdmin: true },
        children: [
          {
            path: "",
            redirect: { name: "AdminHome" }
          },
          {
            path: "home",
            name: "AdminHome",
            component: () => import("../views/admin/AdminHome.vue"),
            meta: { title: "控制台" }
          },
          {
            path: "list",
            name: "ArticleListManager",
            component: () => import("../views/admin/ArticleListManager.vue"),
            meta: { title: "文章审核" }
          },
          {
            path: "detail",
            name: "ArticleDetailManager",
            component: () => import("../views/admin/ArticleManager.vue"),
            meta: { title: "文章审核详情" }
          },
          {
            path: "user",
            name: "UserManager",
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
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to, _from, next) => {
  const tokenStore = useTokenStore();
  const userInfoStore = useUserInfoStore();
  const token = tokenStore.token;

  if (to.meta.title) {
    document.title = `${to.meta.title} · Blog Platform`;
  }

  if (to.meta.guest && token) {
    next("/home");
    return;
  }

  if (to.matched.some((record) => record.meta.requireAuth) && !token) {
    next({ path: "/login", query: { redirect: to.fullPath } });
    return;
  }

  if (to.matched.some((record) => record.meta.requireAdmin)) {
    if (userInfoStore.userInfo?.username !== "admin") {
      ElMessage.warning("权限不足，仅管理员可访问");
      next("/home");
      return;
    }
  }

  next();
});

export default router;
