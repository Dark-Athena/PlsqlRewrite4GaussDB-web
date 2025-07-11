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
      <el-form-item label="SQL文件/压缩包" prop="file">
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
              请上传文本文件或压缩包（支持zip, tar, tar.gz, tar.bz2, tgz, tbz2, gz, bz2）
            </div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item v-if="isZip" label="并发度">
        <el-input-number v-model="form.concurrency" :min="1" :max="maxConcurrency" />
        <span style="margin-left:8px;">最大: {{ maxConcurrency }}</span>
      </el-form-item>
      <el-form-item v-if="isZip" label="文件后缀配置">
        <el-tag
          v-for="(suffix, index) in form.fileExtensions"
          :key="index"
          closable
          @close="removeSuffix(index)"
          style="margin-right: 8px; margin-bottom: 8px;"
        >
          {{ suffix }}
        </el-tag>
        <el-input
          v-if="inputVisible"
          ref="inputRef"
          v-model="inputValue"
          size="small"
          style="width: 100px;"
          @keyup.enter="handleInputConfirm"
          @blur="handleInputConfirm"
        />
        <el-button v-else size="small" @click="showInput">+ 添加后缀</el-button>
        <div class="el-form-item__tip" style="margin-top: 8px;">
          支持的文件后缀，不需要包含点号（如：sql, pck, spc, bdy等）
        </div>
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
import { ref, reactive, onMounted, nextTick } from 'vue';
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
  concurrency: 1,
  fileExtensions: ['sql', 'pck', 'spc', 'bdy', 'fnc', 'prc', 'trg', 'pkg', 'pkb', 'pkh', 'pks', 'plb', 'pls', 'tps', 'tpb']
});

const rules = reactive({
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  file: [{ required: true, message: '请选择要上传的文件' }],
});

const isZip = ref(false);
const maxConcurrency = ref(1);
const inputVisible = ref(false);
const inputValue = ref('');
const inputRef = ref(null);

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
  // 判断是否为压缩包文件（zip或tar）
  const fileName = uploadFile.raw.name ? uploadFile.raw.name.toLowerCase() : '';
  isZip.value = uploadFile.raw && (
    uploadFile.raw.type === 'application/zip' ||
    uploadFile.raw.type === 'application/gzip' ||
    uploadFile.raw.type === 'application/x-gzip' ||
    uploadFile.raw.type === 'application/x-tar' ||
    fileName.endsWith('.zip') ||
    fileName.endsWith('.tar') ||
    fileName.endsWith('.tar.gz') ||
    fileName.endsWith('.tgz') ||
    fileName.endsWith('.tar.bz2') ||
    fileName.endsWith('.tbz2') ||
    fileName.endsWith('.gz') ||
    fileName.endsWith('.bz2')
  );
  if (!isZip.value) {
    form.concurrency = 1;
  }
  formRef.value.validateField('file');
};

const handleFileRemove = () => {
  form.file = null;
};

const removeSuffix = (index) => {
  form.fileExtensions.splice(index, 1);
};

const showInput = () => {
  inputVisible.value = true;
  inputValue.value = '';
  // 使用 nextTick 确保DOM更新后再聚焦
  nextTick(() => {
    inputRef.value && inputRef.value.focus();
  });
};

const handleInputConfirm = () => {
  if (inputValue.value) {
    const suffix = inputValue.value.toLowerCase().trim();
    if (suffix && !form.fileExtensions.includes(suffix)) {
      form.fileExtensions.push(suffix);
    }
  }
  inputVisible.value = false;
  inputValue.value = '';
};

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        await createProject(form.name, form.file, form.inputCharset, form.outputCharset, form.concurrency, form.fileExtensions);
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
  form.fileExtensions = ['sql', 'pck', 'spc', 'bdy', 'fnc', 'prc', 'trg', 'pkg', 'pkb', 'pkh', 'pks', 'plb', 'pls', 'tps', 'tpb'];
  isZip.value = false;
};
</script>

<style scoped>
.card-header {
  font-size: 1.1em;
  font-weight: bold;
}
</style> 