import request from '../utils/request.js'

export function getCommunityFeedService(params) {
  return request({ url: '/community/feed', method: 'get', params })
}

export function getFollowingCommunityFeedService(params) {
  return request({ url: '/community/feed/following', method: 'get', params })
}

export function getCommunityMetaService(personalized = false) {
  return request({ url: personalized ? '/community/meta/personalized' : '/community/meta', method: 'get' })
}

export function toggleCommunityFollowService(userId) {
  return request({ url: `/community/follow/${userId}`, method: 'post' })
}
