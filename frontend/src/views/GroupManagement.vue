<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>用户组管理</span>
        <el-button type="primary" @click="handleCreate">添加用户组</el-button>
      </div>
    </template>
    <el-table :data="groupList" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="name" label="组名" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="30%">
    <el-form ref="groupFormRef" :model="groupForm" :rules="rules" label-width="80px">
      <el-form-item label="组名" prop="name">
        <el-input v-model="groupForm.name" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue';
import { getGroupList, createGroup, updateGroup, deleteGroup } from '@/api/group';
import { ElMessage, ElMessageBox } from 'element-plus';

const groupList = ref([]);
const loading = ref(true);
const dialogVisible = ref(false);
const groupFormRef = ref(null);
const groupForm = reactive({
  id: null,
  name: '',
});
const rules = reactive({
  name: [{ required: true, message: '请输入组名', trigger: 'blur' }],
});

const isEdit = computed(() => !!groupForm.id);
const dialogTitle = computed(() => (isEdit.value ? '编辑用户组' : '添加用户组'));

const fetchGroups = async () => {
  loading.value = true;
  try {
    const res = await getGroupList();
    groupList.value = res;
  } finally {
    loading.value = false;
  }
};

onMounted(fetchGroups);

const resetForm = () => {
  groupForm.id = null;
  groupForm.name = '';
};

const handleCreate = () => {
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  groupForm.id = row.id;
  groupForm.name = row.name;
  dialogVisible.value = true;
};

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除此用户组吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteGroup(id);
    ElMessage.success('删除成功');
    await fetchGroups();
  });
};

const submitForm = () => {
  groupFormRef.value.validate(async (valid) => {
    if (valid) {
      if (isEdit.value) {
        await updateGroup(groupForm.id, { name: groupForm.name });
        ElMessage.success('更新成功');
      } else {
        await createGroup({ name: groupForm.name });
        ElMessage.success('创建成功');
      }
      dialogVisible.value = false;
      await fetchGroups();
    }
  });
};
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 