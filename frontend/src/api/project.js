import request from '@/utils/request';

/**
 * 获取项目列表
 * @param {Object} params 查询参数
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getProjectList(params = {}) {
    return request({
        url: '/api/project/list',
        method: 'get',
        params
    });
}

/**
 * 创建新项目
 * @param {string} name 项目名称
 * @param {File} file 上传的文件
 * @param {string} inputCharset 输入字符集
 * @param {string} outputCharset 输出字符集
 * @param {number} concurrency 并发数
 * @param {Array<string>} fileExtensions 文件后缀配置
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function createProject(name, file, inputCharset, outputCharset, concurrency = 1, fileExtensions = ['sql']) {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('file', file);
    formData.append('inputCharset', inputCharset);
    formData.append('outputCharset', outputCharset);
    formData.append('concurrency', concurrency);
    formData.append('fileExtensions', JSON.stringify(fileExtensions));

    return request({
        url: '/api/project/create',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
}

/**
 * 删除项目
 * @param {string} id 项目ID
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteProject(id) {
    return request({
        url: `/api/project/${id}`,
        method: 'delete'
    });
}

export function batchDeleteProjects(ids) {
    return request({
        url: '/api/project/batch',
        method: 'delete',
        data: { ids: ids },
    });
}

export function getProjectDetail(id, params = {}) {
    return request({
        url: `/api/project/detail/${id}`,
        method: 'get',
        params
    });
}

export function getFileContent(id, file, type) {
    return request({
        url: `/api/project/fileContent/${id}`,
        method: 'get',
        params: { file, type }
    });
}