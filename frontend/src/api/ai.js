import { getBaseURL } from "../utils/request.js";
import { useTokenStore } from "../store/token.js";

/**
 * AI聊天流式接口 - 最简化版本
 * @param {Object} data - 包含messages, model, temperature等参数
 * @returns {Promise<Response>} - 返回流式响应
 */
export function chatStreamService(data) {
  const tokenStore = useTokenStore();
  const token = tokenStore.token;
  const baseURL = getBaseURL();

  return fetch(`${baseURL}ai/chat-stream`, {
    method: "POST",
    headers: {
      Authorization: token,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(data)
  });
}
