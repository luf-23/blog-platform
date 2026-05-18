import axios from "axios";
import { ElMessage } from "element-plus";
import { useTokenStore } from "../store/token.js";
import router from "../router/index.js";
//const baseURL = "http://43.142.2.253/api/";
//const baseURL = "http://luf.woyioii.cn/api/";
const baseURL = "http://localhost:8080/";
//const baseURL = "http://192.168.159.105:8080/";
const request = axios.create({
  baseURL,
  timeout: 6000,
  withCredentials: true // 确保发送Cookie
});

// 标记是否正在刷新token
let isRefreshing = false;
// 存储因为token过期而失败的请求
let failedQueue = [];

// 处理队列中的请求
const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });

  failedQueue = [];
};

// 刷新token的函数
const refreshToken = async () => {
  try {
    const response = await axios.get(`${baseURL}user/refreshToken`, {
      withCredentials: true,
      timeout: 6000
    });

    if (response.data.code === 0) {
      return response.data.data; // 返回新的accessToken
    } else {
      throw new Error("刷新token失败");
    }
  } catch (error) {
    throw error;
  }
};

//响应拦截器
request.interceptors.response.use(
  (result) => {
    if (result.data.code == 0) {
      return result.data;
    }

    ElMessage.error(result.data.message ? result.data.message : "请求失败");
    return Promise.reject(result.data);
  },
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 如果正在刷新token，将请求加入队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => {
            return request(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // 尝试刷新token
        const newAccessToken = await refreshToken();
        const tokenStore = useTokenStore();
        tokenStore.setToken(newAccessToken);

        // 处理队列中的请求
        processQueue(null, newAccessToken);

        // 重试原始请求
        originalRequest.headers.Authorization = newAccessToken;
        return request(originalRequest);
      } catch (refreshError) {
        // 刷新失败，清除token并跳转到登录页
        processQueue(refreshError, null);
        const tokenStore = useTokenStore();
        tokenStore.removeToken();

        ElMessage.error("登录已过期，请重新登录");
        const currentPath = router.currentRoute.value.fullPath;
        router.push({
          path: "/login",
          query: {
            redirect: currentPath
          }
        });
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    ElMessage.error(error.message || "未知错误");
    return Promise.reject(error);
  }
);

//请求拦截器
request.interceptors.request.use(
  (config) => {
    const tokenStore = useTokenStore();
    const token = tokenStore.token; // 获取 accessToken
    if (token) {
      config.headers.Authorization = token;
    }
    return config;
  },
  (error) => {
    console.error("Request Error:", error);
    return Promise.reject(error);
  }
);

export default request;

export const getBaseURL = () => {
  return baseURL;
};
