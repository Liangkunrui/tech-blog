import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ gfm: true, breaks: true })

/** 渲染 Markdown 并消毒 HTML（防 XSS） */
export function renderMarkdown(md: string): string {
  const html = marked.parse(md ?? '') as string
  return DOMPurify.sanitize(html)
}
