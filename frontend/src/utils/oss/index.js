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
    ARTICLE_COVER: "article/cover",
    ARTICLE_CONTENT: "article/content"
  };

  constructor() {
    this.client = null;
    this.initializing = null;
    this.STS = {
      credentials: [],
      bucket: "",
      endPoint: ""
    };
  }

  async init() {
    if (this.client) return this.client;
    if (this.initializing) return this.initializing;
    this.initializing = this.createClient();
    try {
      await this.initializing;
      return this.client;
    } finally {
      this.initializing = null;
    }
  }

  async createClient() {
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
    const endpoint = String(this.STS.endPoint || "").replace(/^https?:\/\//, "").replace(/\/$/, "");
    return `https://${this.STS.bucket}.${endpoint}/${fileName}`;
  }

  generateFileName(id, type, fileExtension) {
    if (!Object.values(OSSClient.IMAGE_TYPE).includes(type)) {
      throw new Error("Invalid image type");
    }
    const timestamp = Date.now();
    return `${type}/${id}_${timestamp}.${fileExtension}`;
  }

  async uploadFile(fileName, file) {
    await this.init();
    return this.client.put(fileName, file);
  }
}

export const ossClient = new OSSClient();

const MIME_EXTENSIONS = {
  "image/jpeg": "jpg",
  "image/png": "png",
  "image/gif": "gif",
  "image/webp": "webp",
  "image/avif": "avif"
};

export async function uploadImageToOss(file, type, ownerId = "user") {
  if (!(file instanceof File) || !file.type.startsWith("image/")) {
    throw new Error("请选择有效的图片文件");
  }
  if (file.size > 5 * 1024 * 1024) {
    throw new Error("图片大小不能超过 5MB");
  }
  const extension = MIME_EXTENSIONS[file.type] || file.name.split(".").pop()?.toLowerCase() || "png";
  const safeOwner = String(ownerId || "user").replace(/[^a-zA-Z0-9_-]/g, "-");
  const fileName = ossClient.generateFileName(safeOwner, type, extension);
  const result = await ossClient.uploadFile(fileName, file);
  return result.url || ossClient.generateFileUrl(fileName);
}
