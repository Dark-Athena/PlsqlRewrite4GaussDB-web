<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>新建转换项目</span>
      </div>
    </template>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入项目名称"></el-input>
      </el-form-item>
      <el-form-item label="SQL文件/ZIP包" prop="file">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :limit="1"
        >
          <template #trigger>
            <el-button type="primary">选择文件</el-button>
          </template>
          <template #tip>
            <div class="el-upload__tip">
              请上传文本文件或zip压缩包
            </div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item v-if="isZip" label="并发度">
        <el-input-number v-model="form.concurrency" :min="1" :max="maxConcurrency" />
        <span style="margin-left:8px;">最大: {{ maxConcurrency }}</span>
      </el-form-item>
      <el-form-item label="输入文件字符集">
        <el-select v-model="form.inputCharset" placeholder="请选择输入字符集">
          <el-option label="UTF-8" value="UTF-8" />
          <el-option label="GBK" value="GBK" />
          <el-option label="ISO-8859-1" value="ISO-8859-1" />
        </el-select>
      </el-form-item>
      <el-form-item label="输出文件字符集">
        <el-select v-model="form.outputCharset" placeholder="请选择输出字符集">
          <el-option label="UTF-8" value="UTF-8" />
          <el-option label="GBK" value="GBK" />
          <el-option label="ISO-8859-1" value="ISO-8859-1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submitForm" :loading="loading">立即创建</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { createProject } from '../api/project';
import { ElMessage } from 'element-plus';

const router = useRouter();
const formRef = ref(null);
const uploadRef = ref(null);
const loading = ref(false);

const form = reactive({
  name: '',
  file: null,
  inputCharset: 'UTF-8',
  outputCharset: 'UTF-8',
  concurrency: 1
});

const rules = reactive({
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  file: [{ required: true, message: '请选择要上传的文件' }],
});

const isZip = ref(false);
const maxConcurrency = ref(1);

onMounted(async () => {
  // 获取最大并发度
  try {
    const res = await fetch('/api/project/maxThreads');
    const maxThreads = await res.json();
    maxConcurrency.value = Math.max(1, maxThreads - 1);
  } catch {
    maxConcurrency.value = 1;
  }
});

const handleFileChange = (uploadFile) => {
  form.file = uploadFile.raw;
  // 判断是否为zip
  isZip.value = uploadFile.raw && uploadFile.raw.type === 'application/zip' ||
    (uploadFile.raw.name && uploadFile.raw.name.toLowerCase().endsWith('.zip'));
  if (!isZip.value) {
    form.concurrency = 1;
  }
  formRef.value.validateField('file');
};

const handleFileRemove = () => {
  form.file = null;
};

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        await createProject(form.name, form.file, form.inputCharset, form.outputCharset, form.concurrency);
        ElMessage.success('项目创建成功，已开始执行');
        router.push('/projects');
      } catch (error) {
        console.error("Project creation failed:", error);
      } finally {
        loading.value = false;
      }
    }
  });
};

const resetForm = () => {
  formRef.value.resetFields();
  uploadRef.value.clearFiles();
  form.file = null;
};
</script>

<style scoped>
.card-header {
  font-size: 1.1em;
  font-weight: bold;
}
</style> 