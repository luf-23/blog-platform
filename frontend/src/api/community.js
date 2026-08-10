import request from '../utils/request.js'

export function getCommunityFeedService(params) {
  return request({ url: '/community/feed', method: 'get', params })
}

export function getFollowingCommunityFeedService(params) {
  return request({ url: '/community/feed/following', method: 'get', params })
}

export function getCommunityMetaService() {
  return request({ url: '/community/meta', method: 'get' })
}

export function createCommunityPostService(data) {
  return request({ url: '/community/posts', method: 'post', data })
}

export function toggleCommunityLikeService(postId) {
  return request({ url: `/community/posts/${postId}/like`, method: 'post' })
}

export function voteCommunityPollService(postId, optionId) {
  return request({ url: `/community/posts/${postId}/vote`, method: 'post', params: { optionId } })
}

export function toggleCommunityFollowService(userId) {
  return request({ url: `/community/follow/${userId}`, method: 'post' })
}
