import axios from 'axios';
import { ElMessage } from 'element-plus';

// 创建 axios 实例
const request = axios.create({
    baseURL: '', // 关键：不加/api前缀，所有API路径写全
    timeout: 5000,
    withCredentials: true
});

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        // 在发送请求之前做些什么
        return config;
    },
    (error) => {
        // 对请求错误做些什么
        console.log(error); // for debug
        return Promise.reject(error);
    }
);

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        const res = response.data;
        // 如果状态码不是200，则抛出错误（虽然axios会自动处理）
        if (response.status !== 200) {
            ElMessage.error(res.message || 'Error');
            return Promise.reject(new Error(res.message || 'Error'));
        } else {
            // 后端返回的数据可能在data字段或者直接就是数据
            return res.data || res;
        }
    },
    (error) => {
        // 对响应错误做点什么
        console.log('err' + error); // for debug
        let message = error.message;
        if (error.response && error.response.data) {
            message = error.response.data.message || error.response.data;
        }
        ElMessage.error(message);
        return Promise.reject(error);
    }
);

export default request;