export function formatFileSize(bytes: number) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const unitBase = 1024
  const unitIndex = Math.floor(Math.log(bytes) / Math.log(unitBase))
  return `${parseFloat((bytes / Math.pow(unitBase, unitIndex)).toFixed(1))} ${units[unitIndex]}`
}
