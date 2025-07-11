import axios from 'axios';

const request = axios.create({
    baseURL: '/api', // 可根据需要调整
    timeout: 10000,
    withCredentials: true // 允许携带 cookie
});

// 响应拦截，自动返回 data
request.interceptors.response.use(
    response => response.data,
    error => Promise.reject(error)
);

export default request;