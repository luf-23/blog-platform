import request from "../utils/request";

// Public
export function searchArticlesService(params) {
  return request({ url: "/article/search", method: "get", params });
}

export function getArticleDetailService(id) {
  return request({ url: `/article/detail/${id}`, method: "get" });
}

// My blog
export function getMyArticlesService(params) {
  return request({ url: "/article/my", method: "get", params });
}

export function getMyArticleDetailService(id) {
  return request({ url: `/article/my/detail/${id}`, method: "get" });
}

export function addArticleService(data) {
  return request({ url: "/article/add", method: "post", data });
}

export function updateArticleService(data) {
  return request({ url: "/article/update", method: "put", data });
}

export function deleteArticleService(id) {
  return request({ url: `/article/delete/${id}`, method: "delete" });
}

export function submitArticleForReviewService(id) {
  return request({ url: `/article/submit/${id}`, method: "post" });
}

export function updateCoverImageService(id, coverImage) {
  return request({ url: `/article/cover/${id}`, method: "post", params: { coverImage } });
}

// Admin
export function adminArticleListService(params) {
  return request({ url: "/article/admin/list", method: "get", params });
}

export function adminApproveArticleService(id) {
  return request({ url: `/article/admin/approve/${id}`, method: "post" });
}

export function adminRejectArticleService(id) {
  return request({ url: `/article/admin/reject/${id}`, method: "post" });
}
