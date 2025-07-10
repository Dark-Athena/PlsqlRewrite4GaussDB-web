<script setup>
import UserDropdown from '@/components/UserDropdown.vue';
import { ref, onMounted } from 'vue';
import { getCurrentUser } from '@/api/auth';

const user = ref(null);
onMounted(async () => {
  try {
    user.value = await getCurrentUser();
  } catch {
    user.value = null;
  }
});
</script>

<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="logo-title">
        <img alt="logo" src="/vite.svg" style="height: 40px; margin-right: 10px;"/>
        <span>PlsqlRewrite4GaussDB</span>
      </div>
      <div class="user-info">
        <UserDropdown />
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px">
        <el-scrollbar>
          <el-menu :default-openeds="['1']" router>
            <el-sub-menu index="1">
              <template #title>
                <el-icon><Memo /></el-icon>项目管理
              </template>
              <el-menu-item index="/projects">项目列表</el-menu-item>
              <el-menu-item index="/project/create">新建项目</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/sql/rewrite">
              <el-icon><Switch /></el-icon>
              <span>SQL在线转换</span>
            </el-menu-item>
            <!--
            <el-menu-item index="/template/manage">
              <el-icon><Files /></el-icon>
              <span>参数模板管理</span>
            </el-menu-item>
            -->
            <el-sub-menu index="2" v-if="user && user.roles && user.roles.includes('ADMIN')">
              <template #title>
                <el-icon><Setting /></el-icon>系统管理
              </template>
              <el-menu-item index="/user">用户管理</el-menu-item>
              <el-menu-item index="/group">用户组管理</el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-scrollbar>
      </el-aside>
      <el-main>
        <el-scrollbar>
          <!-- 路由视图，用于显示子页面 -->
          <router-view></router-view>
        </el-scrollbar>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #409eff;
  color: #fff;
  border-bottom: 1px solid #dcdfe6;
}

.logo-title {
  display: flex;
  align-items: center;
  font-size: 1.2em;
  font-weight: bold;
}

.user-info {
  cursor: pointer;
}

.el-dropdown-link {
  color: #fff;
  display: flex;
  align-items: center;
}

.el-aside {
  color: var(--el-text-color-primary);
  background: var(--el-color-primary-light-9);
}

.el-menu {
  border-right: none;
}

.el-main {
  padding: 20px;
}
</style> 