/** 轻量提示（无 UI 库依赖） */
export function toast(message: string, type: 'info' | 'error' = 'info') {
  const el = document.createElement('div')
  el.textContent = message
  el.style.cssText =
    'position:fixed;top:16px;left:50%;transform:translateX(-50%);padding:10px 20px;border-radius:6px;' +
    `background:${type === 'error' ? '#cf222e' : '#1a7f37'};color:#fff;font-size:14px;z-index:9999;box-shadow:0 2px 8px rgba(0,0,0,.2);`
  document.body.appendChild(el)
  setTimeout(() => el.remove(), 2500)
}
