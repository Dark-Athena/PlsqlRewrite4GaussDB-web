import request from '@/utils/request';

export function getGroupList() {
    return request({
        url: '/api/groups',
        method: 'get'
    });
}

export function createGroup(data) {
    return request({
        url: '/api/groups',
        method: 'post',
        data
    });
}

export function updateGroup(id, data) {
    return request({
        url: `/api/groups/${id}`,
        method: 'put',
        data
    });
}

export function deleteGroup(id) {
    return request({
        url: `/api/groups/${id}`,
        method: 'delete'
    });
}