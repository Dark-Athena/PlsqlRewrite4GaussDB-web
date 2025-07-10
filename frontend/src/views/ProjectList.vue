<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>项目列表</span>
        <div class="header-buttons">
          <el-button 
            type="danger" 
            :disabled="selectedProjects.length === 0" 
            @click="handleBatchDelete">
            批量删除
          </el-button>
          <el-button :icon="Refresh" circle @click="fetchProjectList" :loading="loading" />
        </div>
      </div>
    </template>
    <el-form :inline="true" :model="filters" class="filter-form" @submit.prevent>
      <el-form-item label="项目名">
        <el-input v-model="filters.name" placeholder="项目名" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部" clearable>
          <el-option label="全部" value="" />
          <el-option label="排队中" value="QUEUED" />
          <el-option label="运行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已终止" value="TERMINATED" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建用户">
        <el-input v-model="filters.owner" placeholder="用户名" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleFilter">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="projectList" v-loading="loading" style="width: 100%" @selection-change="handleSelectionChange" @sort-change="handleSortChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="项目名称" sortable="custom" />
      <el-table-column prop="owner" label="创建用户" sortable="custom" />
      <el-table-column prop="status" label="状态" sortable="custom">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" sortable="custom">
        <template #default="scope">
          {{ formatTime(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="finishTime" label="完成时间" sortable="custom">
        <template #default="scope">
          {{ formatTime(scope.row.finishTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="420">
        <template #default="scope">
          <el-button type="primary" size="small" @click="viewLog(scope.row)">查看日志</el-button>
          <el-button type="success" size="small" @click="downloadResult(scope.row)">下载结果</el-button>
          <el-button type="info" size="small" @click="downloadInput(scope.row)">下载原始文件</el-button>
          <el-button type="warning" size="small" @click="viewDetail(scope.row)">查看转换明细</el-button>
          <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="margin-top: 16px; text-align: right;">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
    <el-dialog v-model="logDialogVisible" title="查看日志" width="80vw" :before-close="closeLogDialog" style="max-width: 1000px;">
      <div style="height: 70vh; overflow-y: auto; background: #222; color: #eee; font-family: monospace; padding: 12px; white-space: pre-wrap;" ref="logContentRef">
        <span v-if="logLoading">加载中...</span>
        <span v-else>{{ logContent }}</span>
      </div>
      <template #footer>
        <el-button @click="closeLogDialog">关闭</el-button>
        <el-button @click="fetchLogContent" :loading="logLoading">手动刷新</el-button>
        <el-button @click="copyLogContent">复制日志</el-button>
        <el-button @click="downloadLogFile">下载日志</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="detailDialogVisible" title="转换明细" width="70vw">
      <div style="margin-bottom: 12px; display: flex; gap: 12px; align-items: center;">
        <el-input v-model="detailFilters.fileName" placeholder="文件名筛选" clearable style="width: 180px;" />
        <el-select v-model="detailFilters.status" placeholder="状态筛选" clearable style="width: 120px;">
          <el-option label="全部" value="" />
          <el-option label="成功" value="success" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-input v-model="detailFilters.content" placeholder="内容查找（输入关键字）" clearable style="width: 220px;" />
        <el-button @click="resetDetailFilters">重置</el-button>
      </div>
      <el-table :data="fileDetails" style="width: 100%" @sort-change="handleDetailSort">
        <el-table-column prop="fileName" label="文件名" sortable="custom" />
        <el-table-column prop="status" label="状态" sortable="custom">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'success' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="error" label="错误信息" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="compareFile(scope.row)">对比</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 8px; text-align: right;">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="detailTotal"
          :page-size="detailPageSize"
          :current-page="detailPage"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="val => { detailPageSize = val; detailPage = 1; fetchFileDetails(); }"
          @current-change="val => { detailPage = val; fetchFileDetails(); }"
        />
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="compareDialogVisible" title="文件对比" width="80vw">
      <div style="display: flex; gap: 16px;">
        <div style="flex:1;">
          <div style="font-weight:bold; margin-bottom:4px;">原始内容</div>
          <pre style="background:#222;color:#eee;min-height:300px;max-height:60vh;overflow:auto;white-space:pre-wrap;">{{ compareInput }}</pre>
        </div>
        <div style="flex:1;">
          <div style="font-weight:bold; margin-bottom:4px;">转换后内容</div>
          <pre style="background:#222;color:#eee;min-height:300px;max-height:60vh;overflow:auto;white-space:pre-wrap;">{{ compareOutput }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="compareDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="showDiffDialog = true">显示差异</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showDiffDialog" title="高亮差异" width="90vw">
      <div v-html="diffHtml"></div>
      <template #footer>
        <el-button @click="showDiffDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue';
import { getProjectList, deleteProject, batchDeleteProjects, getProjectDetail, getFileContent } from '../api/project';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';
import { Refresh } from '@element-plus/icons-vue';
import axios from 'axios';
import { createTwoFilesPatch } from 'diff';
import * as Diff2Html from 'diff2html';
import 'diff2html/bundles/css/diff2html.min.css';
import debounce from 'lodash.debounce';

const projectList = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const selectedProjects = ref([]);
const sortField = ref('');
const sortOrder = ref('');
const filters = ref({ name: '', status: '', owner: '' });
const logDialogVisible = ref(false);
const logContent = ref('');
const logTotalBytes = ref(0);
const logLoading = ref(false);
const logProjectId = ref('');
const logTimer = ref(null);
const logContentRef = ref(null);
const logFirstLoad = ref(true);
const detailDialogVisible = ref(false);
const fileDetails = ref([]);
const compareDialogVisible = ref(false);
const compareInput = ref('');
const compareOutput = ref('');
const compareFileName = ref('');
const detailProjectId = ref('');
const showDiffDialog = ref(false);
const diffHtml = ref('');
const detailFilters = ref({ fileName: '', status: '', content: '' });
const detailSort = ref({ prop: '', order: '' });
const detailPage = ref(1);
const detailPageSize = ref(10);
const detailTotal = ref(0);

const handleSelectionChange = (val) => {
  selectedProjects.value = val;
};

const fetchProjectList = async () => {
  loading.value = true;
  try {
    const res = await getProjectList({
      page: page.value,
      pageSize: pageSize.value,
      name: filters.value.name,
      status: filters.value.status,
      owner: filters.value.owner,
      sortField: sortField.value,
      sortOrder: sortOrder.value
    });
    projectList.value = res.list || [];
    total.value = res.total || 0;
  } catch (error) {
    console.error("Failed to fetch project list:", error);
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (val) => {
  page.value = val;
  fetchProjectList();
};
const handleSizeChange = (val) => {
  pageSize.value = val;
  page.value = 1;
  fetchProjectList();
};
const handleSortChange = ({ prop, order }) => {
  sortField.value = prop;
  sortOrder.value = order === 'descending' ? 'desc' : (order === 'ascending' ? 'asc' : '');
  fetchProjectList();
};
const handleFilter = () => {
  page.value = 1;
  fetchProjectList();
};
const resetFilter = () => {
  filters.value = { name: '', status: '', owner: '' };
  page.value = 1;
  fetchProjectList();
};

const getStatusTagType = (status) => {
  switch (status) {
    case 'SUCCESS':
      return 'success';
    case 'RUNNING':
      return 'primary';
    case 'FAILED':
      return 'danger';
    case 'PENDING':
      return 'warning';
    case 'QUEUED':
      return 'warning';
    case 'TERMINATED':
      return 'info';
    default:
      return 'info';
  }
};

const formatTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-';
};

const fetchLogContent = async () => {
  if (!logProjectId.value) return;
  if (logFirstLoad.value) logLoading.value = true;
  try {
    let isAtBottom = false;
    let distanceToBottom = 0;
    if (logContentRef.value) {
      const { scrollTop, scrollHeight, clientHeight } = logContentRef.value;
      isAtBottom = Math.abs(scrollTop + clientHeight - scrollHeight) < 10;
      distanceToBottom = scrollHeight - scrollTop - clientHeight;
    }
    const res = await axios.get(`/api/project/log/${logProjectId.value}?fromByte=${logTotalBytes.value}`);
    if (res.data.content && res.data.content.length > 0) {
      logContent.value += res.data.content;
      logTotalBytes.value = res.data.totalBytes || 0;
      await nextTick();
      if (logContentRef.value) {
        if (isAtBottom) {
          logContentRef.value.scrollTop = logContentRef.value.scrollHeight;
        } else {
          logContentRef.value.scrollTop = logContentRef.value.scrollHeight - logContentRef.value.clientHeight - distanceToBottom;
        }
      }
    } else {
      logTotalBytes.value = res.data.totalBytes || logTotalBytes.value;
    }
  } catch (e) {
    logContent.value += '\n日志加载失败';
  } finally {
    if (logFirstLoad.value) {
      logLoading.value = false;
      logFirstLoad.value = false;
    }
  }
};

const openLogDialog = (row) => {
  logProjectId.value = row.id;
  logDialogVisible.value = true;
  logContent.value = '';
  logTotalBytes.value = 0;
  logFirstLoad.value = true;
  fetchLogContent();
  clearInterval(logTimer.value);
  logTimer.value = setInterval(async () => {
    await fetchLogContent();
    if (row.status === 'SUCCESS' || row.status === 'FAILED' || row.status === 'TERMINATED') {
      clearInterval(logTimer.value);
    }
  }, 1500);
};

const closeLogDialog = () => {
  logDialogVisible.value = false;
  logContent.value = '';
  logProjectId.value = '';
  clearInterval(logTimer.value);
};

const viewLog = (row) => {
  openLogDialog(row);
};

const downloadResult = (row) => {
  if (row.status === 'SUCCESS') {
    window.location.href = `/api/project/download/${row.id}`;
  } else {
    ElMessage.warning('项目未完成或失败，无法下载结果');
  }
};

const downloadInput = (row) => {
  window.location.href = `/api/project/downloadInput/${row.id}`;
};

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除项目 "${row.name}" 吗？此操作将永久删除项目及其所有文件，且无法恢复。`,
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await deleteProject(row.id);
      ElMessage.success('项目已删除');
      fetchProjectList();
    } catch (error) {
      ElMessage.error('删除失败: ' + (error.response?.data || error.message));
    }
  }).catch(() => {});
};

const handleBatchDelete = () => {
  if (selectedProjects.value.length === 0) {
    ElMessage.warning('请至少选择一个项目');
    return;
  }
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedProjects.value.length} 个项目吗？此操作将永久删除项目及其所有文件，且无法恢复。`,
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      const ids = selectedProjects.value.map(p => p.id);
      await batchDeleteProjects(ids);
      ElMessage.success('选中的项目已删除');
      fetchProjectList();
    } catch (error) {
      ElMessage.error('批量删除失败: ' + (error.response?.data || error.message));
    }
  }).catch(() => {});
};

const copyLogContent = () => {
  if (!logContent.value) return;
  navigator.clipboard.writeText(logContent.value).then(() => {
    ElMessage.success('日志内容已复制到剪贴板');
  }, () => {
    ElMessage.error('复制失败，请手动选择内容复制');
  });
};

const downloadLogFile = () => {
  if (!logProjectId.value) return;
  window.open(`/api/project/log/${logProjectId.value}?download=1`, '_blank');
};

const viewDetail = async (row) => {
  detailProjectId.value = row.id;
  detailDialogVisible.value = true;
  detailPage.value = 1;
  fetchFileDetails();
};

const compareFile = async (fileDetail) => {
  compareFileName.value = fileDetail.fileName;
  compareInput.value = '加载中...';
  compareOutput.value = '加载中...';
  compareDialogVisible.value = true;
  const safeFileName = fileDetail.fileName.replace(/\\/g, '/');
  try {
    const [input, output] = await Promise.all([
      getFileContent(detailProjectId.value, safeFileName, 'input'),
      getFileContent(detailProjectId.value, safeFileName, 'output')
    ]);
    compareInput.value = input;
    compareOutput.value = output;
  } catch (e) {
    compareInput.value = compareOutput.value = '加载失败';
  }
};

const resetDetailFilters = () => {
  detailFilters.value = { fileName: '', status: '', content: '' };
  detailPage.value = 1;
  detailSort.value = { prop: '', order: '' };
  fetchFileDetails();
};

const handleDetailSort = ({ prop, order }) => {
  detailSort.value = { prop, order };
  detailPage.value = 1;
  fetchFileDetails();
};

const fetchFileDetails = async () => {
  if (!detailProjectId.value) return;
  const params = {
    fileName: detailFilters.value.fileName,
    status: detailFilters.value.status,
    content: detailFilters.value.content,
    page: detailPage.value,
    pageSize: detailPageSize.value,
    sortField: detailSort.value.prop,
    sortOrder: detailSort.value.order === 'ascending' ? 'asc' : (detailSort.value.order === 'descending' ? 'desc' : '')
  };
  try {
    const res = await getProjectDetail(detailProjectId.value, params);
    fileDetails.value = res.list || [];
    detailTotal.value = res.total || 0;
  } catch {
    fileDetails.value = [];
    detailTotal.value = 0;
  }
};

const debouncedFetchFileDetails = debounce(() => {
  fetchFileDetails();
}, 500);

watch([
  () => detailFilters.value.fileName,
  () => detailFilters.value.status,
  () => detailFilters.value.content
], () => {
  detailPage.value = 1;
  if (detailFilters.value.content && detailFilters.value.content.length > 0 && detailFilters.value.content.length < 2) {
    fileDetails.value = [];
    detailTotal.value = 0;
    return;
  }
  debouncedFetchFileDetails();
});

watch(showDiffDialog, (val) => {
  if (val) {
    const diff = createTwoFilesPatch('原始', '转换后', compareInput.value || '', compareOutput.value || '');
    diffHtml.value = Diff2Html.html(diff, { drawFileList: false, matching: 'lines', outputFormat: 'side-by-side' });
  }
});

onMounted(() => {
  fetchProjectList();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
}
.filter-form {
  margin-bottom: 16px;
}
</style> 