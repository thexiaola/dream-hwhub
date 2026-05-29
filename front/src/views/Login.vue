<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="grid-lines"></div>
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="stars"></div>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <div class="logo-icon">
            <component :is="componentMap['GraduationCap']" />
          </div>
          <span class="logo-text">作业管理系统</span>
        </div>
        <p class="subtitle">欢迎回来，开始新的学习之旅</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="account">
          <div class="input-wrapper">
            <component :is="componentMap['User']" class="input-icon" />
            <el-input
              v-model="form.account"
              placeholder="请输入账号（学号/工号或邮箱）"
              class="custom-input"
            />
          </div>
        </el-form-item>

        <el-form-item prop="password">
          <div class="input-wrapper">
            <component :is="componentMap['Lock']" class="input-icon" />
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              class="custom-input"
              show-password
            />
          </div>
        </el-form-item>

        <el-form-item class="login-actions">
          <el-button
            type="primary"
            @click="handleLogin"
            :loading="loading"
            class="login-btn"
          >
            登 录
          </el-button>
        </el-form-item>

        <div class="login-links">
          <router-link to="/register" class="link">注册账号</router-link>
          <router-link to="/retrieve" class="link">忘记密码</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "../stores/user";
import { ElMessage } from "element-plus";
import { User, Lock, BookOpen } from "lucide-vue-next";

const componentMap = {
  User,
  Lock,
  BookOpen,
};

const router = useRouter();
const userStore = useUserStore();

const form = reactive({
  account: "",
  password: "",
});

const loading = ref(false);

const rules = {
  account: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

async function handleLogin() {
  if (!form.account || !form.password) {
    ElMessage.error("请填写账号和密码");
    return;
  }

  loading.value = true;

  try {
    const response = await userStore.login(form.account, form.password);
    if (response.code === 200) {
      ElMessage.success("登录成功");
      router.push("/dashboard");
    } else {
      ElMessage.error(response.message);
      form.password = "";
    }
  } catch (error) {
    ElMessage.error(error.message || "登录失败");
    form.password = "";
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(180deg, #0a0a1a 0%, #1a1a3a 50%, #0a0a1a 100%);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(102, 126, 234, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(102, 126, 234, 0.05) 1px, transparent 1px);
  background-size: 60px 60px;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #667eea 0%, transparent 100%);
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #764ba2 0%, transparent 100%);
  bottom: -50px;
  left: -50px;
  animation: float 6s ease-in-out infinite reverse;
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(
      2px 2px at 20px 30px,
      rgba(255, 255, 255, 0.4),
      transparent
    ),
    radial-gradient(
      2px 2px at 40px 70px,
      rgba(255, 255, 255, 0.3),
      transparent
    ),
    radial-gradient(
      1px 1px at 90px 40px,
      rgba(255, 255, 255, 0.5),
      transparent
    ),
    radial-gradient(
      2px 2px at 130px 80px,
      rgba(255, 255, 255, 0.3),
      transparent
    ),
    radial-gradient(
      1px 1px at 160px 120px,
      rgba(255, 255, 255, 0.4),
      transparent
    ),
    radial-gradient(
      2px 2px at 200px 50px,
      rgba(255, 255, 255, 0.3),
      transparent
    ),
    radial-gradient(
      1px 1px at 250px 150px,
      rgba(255, 255, 255, 0.4),
      transparent
    ),
    radial-gradient(
      2px 2px at 300px 90px,
      rgba(255, 255, 255, 0.3),
      transparent
    ),
    radial-gradient(
      1px 1px at 350px 180px,
      rgba(255, 255, 255, 0.5),
      transparent
    ),
    radial-gradient(
      2px 2px at 400px 60px,
      rgba(255, 255, 255, 0.3),
      transparent
    );
  background-repeat: repeat;
  background-size: 450px 200px;
}

.login-card {
  width: 420px;
  padding: 48px;
  background: rgba(18, 18, 42, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 20px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.5),
    0 0 40px rgba(102, 126, 234, 0.1);
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.logo-text {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #a0a0c0;
  font-size: 14px;
}

.login-form {
  margin-top: 20px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  padding: 0 16px;
  transition: all 0.3s ease;
}

.input-wrapper:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 20px rgba(102, 126, 234, 0.2);
}

.input-icon {
  color: #667eea;
  font-size: 18px;
  margin-right: 12px;
}

.custom-input {
  flex: 1;
  background: transparent;
  border: none;
  color: white;
  font-size: 14px;
  padding: 14px 0;
}

.custom-input::placeholder {
  color: #606080;
}

.login-actions {
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.login-links {
  display: flex;
  justify-content: center;
  gap: 32px;
}

.link {
  color: #a0a0c0;
  font-size: 14px;
  text-decoration: none;
  transition: color 0.3s ease;
}

.link:hover {
  color: #667eea;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}
</style>
