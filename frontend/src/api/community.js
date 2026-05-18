import request from "../utils/request";
export function getCommunityListService() {
  return request({
    url: "/community/list",
    method: "get",
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getSelectedCommunityListService(params) {
  return request({
    url: "/community/selectedList",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

//根据categoryId获取作者名字
export function getAuthorNameService(params) {
  return request({
    url: "/community/author",
    method: "get",
    params,
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}
