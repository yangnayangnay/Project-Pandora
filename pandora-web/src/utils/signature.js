export const normalizeSignature = (value) => String(value ?? '').replace(/\r\n|[\r\u0085\u2028\u2029]/g, '\n')
export const signatureLineCount = (value) => normalizeSignature(value).split('\n').length

// Measure with the textarea's actual width and typography, including soft wrapping.
export const signatureFits = (value, textarea) => {
  const text = normalizeSignature(value)
  if (signatureLineCount(text) > 4) return false
  if (!textarea || textarea.clientWidth <= 0) return true
  const style = getComputedStyle(textarea)
  const probe = document.createElement('textarea')
  Object.assign(probe.style, {
    position: 'fixed', left: '-9999px', top: '0', visibility: 'hidden',
    height: '0', minHeight: '0', maxHeight: 'none', border: '0', boxSizing: 'border-box',
    width: `${textarea.clientWidth}px`, padding: style.padding, font: style.font,
    lineHeight: style.lineHeight, letterSpacing: style.letterSpacing, whiteSpace: 'pre-wrap',
    wordBreak: style.wordBreak, overflowWrap: style.overflowWrap, overflow: 'hidden'
  })
  probe.value = text
  document.body.appendChild(probe)
  try {
    const lineHeight = parseFloat(style.lineHeight) || parseFloat(style.fontSize) * 1.5
    const contentHeight = probe.scrollHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom)
    return contentHeight <= lineHeight * 4 + 1
  } finally { probe.remove() }
}
