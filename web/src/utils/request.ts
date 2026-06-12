// src/utils/request.ts
import axios from 'axios';

const service = axios.create({
    // 基础路径，自动读取 Vite 环境变量
    baseURL: import.meta.env.VITE_API_BASE_URL,
    // 超时时间（5秒）
    timeout: 60000,
});

// 响应拦截器：统一处理后端返回的数据和错误
service.interceptors.response.use(
    (response) => {
        // HTTP 状态码为 200 时进入这里
        const res = response.data;

        // 检查后端自定义的业务状态码是否为 200
        if (res && res.code !== 200) {
            // 如果不是 200，自动抛出异常，并传入后端的错误提示信息
            return Promise.reject(new Error(res.message || '业务请求失败'));
        }
        return res;
    },
    (error) => {
        // HTTP 状态码不是 2xx（如 400, 401, 403, 404, 500 等）或者网络超时会进入这里
        let message = '网络请求失败';

        if (error.response) {
            // 后端有返回，但是 HTTP 状态码报错
            message = error.response.data?.message || `服务器响应错误: ${error.response.status}`;
        } else if (error.request) {
            // 请求发出了，但没有收到响应（比如断网、超时）
            message = '服务器无响应，请检查网络连接';
        } else {
            // 其他设置请求时引发的错误
            message = error.message;
        }

        // 自动抛出异常
        return Promise.reject(new Error(message));
    }
);

export default service;