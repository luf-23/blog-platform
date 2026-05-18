<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { check } from "../../utils/admin/check";
import { getUsersService } from "../../api/admin";
check();
const router = useRouter();
const userList = ref([
  {
    userId: "",
    username: "",
    nickname: "",
    signature: "",
    avatarImage: "",
    backgroundImage: "",
    lastLoginTime: "",
    lastLoginIp: "",
    createTime: "",
    updateTime: ""
  }
]);
const isLoading = ref(false);
const getUsers = async () => {
  const result = await getUsersService();
  userList.value = result.data.map((item) => {
    return {
      userId: item.userId,
      username: item.username,
      nickname: item.nickname,
      signature: item.signature,
      avatarImage: item.avatarImage,
      backgroundImage: item.backgroundImage,
      lastLoginTime: item.lastLogin,
      lastLoginIp: item.lastLoginIp,
      createTime: item.createTime,
      updateTime: item.updateTime
    };
  });
};
try {
  isLoading.value = true;
  getUsers();
} catch {
  console.log("获取用户信息列表失败");
} finally {
  isLoading.value = false;
}
const handleClick = (item) => {
  router.push({
    name: "Profile",
    query: {
      author: item.username
    }
  });
};
</script>

<template>
  <div class="user-manager-container">
    <h1 class="page-title">用户管理</h1>

    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div class="user-grid" v-else>
      <div
        class="user-card"
        v-for="item in userList"
        :key="item.userId"
        :style="{
          '--bg-image': item.backgroundImage
            ? `url(${item.backgroundImage})`
            : 'none'
        }"
        @click="() => handleClick(item)"
      >
        <div class="card-bg"></div>

        <div class="card-header">
          <div class="avatar-container">
            <img
              :src="
                item.avatarImage || 'http://luf.woyioii.cn/avatar/avatar1.png'
              "
              alt="用户头像"
              class="avatar"
              @error="
                (e) =>
                  (e.target.src = 'http://luf.woyioii.cn/avatar/avatar1.png')
              "
            />
          </div>
          <div class="user-basic">
            <h3 class="username">{{ item.nickname || item.username }}</h3>
            <p class="user-id">ID: {{ item.userId }}</p>
          </div>
        </div>

        <div class="card-body">
          <p class="signature" v-if="item.signature">"{{ item.signature }}"</p>
          <p class="signature placeholder" v-else>该用户还没有签名</p>

          <div class="info-grid">
            <div class="info-item">
              <span class="label">用户名:</span>
              <span class="value">{{ item.username }}</span>
            </div>

            <div class="info-item">
              <span class="label">最后登录:</span>
              <span class="value">{{ item.lastLoginTime || "暂无记录" }}</span>
            </div>

            <div class="info-item">
              <span class="label">登录IP:</span>
              <span class="value">{{ item.lastLoginIp || "未知" }}</span>
            </div>

            <div class="info-item">
              <span class="label">注册时间:</span>
              <span class="value">{{ item.createTime }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <span class="update-time">最后更新: {{ item.updateTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  color: #2c3e50;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 25px;
}

.user-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
}

.user-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.card-bg {
  height: 140px;
  background-image: var(
    --bg-image,
    linear-gradient(135deg, #667eea 0%, #764ba2 100%)
  );
  background-size: cover;
  background-position: center;
}

.card-header {
  display: flex;
  align-items: flex-end;
  padding: 20px;
  position: relative;
  margin-top: -30px;
}

.avatar-container {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 15px;
  border: 3px solid white;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
  background: #f5f7fa;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-basic {
  flex: 1;
  margin-bottom: 5px;
}

.username {
  margin: 0;
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
}

.user-id {
  margin: 5px 0 0;
  color: #7f8c8d;
  font-size: 13px;
}

.card-body {
  padding: 0 20px 15px;
}

.signature {
  font-style: italic;
  color: #555;
  margin: 0 0 20px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #4e73ff;
  font-size: 14px;
  line-height: 1.5;
}

.signature.placeholder {
  color: #aaa;
  font-style: normal;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  font-size: 14px;
}

.label {
  color: #7f8c8d;
  min-width: 80px;
  font-weight: 500;
}

.value {
  color: #34495e;
  flex: 1;
  word-break: break-word;
}

.card-footer {
  padding: 12px 20px;
  background: #f8f9fa;
  border-top: 1px solid #eee;
  text-align: right;
}

.update-time {
  font-size: 12px;
  color: #95a5a6;
}

@media (max-width: 768px) {
  .user-grid {
    grid-template-columns: 1fr;
  }

  .container {
    padding: 15px;
  }
}
</style>
