import request from "../utils/request";

export function getCategoryListService() {
  return request({ url: "/category/list", method: "get" });
}

export function getCategoriesByUserService(userId) {
  return request({ url: `/category/byUser/${userId}`, method: "get" });
}

export function addCategoryService(data) {
  return request({ url: "/category/add", method: "post", data });
}

export function deleteCategoryService(id) {
  return request({ url: `/category/delete/${id}`, method: "delete" });
}

export function updateCategoryService(data) {
  return request({ url: "/category/update", method: "put", data });
}
