import request from './request';

export function login(data) {
    return request({
        url: '/login',
        method: 'post',
        data // 直接传对象，axios自动转json
    });
}

export function logout() {
    return request({
        url: '/logout',
        method: 'post'
    });
}

export function getCurrentUser() {
    return request({
        url: '/me',
        method: 'get'
    });
}

export function changePassword(password) {
    return request({
        url: '/change-password',
        method: 'post',
        data: { password }
    });
}