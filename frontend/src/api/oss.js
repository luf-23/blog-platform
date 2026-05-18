import request from "../utils/request";

export function getCredentialsService() {
  return request({
    url: "/oss/credentials",
    method: "get",
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getBucketService() {
  return request({
    url: "/oss/bucket",
    method: "get",
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getRegionService() {
  return request({
    url: "/oss/region",
    method: "get",
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}

export function getEndPointService() {
  return request({
    url: "/oss/endPoint",
    method: "get",
    headers: {
      ContentType: "application/xxx-www-form-urlencoded"
    }
  });
}
