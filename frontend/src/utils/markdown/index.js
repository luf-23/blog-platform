import MarkdownIt from 'markdown-it'
import markdownItAnchor from 'markdown-it-anchor'
import markdownItKatex from 'markdown-it-katex'
import markdownItTaskLists from 'markdown-it-task-lists'
import hljs from 'highlight.js'

function plusDividerRule(state, startLine, endLine, silent) {
  const start = state.bMarks[startLine] + state.tShift[startLine]
  const end = state.eMarks[startLine]
  const line = state.src.slice(start, end).trim()
  if (!/^\+{3,}$/.test(line)) return false
  if (silent) return true

  state.line = startLine + 1
  const token = state.push('hr', 'hr', 0)
  token.map = [startLine, state.line]
  token.markup = line
  return true
}

function markRule(state, silent) {
  const start = state.pos
  if (state.src.slice(start, start + 2) !== '==') return false
  const end = state.src.indexOf('==', start + 2)
  if (end < 0 || end === start + 2) return false
  if (!silent) {
    const open = state.push('mark_open', 'mark', 1)
    open.markup = '=='
    const text = state.push('text', '', 0)
    text.content = state.src.slice(start + 2, end)
    const close = state.push('mark_close', 'mark', -1)
    close.markup = '=='
  }
  state.pos = end + 2
  return true
}

export function applyMarkdownCompatibility(md) {
  if (md.__blogPlatformCompatibilityInstalled) return md
  md.__blogPlatformCompatibilityInstalled = true
  md.block.ruler.before('hr', 'typora_plus_divider', plusDividerRule)
  md.inline.ruler.before('emphasis', 'typora_mark', markRule)
  return md
}

export function createMarkdownRenderer() {
  const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    breaks: false,
    highlight(code, language) {
      if (language && hljs.getLanguage(language)) {
        try {
          return `<pre class="hljs"><code>${hljs.highlight(code, { language }).value}</code></pre>`
        } catch (_) {}
      }
      return `<pre class="hljs"><code>${md.utils.escapeHtml(code)}</code></pre>`
    }
  })

  applyMarkdownCompatibility(md)
  md.use(markdownItAnchor, {
    level: [1, 2, 3, 4],
    slugify: value => `section-${encodeURIComponent(value.trim().toLowerCase().replace(/\s+/g, '-'))}`
  })
  md.use(markdownItTaskLists, { enabled: true, label: true })
  md.use(markdownItKatex)

  const defaultLinkOpen = md.renderer.rules.link_open || ((tokens, index, options, env, self) => self.renderToken(tokens, index, options))
  md.renderer.rules.link_open = (tokens, index, options, env, self) => {
    const token = tokens[index]
    token.attrSet('target', '_blank')
    token.attrSet('rel', 'noopener noreferrer')
    return defaultLinkOpen(tokens, index, options, env, self)
  }
  return md
}
