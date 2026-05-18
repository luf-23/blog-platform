import OSS from "ali-oss";
import {
  getCredentialsService,
  getBucketService,
  getEndPointService
} from "../../api/oss.js";

export class OSSClient {
  static IMAGE_TYPE = {
    AVATAR: "avatar",
    BACKGROUND: "background",
    ARTICLE_BACKGROUND: "article/background",
    ARTICLE_CONTENT: "article/content"
  };

  constructor() {
    this.client = null;
    this.STS = {
      credentials: [],
      bucket: "",
      endPoint: ""
    };
  }

  async init() {
    const credentials = await getCredentialsService();
    const bucket = await getBucketService();
    const endPoint = await getEndPointService();

    this.STS.credentials = credentials.data;
    this.STS.bucket = bucket.data;
    this.STS.endPoint = endPoint.data;

    this.client = new OSS({
      endpoint: this.STS.endPoint,
      accessKeyId: this.STS.credentials.accessKeyId,
      accessKeySecret: this.STS.credentials.accessKeySecret,
      stsToken: this.STS.credentials.securityToken,
      bucket: this.STS.bucket
    });
  }

  generateFileUrl(fileName) {
    return `https://${this.STS.bucket}.${this.STS.endPoint}/${fileName}`;
  }

  generateFileName(id, type, fileExtension) {
    if (!Object.values(OSSClient.IMAGE_TYPE).includes(type)) {
      throw new Error("Invalid image type");
    }
    const timestamp = Date.now();
    return `${type}/${id}_${timestamp}.${fileExtension}`;
  }

  async uploadFile(fileName, file) {
    if (!this.client) {
      throw new Error("OSS client not initialized");
    }
    return await this.client.put(fileName, file);
  }
}

export const ossClient = new OSSClient();
