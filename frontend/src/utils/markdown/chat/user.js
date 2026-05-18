import DOMPurify from "dompurify";

/**
 * 渲染用户消息的函数
 * 保持文本原始格式，确保HTML标签等内容正确显示为纯文本
 * @param {string} content - 用户消息内容
 * @returns {string} - 处理后的HTML字符串
 */
export const renderUserMessage = (content) => {
  // 转义HTML标签，确保它们显示为文本
  const escapedContent = content.replace(/</g, "&lt;").replace(/>/g, "&gt;");

  // 将换行符转换为<br>标签以保持换行格式
  const contentWithBreaks = escapedContent.replace(/\n/g, "<br>");

  // 使用DOMPurify清理内容，只允许<br>标签
  return DOMPurify.sanitize(contentWithBreaks, {
    ALLOWED_TAGS: ["br"],
    ALLOWED_ATTR: []
  });
};
