<template>
  <el-dropdown @command="handleCommand">
    <span class="el-dropdown-link">
      <el-avatar size="small" icon="el-icon-user" />
      <span style="margin: 0 8px;">{{ user?.username || '未登录' }}</span>
      <i class="el-icon-arrow-down el-icon--right"></i>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="changePwd">修改密码</el-dropdown-item>
        <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
  <el-dialog v-model="pwdDialogVisible" title="修改密码" width="350px">
    <el-form :model="pwdForm" ref="pwdFormRef" :rules="pwdRules" label-width="80px">
      <el-form-item label="新密码" prop="password">
        <el-input v-model="pwdForm.password" type="password" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="onChangePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getCurrentUser, logout, changePassword } from '@/api/auth';
import { ElMessage } from 'element-plus';

const router = useRouter();
const user = ref(null);
const pwdDialogVisible = ref(false);
const pwdForm = ref({ password: '' });
const pwdFormRef = ref();
const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
};

onMounted(async () => {
  try {
    const res = await getCurrentUser();
    user.value = res;
  } catch {
    user.value = null;
  }
});

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    await logout();
    ElMessage.success('已退出登录');
    router.replace('/login');
  } else if (cmd === 'changePwd') {
    pwdDialogVisible.value = true;
  }
};

const onChangePwd = () => {
  pwdFormRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      await changePassword(pwdForm.value.password);
      ElMessage.success('密码修改成功');
      pwdDialogVisible.value = false;
      pwdForm.value.password = '';
    } catch (e) {
      ElMessage.error(e?.response?.data || '密码修改失败');
    }
  });
};
</script> 