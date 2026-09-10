import request from "../utils/request";

export function getAllTagsService() {
  return request({ url: "/tag/all", method: "get" });
}

export function getPopularTagsService(limit = 20) {
  return request({ url: "/tag/popular", method: "get", params: { limit } });
}
