// Measure real tag widths; member count alone cannot determine whether a row fits.
export function memberRowLayout(widths, available, buttonWidth, gap = 8) {
  if (available <= 0 || !widths.length) return { count: widths.length, overflow: false }
  const total = widths.reduce((sum, width) => sum + width, 0) + gap * (widths.length - 1)
  if (total <= available + 0.5) return { count: widths.length, overflow: false }
  let used = 0, count = 0
  for (const width of widths) {
    const next = used + (count ? gap : 0) + width
    if (next + gap + buttonWidth > available + 0.5) break
    used = next; count++
  }
  return { count, overflow: true }
}
