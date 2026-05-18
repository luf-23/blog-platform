<template>
  <div class="not-found-container">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <div class="error-message">
        <h2>抱歉，页面未找到</h2>
        <p>您访问的页面可能已被移除、更名或暂时不可用</p>
      </div>
      <button class="home-button" @click="goHome">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

const goHome = () => {
  router.push("/home");
};
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f8f9fa;
}

.error-content {
  text-align: center;
  padding: 2rem;
  animation: fadeIn 0.5s ease-in;
}

.error-code {
  font-size: 8rem;
  font-weight: bold;
  color: #dc3545;
  margin: 0;
  line-height: 1;
  animation: shake 0.5s ease-in-out;
}

.error-message {
  margin: 2rem 0;
}

.error-message h2 {
  color: #343a40;
  font-size: 2rem;
  margin-bottom: 1rem;
}

.error-message p {
  color: #6c757d;
  font-size: 1.1rem;
}

.home-button {
  padding: 0.8rem 2rem;
  font-size: 1.1rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.home-button:hover {
  background-color: #0056b3;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-10px);
  }
  75% {
    transform: translateX(10px);
  }
}
</style>
