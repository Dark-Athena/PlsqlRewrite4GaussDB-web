<template>
  <div class="login-container">
    <div class="login-background">
      <div class="login-form-container">
        <div class="login-header">
          <img src="/vite.svg" alt="Logo" class="login-logo" />
          <h1 class="login-title">PlsqlRewrite4GaussDB</h1>
          <p class="login-subtitle">PL/SQL 转换管理系统</p>
        </div>
        <el-card class="login-card" shadow="hover">
          <h2 class="card-title">用户登录</h2>
          <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="onLogin">
            <el-form-item prop="username">
              <el-input 
                v-model="form.username" 
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input 
                v-model="form.password" 
                type="password" 
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="onLogin" 
                :loading="loading" 
                size="large"
                style="width:100%"
              >
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock } from '@element-plus/icons-vue';
import { login } from '@/api/auth';

const route = useRoute();
const router = useRouter();
const formRef = ref();
const loading = ref(false);
const form = ref({ username: '', password: '' });
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const onLogin = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await login(form.value);
      ElMessage.success('登录成功');
      const redirect = route.query.redirect || '/';
      router.replace(redirect);
    } catch (e) {
      ElMessage.error('用户名或密码错误');
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-background {
  width: 100%;
  max-width: 1200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form-container {
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  color: white;
}

.login-logo {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
  filter: brightness(0) invert(1);
}

.login-title {
  font-size: 2.5em;
  font-weight: 700;
  margin-bottom: 10px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.login-subtitle {
  font-size: 1.1em;
  opacity: 0.9;
  margin: 0;
}

.login-card {
  border-radius: 15px;
  border: none;
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
}

.card-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 1.5em;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 600;
  height: 48px;
  font-size: 16px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .login-title {
    font-size: 2em;
  }
  
  .login-form-container {
    max-width: 100%;
  }
}
</style> 