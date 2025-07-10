import request from '@/utils/request';

/**
 * 请求SQL重写
 * @param {string} sql - 需要重写的SQL语句
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function rewriteSql(sql) {
    // 后端需要一个简单的字符串，但axios的POST默认发送JSON。
    // 我们需要设置Content-Type为'text/plain'，并直接发送sql字符串。
    return request({
        url: '/api/sql/rewrite',
        method: 'post',
        data: sql,
        headers: {
            'Content-Type': 'text/plain'
        }
    });
}