import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";
import highlightjs from "highlight.js";
import markdownItKatex from "markdown-it-katex";
import markdownItTaskLists from "markdown-it-task-lists";
import markdownItAnchor from "markdown-it-anchor";

// 配置 MarkdownIt 实例
const md = new MarkdownIt({
  html: true, // 启用 HTML 标签解析
  breaks: true, // 转换段落里的 '\n' 到 <br>
  linkify: true, // 将URL 转换为链接
  xhtmlOut: true, // 使用 '/' 闭合单标签
  typographer: true, // 启用语言替换和引号美化
  highlight: function (str, lang) {
    if (lang && highlightjs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${
          highlightjs.highlight(str, { language: lang, ignoreIllegals: true })
            .value
        }</code></pre>`;
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`;
  }
});

// 使用插件
md.use(markdownItKatex) // 数学公式支持
  .use(markdownItTaskLists, { enabled: true }) // 任务列表支持
  .use(markdownItAnchor); // 标题锚点支持

// 自定义渲染规则
md.renderer.rules.table_open = () =>
  '<div class="table-container"><table class="markdown-table">';
md.renderer.rules.table_close = () => "</table></div>";

// 将 Markdown 转换为 HTML
export const renderAssistantMessage = (content) => {
  const html = md.render(content);
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      // 基础标签
      "div",
      "span",
      "p",
      "br",
      "hr",
      // 标题
      "h1",
      "h2",
      "h3",
      "h4",
      "h5",
      "h6",
      // 文本格式
      "strong",
      "em",
      "del",
      "code",
      "pre",
      // 列表
      "ul",
      "ol",
      "li",
      "input",
      // 表格
      "table",
      "thead",
      "tbody",
      "tr",
      "th",
      "td",
      // 链接和图片
      "a",
      "img",
      // 数学公式
      "math",
      "semantics",
      "mrow",
      "mi",
      "mo",
      "mn",
      // 代码高亮
      "span"
    ],
    ALLOWED_ATTR: [
      "href",
      "target",
      "class",
      "id",
      "style",
      "src",
      "alt",
      "title",
      "type",
      "checked",
      "xmlns",
      "display",
      "data-*"
    ]
  });
};

// 为了方便在组件中使用，导出一些常用的样式类名
export const markdownStyles = {
  container: "markdown-content",
  codeBlock: "markdown-code-block",
  table: "markdown-table",
  taskList: "markdown-task-list",
  katex: "markdown-katex"
};
