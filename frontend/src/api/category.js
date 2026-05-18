import request from "../utils/request";
export function getCategoryListService() {
  return request({
    url: "/category/list",
    method: "get",
    headers: {
      //Authorization: token
    }
  });
}

export function addCategoryService(data) {
  return request({
    url: "/category/add",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/json"
    }
  });
}

export function deleteCategoryService(params) {
  return request({
    url: "/category/delete",
    method: "post",
    params,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  });
}

export function updateCategoryService(data) {
  return request({
    url: "/category/update",
    method: "post",
    data,
    headers: {
      "Content-Type": "application/json"
    }
  });
}
