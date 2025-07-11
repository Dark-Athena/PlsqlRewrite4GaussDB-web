import request from '@/utils/request';

/**
 * 请求SQL重写
 * @param {string} sql - 需要重写的SQL语句
 * @param {number} timeout - 超时时间（秒），默认20秒
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function rewriteSql(sql, timeout = 20) {
    // 后端需要一个简单的字符串，但axios的POST默认发送JSON。
    // 我们需要设置Content-Type为'text/plain'，并直接发送sql字符串。

    // 根据用户设置的timeout动态设置请求超时，额外增加10秒缓冲时间
    const requestTimeout = Math.max((timeout + 10) * 1000, 60000); // 至少60秒，用户设置+10秒缓冲

    return request({
        url: '/api/sql/rewrite',
        method: 'post',
        data: sql,
        params: {
            timeout: timeout
        },
        headers: {
            'Content-Type': 'text/plain'
        },
        timeout: requestTimeout // 动态设置请求超时时间
    });
}