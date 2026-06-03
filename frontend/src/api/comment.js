import request from "../utils/request";

export function getCommentListService(params) {
  return request({ url: "/comment/list", method: "get", params });
}

export function getCommentRepliesService(params) {
  return request({ url: "/comment/replies", method: "get", params });
}

export function publishCommentService(data) {
  return request({ url: "/comment/publish", method: "post", data });
}

export function deleteCommentService(id) {
  return request({ url: `/comment/delete/${id}`, method: "delete" });
}
