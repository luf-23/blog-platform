import { createRouter, createWebHistory } from "vue-router";
import { useTokenStore } from "../store/token.js";
import Login from "../views/Login.vue";
import Home from "../views/Home.vue";
import Profile from "../views/Profile.vue";
import ArticleCategory from "../views/article/ArticleCategory.vue";
import ArticleList from "../views/article/ArticleList.vue";
import Article from "../views/article/Article.vue";
import SaveArticle from "../views/article/SaveArticle.vue";
import NotFound from "../views/default/NotFound.vue";
import Community from "../views/Community.vue";
import Announcement from "../views/Announcement.vue";
import Chat from "../views/ai/Chat.vue";
import ArticleListManager from "../views/admin/ArticleListManager.vue";
import ArticleManager from "../views/admin/ArticleManager.vue";
import UseManager from "../views/admin/UseManager.vue";
import AdminHome from "../views/admin/AdminHome.vue";
const routes = [
  {
    path: "/",
    redirect: "/home"
  },
  {
    path: "/login",
    name: "Login",
    component: Login,
    meta: {
      title: "登录"
    }
  },
  {
    path: "/home",
    name: "Home",
    meta: {
      title: "首页",
      requireAuth: true
    },
    component: Home
  },
  {
    path: "/profile",
    name: "Profile",
    meta: {
      title: "用户信息",
      requireAuth: true
    },
    component: Profile
  },
  {
    path: "/article",
    name: "Article",
    meta: {
      title: "文章管理",
      requireAuth: true
    },
    children: [
      {
        path: "list",
        name: "ArticleList",
        meta: {
          title: "文章列表"
        },
        component: ArticleList
      },
      {
        path: "detail",
        name: "ArticleDetail",
        meta: {
          title: "文章详情"
        },
        component: Article
      },
      {
        path: "category",
        name: "ArticleCategory",
        meta: {
          title: "文章分类"
        },
        component: ArticleCategory
      },
      {
        path: "add",
        name: "ArticleAdd",
        meta: {
          title: "添加文章"
        },
        component: SaveArticle
      },
      {
        path: "edit",
        name: "ArticleEdit",
        meta: {
          title: "编辑文章"
        },
        component: SaveArticle
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: NotFound
  },
  {
    path: "/announcement",
    name: "Announcement",
    meta: {
      title: "公告",
      requireAuth: true
    },
    component: Announcement
  },
  {
    path: "/community",
    name: "Community",
    meta: {
      title: "社区",
      requireAuth: true
    },
    component: Community
  },
  {
    path: "/ai/chat",
    name: "Chat",
    meta: {
      title: "对话",
      requireAuth: true
    },
    component: Chat
  },
  {
    path: "/admin",
    name: "Admin",
    meta: {
      title: "管理员操作",
      requireAuth: true
    },
    children: [
      {
        path: "home",
        name: "AdminHome",
        meta: {
          title: "管理员界面"
        },
        component: AdminHome
      },
      {
        path: "list",
        name: "ArticleListManager",
        meta: {
          title: "文章列表"
        },
        component: ArticleListManager
      },
      {
        path: "detail",
        name: "ArticleDetailManager",
        meta: {
          title: "文章详情"
        },
        component: ArticleManager
      },
      {
        path: "user",
        name: "UserManager",
        meta: {
          title: "管理用户信息"
        },
        component: UseManager
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const tokenStore = useTokenStore();
  const token = tokenStore.token; // 获取 token

  // 如果已登录且访问登录页，重定向到首页
  if (to.path === "/login" && token) {
    next("/home");
    return;
  }

  // 检查目标路由是否需要认证
  if (to.meta.requireAuth) {
    // 检查用户是否已登录（这里检查 localStorage 中的 token）
    if (!token) {
      // 如果没有 token，重定向到登录页面
      next({
        path: "/login",
        query: { redirect: to.fullPath } // 保存目标路径，登录后可以跳转回来
      });
    } else {
      // 如果有 token，允许访问
      next();
    }
  } else {
    // 不需要认证的路由直接放行
    next();
  }
});

export default router;
