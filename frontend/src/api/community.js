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

export function getCommunityProfileService(userId) {
  return request({ url: `/community/profile/${userId}`, method: 'get' })
}

export function getCommunityProfileFollowersService(userId, params) {
  return request({ url: `/community/profile/${userId}/followers`, method: 'get', params })
}

export function getCommunityProfileFollowingService(userId, params) {
  return request({ url: `/community/profile/${userId}/following`, method: 'get', params })
}

export function getCommunityFollowStateService(userId) {
  return request({ url: `/community/follow/${userId}`, method: 'get' })
}

export function toggleCommunityFollowService(userId) {
  return request({ url: `/community/follow/${userId}`, method: 'post' })
}
