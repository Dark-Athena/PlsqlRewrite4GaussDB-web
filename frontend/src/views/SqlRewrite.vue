<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>SQL 在线转换</span>
      </div>
    </template>
    <div class="converter-container">
      <el-input
        v-model="sourceSql"
        type="textarea"
        :rows="15"
        placeholder="请输入Oracle SQL..."
        class="sql-textarea"
      />
      <div class="actions">
        <el-button type="primary" @click="handleRewrite" :loading="loading">
          转换 <el-icon class="el-icon--right"><Right /></el-icon>
        </el-button>
        <div class="timeout-config">
          <label style="font-size: 12px; color: #666;">超时时间（秒）</label>
          <el-input-number v-model="timeoutSeconds" :min="5" :max="300" size="small" style="width: 100px;" />
        </div>
        <el-button type="info" style="margin-top:12px;" @click="showDiffDialog = true" :disabled="!sourceSql || !rewrittenSql">显示差异</el-button>
        <el-button type="warning" style="margin-top:12px;" @click="clearAll">清空</el-button>
      </div>
      <el-input
        v-model="rewrittenSql"
        type="textarea"
        :rows="15"
        placeholder="转换后的GaussDB SQL将显示在这里..."
        readonly
        class="sql-textarea"
      />
    </div>
    <el-dialog v-model="showDiffDialog" title="高亮差异" width="90vw">
      <div v-html="diffHtml"></div>
      <template #footer>
        <el-button @click="showDiffDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { rewriteSql } from '../api/sql';
import { ElMessage } from 'element-plus';
import { Right } from '@element-plus/icons-vue';
import { createTwoFilesPatch } from 'diff';
import * as Diff2Html from 'diff2html';
import 'diff2html/bundles/css/diff2html.min.css';

const LOCAL_KEY = 'sql_rewrite_last_content';
const sourceSql = ref('');
const rewrittenSql = ref('');
const loading = ref(false);
const showDiffDialog = ref(false);
const diffHtml = ref('');
const timeoutSeconds = ref(20);

onMounted(() => {
  // 页面加载时自动恢复上次内容
  const saved = localStorage.getItem(LOCAL_KEY);
  if (saved) {
    try {
      const obj = JSON.parse(saved);
      sourceSql.value = obj.sourceSql || '';
      rewrittenSql.value = obj.rewrittenSql || '';
    } catch {}
  }
});

watch([sourceSql, rewrittenSql], ([src, rew]) => {
  // 输入或转换后自动保存
  localStorage.setItem(LOCAL_KEY, JSON.stringify({ sourceSql: src, rewrittenSql: rew }));
});

watch(showDiffDialog, (val) => {
  if (val) {
    const diff = createTwoFilesPatch('原始SQL', '转换后SQL', sourceSql.value || '', rewrittenSql.value || '');
    diffHtml.value = Diff2Html.html(diff, { drawFileList: false, matching: 'lines', outputFormat: 'side-by-side' });
  }
});

const handleRewrite = async () => {
  if (!sourceSql.value.trim()) {
    ElMessage.warning('请输入需要转换的SQL');
    return;
  }
  loading.value = true;
  rewrittenSql.value = ''; // 清空上次的结果
  try {
    const response = await rewriteSql(sourceSql.value, timeoutSeconds.value);
    rewrittenSql.value = response;
  } catch (error) {
    let errorMsg = '转换失败，请检查后端服务';
    if (error.response?.status === 408) {
      errorMsg = '转换超时，请尝试增加超时时间或简化SQL语句';
    } else if (error.response?.data) {
      errorMsg = error.response.data;
    }
    ElMessage.error(errorMsg);
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const clearAll = () => {
  sourceSql.value = '';
  rewrittenSql.value = '';
  localStorage.removeItem(LOCAL_KEY);
};
</script>

<style scoped>
.card-header {
  font-size: 1.1em;
  font-weight: bold;
}
.converter-container {
  display: flex;
  gap: 20px;
  align-items: center;
}
.sql-textarea {
  flex: 1;
  font-family: 'Courier New', Courier, monospace;
}
.actions {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeout-config {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 8px;
  gap: 4px;
}
</style> 