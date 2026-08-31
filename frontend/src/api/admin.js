import request from "../utils/request";

export function getAdminStatsService() {
  return request({ url: "/admin/stats", method: "get" });
}

export function getAdminArticlesService(params) {
  return request({ url: "/admin/articles", method: "get", params });
}

export function acceptArticleService(articleId) {
  return request({ url: "/admin/accept", method: "post", params: { articleId } });
}

export function rejectArticleService(articleId) {
  return request({ url: "/admin/reject", method: "post", params: { articleId } });
}

export function dropArticleService(articleId) {
  return request({ url: "/admin/drop", method: "post", params: { articleId } });
}

export function getAdminUsersService(params) {
  return request({ url: "/admin/users", method: "get", params });
}

export function getAdminTagsService() {
  return request({ url: "/admin/tags", method: "get" });
}

export function addAdminTagService(data) {
  return request({ url: "/admin/tags", method: "post", data });
}

export function updateAdminTagService(tagId, data) {
  return request({ url: `/admin/tags/${tagId}`, method: "put", data });
}

export function deleteAdminTagService(tagId) {
  return request({ url: `/admin/tags/${tagId}`, method: "delete" });
}

export function getAnnouncementService() {
  return request({ url: "/admin/announcement", method: "get" });
}

export function addAnnouncementService(data) {
  return request({ url: "/admin/addAnnouncement", method: "post", data });
}

export function deleteAnnouncementService(id) {
  return request({ url: "/admin/deleteAnnouncement", method: "post", params: { id } });
}
