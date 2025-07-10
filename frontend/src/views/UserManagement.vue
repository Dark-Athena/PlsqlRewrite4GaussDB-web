<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <el-button type="primary" @click="handleCreate">添加用户</el-button>
      </div>
    </template>
    <el-table :data="userList" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="userGroup.name" label="用户组" />
      <el-table-column prop="roles" label="角色">
        <template #default="scope">
          <el-tag v-for="role in (scope.row.roles ? scope.row.roles.split(',') : [])" :key="role" style="margin-right: 5px;">
            {{ role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="30%">
      <el-form ref="userFormRef" :model="userForm" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" :placeholder="isEdit ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-input v-model="userForm.roles" />
        </el-form-item>
        <el-form-item label="用户组" prop="userGroupId">
          <el-select v-model="userForm.userGroupId" placeholder="请选择用户组">
            <el-option
                v-for="group in groupList"
                :key="group.id"
                :label="group.name"
                :value="group.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue';
import { getUserList, createUser, updateUser, deleteUser } from '@/api/user';
import { getGroupList } from '@/api/group';
import { ElMessage, ElMessageBox } from 'element-plus';

const userList = ref([]);
const groupList = ref([]);
const loading = ref(true);
const dialogVisible = ref(false);
const userFormRef = ref(null);

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  roles: 'USER',
  userGroupId: null
});

const isEdit = computed(() => !!userForm.id);
const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '添加新用户'));

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: !isEdit.value, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  roles: [{ required: true, message: '请输入角色', trigger: 'blur' }],
  userGroupId: [{ required: true, message: '请选择用户组', trigger: 'change' }]
}));

const fetchUsers = async () => {
  loading.value = true;
  try {
    userList.value = await getUserList();
    groupList.value = await getGroupList();
  } catch (error) {
    console.error("Failed to fetch users:", error);
  } finally {
    loading.value = false;
  }
};

const fetchGroups = async () => {
  try {
    groupList.value = await getGroupList();
  } catch (error) {
    ElMessage.error('获取用户组列表失败');
  }
}

const resetForm = () => {
  userForm.id = null;
  userForm.username = '';
  userForm.password = '';
  userForm.roles = 'USER';
  userForm.userGroupId = null;
}

const handleCreate = () => {
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  userForm.id = row.id;
  userForm.username = row.username;
  userForm.password = ''; // Do not show old password
  userForm.roles = row.roles;
  userForm.userGroupId = row.userGroup ? row.userGroup.id : null;
  dialogVisible.value = true;
};

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteUser(id);
    ElMessage.success('删除成功');
    await fetchUsers();
  });
};

const submitForm = () => {
  userFormRef.value.validate(async (valid) => {
    if (valid) {
      const data = { ...userForm };
      if (isEdit.value && !data.password) {
        delete data.password; // Don't send empty password
      }

      try {
        if (isEdit.value) {
          await updateUser(data.id, data);
          ElMessage.success('更新成功');
        } else {
          await createUser(data);
          ElMessage.success('用户创建成功');
        }
        dialogVisible.value = false;
        await fetchUsers();
      } catch (error) {
        ElMessage.error('操作失败');
      }
    }
  });
};

onMounted(() => {
  fetchUsers();
  fetchGroups();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 